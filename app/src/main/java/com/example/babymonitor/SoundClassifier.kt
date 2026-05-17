package com.example.babymonitor

import android.util.Log
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.PI

class SoundClassifier {

    companion object {
        private const val TAG = "SoundClassifier"
        private const val UMBRAL_RMS_MINIMO = 400
    }

    fun clasificar(audioData: ShortArray, sampleRate: Int = 16000): ResultadoSonido {
        try {
            val rms = calcularRMS(audioData)
            Log.d(TAG, "RMS=$rms")

            if (rms < UMBRAL_RMS_MINIMO) return ResultadoSonido.NORMAL

            val magnitudes = calcularFFT(audioData)
            val freqPorBin = sampleRate.toDouble() / (magnitudes.size * 2)

            // Rangos de frecuencia
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
                    "Continuo=$esContinuo Ritmico=$esRitmico RMS=$rms")

            return when {
                // Llanto bebé: frecuencias características + continuo + rítmico
                // + tiene armónicos + no dominado por voz adulta baja
                ratioBebe > 0.15 &&
                        ratioArmo > 0.10 &&
                        esContinuo &&
                        esRitmico &&
                        rms in 500..4000 &&
                        ratioBebe > ratioVoz -> {
                    Log.d(TAG, "🍼 Llanto detectado")
                    ResultadoSonido.LLANTO_BEBE
                }
                // Golpe: muy fuerte + corto + no continuo
                rms > 2500 && !esContinuo -> {
                    Log.d(TAG, "💥 Golpe detectado")
                    ResultadoSonido.GOLPE_FUERTE
                }
                // Sonido fuerte general
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
                .map { it.toDouble() * it.toDouble() }
                .average()
            energias.add(sqrt(energia))
        }

        val promedio = energias.average()
        val segmentosActivos = energias.count { it > promedio * 0.3 }
        return segmentosActivos >= segmentos / 2
    }

    fun cerrar() {
        // No hay recursos que liberar
    }
    // El llanto de bebé es rítmico: tiene variaciones periódicas de energía
    private fun verificarRitmo(audio: ShortArray): Boolean {
        val segmentos = 16
        val tamSegmento = audio.size / segmentos
        val energias = mutableListOf<Double>()

        for (i in 0 until segmentos) {
            val inicio = i * tamSegmento
            val fin = minOf(inicio + tamSegmento, audio.size)
            val energia = audio.slice(inicio until fin)
                .map { it.toDouble() * it.toDouble() }
                .average()
            energias.add(sqrt(energia))
        }

        val promedio = energias.average()
        if (promedio < 100) return false

        // Contar cuántas veces sube y baja la energía
        var cambios = 0
        for (i in 1 until energias.size) {
            val diff = energias[i] - energias[i - 1]
            if (Math.abs(diff) > promedio * 0.2) cambios++
        }

        // El llanto tiene entre 4 y 12 cambios de energía por segundo
        Log.d(TAG, "Cambios rítmicos: $cambios")
        return cambios in 3..12
    }
}

enum class ResultadoSonido {
    LLANTO_BEBE,
    GOLPE_FUERTE,
    SONIDO_FUERTE,
    NORMAL,
    DESCONOCIDO
}