package com.example.babymonitor

import android.content.Context
import android.util.Log
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.PI

class SoundClassifier(private val context: Context? = null) {

    companion object {
        private const val TAG = "SoundClassifier"
        private const val UMBRAL_RMS_MINIMO = 400
    }

    private var babyCryDetector: BabyCryDetector? = null

    init {
        if (context != null) {
            babyCryDetector = BabyCryDetector(context)
            if (babyCryDetector?.tieneMuestras() == true) {
                Log.d(TAG, "✅ BabyCryDetector inicializado con muestras")
            } else {
                Log.w(TAG, "⚠️ Sin muestras de referencia, usando solo FFT")
            }
        }
    }

    fun clasificar(audioData: ShortArray, sampleRate: Int = 16000): ResultadoSonido {
        try {
            val rms = calcularRMS(audioData)
            Log.d(TAG, "RMS=$rms")

            if (rms < UMBRAL_RMS_MINIMO) return ResultadoSonido.NORMAL

            // Intentar detectar llanto con muestras + FFT combinados
            if (babyCryDetector?.tieneMuestras() == true && rms in 400..5000) {
                val esLlanto = babyCryDetector?.esProbableLlanto(audioData) ?: false
                val magnitudes = calcularFFT(audioData)
                val freqPorBin = sampleRate.toDouble() / (magnitudes.size * 2)

                val binBebeMin = (250 / freqPorBin).toInt()
                val binBebeMax = (600 / freqPorBin).toInt()
                val binVozMin  = (80  / freqPorBin).toInt()
                val binVozMax  = (300 / freqPorBin).toInt()
                val binAltaMin = (1000 / freqPorBin).toInt()
                val binAltaMax = (3000 / freqPorBin).toInt()

                var energiaBebe  = 0.0
                var energiaVoz   = 0.0
                var energiaAlta  = 0.0
                var energiaTotal = 0.0

                for (i in magnitudes.indices) {
                    energiaTotal += magnitudes[i]
                    if (i in binBebeMin..binBebeMax) energiaBebe += magnitudes[i]
                    if (i in binVozMin..binVozMax)   energiaVoz  += magnitudes[i]
                    if (i in binAltaMin..binAltaMax) energiaAlta += magnitudes[i]
                }

                val ratioBebe = if (energiaTotal > 0) energiaBebe / energiaTotal else 0.0
                val ratioVoz  = if (energiaTotal > 0) energiaVoz  / energiaTotal else 0.0
                val ratioAlta = if (energiaTotal > 0) energiaAlta / energiaTotal else 0.0
                val esContinuo = verificarContinuidad(audioData)
                val esRitmico  = verificarRitmo(audioData)
                val duracion   = verificarDuracionMinima(audioData)

                Log.d(TAG, "Bebe=${"%.2f".format(ratioBebe)} " +
                        "Voz=${"%.2f".format(ratioVoz)} " +
                        "Alta=${"%.2f".format(ratioAlta)} " +
                        "Continuo=$esContinuo Ritmico=$esRitmico Duracion=$duracion")

                // Filtros anti voz adulta
                val noEsVozAdulta = ratioVoz < 0.35 || ratioAlta > ratioVoz
                val tienePatronBebe = ratioBebe > 0.10 && ratioAlta > 0.08
                val esComportamientoBebe = esContinuo && esRitmico && duracion

                if ((esLlanto && noEsVozAdulta && esComportamientoBebe) ||
                    (tienePatronBebe && noEsVozAdulta && esComportamientoBebe && rms in 500..4000)) {
                    Log.d(TAG, "🍼 Llanto detectado")
                    return ResultadoSonido.LLANTO_BEBE
                }
            }

            // FFT para otros sonidos
            val magnitudes = calcularFFT(audioData)
            val freqPorBin = sampleRate.toDouble() / (magnitudes.size * 2)

            val binBebeMin = (250 / freqPorBin).toInt()
            val binBebeMax = (600 / freqPorBin).toInt()
            val binArmoMin = (600 / freqPorBin).toInt()
            val binArmoMax = (1200 / freqPorBin).toInt()
            val binVozMin  = (80  / freqPorBin).toInt()
            val binVozMax  = (250 / freqPorBin).toInt()

            var energiaBebe  = 0.0
            var energiaArmo  = 0.0
            var energiaVoz   = 0.0
            var energiaTotal = 0.0

            for (i in magnitudes.indices) {
                energiaTotal += magnitudes[i]
                if (i in binBebeMin..binBebeMax) energiaBebe += magnitudes[i]
                if (i in binArmoMin..binArmoMax) energiaArmo += magnitudes[i]
                if (i in binVozMin..binVozMax)   energiaVoz  += magnitudes[i]
            }

            val ratioBebe = if (energiaTotal > 0) energiaBebe / energiaTotal else 0.0
            val ratioArmo = if (energiaTotal > 0) energiaArmo / energiaTotal else 0.0
            val ratioVoz  = if (energiaTotal > 0) energiaVoz  / energiaTotal else 0.0
            val esContinuo = verificarContinuidad(audioData)
            val esRitmico  = verificarRitmo(audioData)

            Log.d(TAG, "RatioBebe=${"%.2f".format(ratioBebe)} " +
                    "RatioArmo=${"%.2f".format(ratioArmo)} " +
                    "RatioVoz=${"%.2f".format(ratioVoz)} " +
                    "Continuo=$esContinuo Ritmico=$esRitmico")

            return when {
                ratioBebe > 0.15 && ratioArmo > 0.10 &&
                        esContinuo && esRitmico &&
                        rms in 500..4000 &&
                        ratioBebe > ratioVoz -> {
                    Log.d(TAG, "🍼 Llanto detectado por FFT")
                    ResultadoSonido.LLANTO_BEBE
                }
                rms > 2500 && !esContinuo -> {
                    Log.d(TAG, "💥 Golpe detectado")
                    ResultadoSonido.GOLPE_FUERTE
                }
                rms > 1200 -> {
                    Log.d(TAG, "🔊 Sonido fuerte")
                    ResultadoSonido.SONIDO_FUERTE
                }
                else -> ResultadoSonido.NORMAL
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            return ResultadoSonido.DESCONOCIDO
        }
    }

    private fun calcularRMS(audio: ShortArray): Int {
        val suma = audio.map { it.toDouble() * it.toDouble() }.average()
        return sqrt(suma).toInt()
    }

    private fun calcularFFT(audio: ShortArray): DoubleArray {
        val n = minOf(audio.size, 1024)
        val magnitudes = DoubleArray(n / 2)
        for (k in 0 until n / 2) {
            var real = 0.0
            var imag = 0.0
            for (t in 0 until n) {
                val angle = 2.0 * PI * k * t / n
                real += audio[t] * cos(angle)
                imag -= audio[t] * kotlin.math.sin(angle)
            }
            magnitudes[k] = sqrt(real * real + imag * imag)
        }
        return magnitudes
    }

    private fun verificarContinuidad(audio: ShortArray): Boolean {
        val segmentos = 8
        val tamSegmento = audio.size / segmentos
        val energias = mutableListOf<Double>()
        for (i in 0 until segmentos) {
            val inicio = i * tamSegmento
            val fin = minOf(inicio + tamSegmento, audio.size)
            val energia = audio.slice(inicio until fin)
                .map { it.toDouble() * it.toDouble() }.average()
            energias.add(sqrt(energia))
        }
        val promedio = energias.average()
        val segmentosActivos = energias.count { it > promedio * 0.3 }
        return segmentosActivos >= segmentos / 2
    }

    private fun verificarRitmo(audio: ShortArray): Boolean {
        val segmentos = 16
        val tamSegmento = audio.size / segmentos
        val energias = mutableListOf<Double>()
        for (i in 0 until segmentos) {
            val inicio = i * tamSegmento
            val fin = minOf(inicio + tamSegmento, audio.size)
            val energia = audio.slice(inicio until fin)
                .map { it.toDouble() * it.toDouble() }.average()
            energias.add(sqrt(energia))
        }
        val promedio = energias.average()
        if (promedio < 100) return false
        var cambios = 0
        for (i in 1 until energias.size) {
            val diff = energias[i] - energias[i - 1]
            if (Math.abs(diff) > promedio * 0.2) cambios++
        }
        Log.d(TAG, "Cambios rítmicos: $cambios")
        return cambios in 3..12
    }

    private fun verificarDuracionMinima(audio: ShortArray): Boolean {
        val segmentos = 10
        val tamSegmento = audio.size / segmentos
        var segmentosActivos = 0
        for (i in 0 until segmentos) {
            val inicio = i * tamSegmento
            val fin = minOf(inicio + tamSegmento, audio.size)
            val energia = audio.slice(inicio until fin)
                .map { it.toDouble() * it.toDouble() }.average()
            val rmsSegmento = sqrt(energia)
            if (rmsSegmento > 300) segmentosActivos++
        }
        Log.d(TAG, "Segmentos activos: $segmentosActivos/10")
        return segmentosActivos >= 7
    }

    fun cerrar() {}
}

enum class ResultadoSonido {
    LLANTO_BEBE,
    GOLPE_FUERTE,
    SONIDO_FUERTE,
    NORMAL,
    DESCONOCIDO
}