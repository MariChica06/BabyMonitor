package com.example.babymonitor

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.PI

class BabyCryDetector(private val context: Context) {

    companion object {
        private const val TAG = "BabyCryDetector"
        private const val UMBRAL_SIMILITUD = 0.55f
    }

    // Perfil espectral promedio del llanto de bebé
    private var perfilEspectral: DoubleArray? = null
    private var muestrasCalculadas = 0

    init {
        cargarYCalcularPerfil()
    }

    private fun cargarYCalcularPerfil() {
        try {
            val archivos = context.assets.list("") ?: emptyArray()
            val archivosWav = archivos.filter { it.endsWith(".wav") }

            Log.d(TAG, "Archivos WAV encontrados: ${archivosWav.size}")

            val perfilesList = mutableListOf<DoubleArray>()

            for (archivo in archivosWav) {
                try {
                    val inputStream = context.assets.open(archivo)
                    val muestra = leerWav(inputStream)
                    if (muestra != null && muestra.isNotEmpty()) {
                        val shortArray = ShortArray(muestra.size) { i ->
                            (muestra[i] * 32768).toInt().toShort()
                        }
                        val perfil = calcularPerfilEspectral(shortArray)
                        perfilesList.add(perfil)
                        muestrasCalculadas++
                        Log.d(TAG, "Perfil calculado para: $archivo")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error con $archivo: ${e.message}")
                }
            }

            if (perfilesList.isNotEmpty()) {
                // Promediar todos los perfiles
                val size = perfilesList[0].size
                perfilEspectral = DoubleArray(size)
                for (perfil in perfilesList) {
                    for (i in 0 until size) {
                        perfilEspectral!![i] += perfil[i]
                    }
                }
                for (i in 0 until size) {
                    perfilEspectral!![i] /= perfilesList.size
                }
                Log.d(TAG, "Perfil espectral promedio calculado con $muestrasCalculadas muestras")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        }
    }

    private fun calcularPerfilEspectral(audio: ShortArray): DoubleArray {
        val n = minOf(audio.size, 512)
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

        // Normalizar
        val max = magnitudes.maxOrNull() ?: 1.0
        return if (max > 0) DoubleArray(magnitudes.size) { i -> magnitudes[i] / max }
        else magnitudes
    }

    fun esProbableLlanto(audioData: ShortArray): Boolean {
        val perfil = perfilEspectral ?: return false

        // Calcular perfil espectral del audio entrante
        val perfilEntrante = calcularPerfilEspectral(audioData)

        // Comparar con perfil de referencia usando cosine similarity
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        val size = minOf(perfil.size, perfilEntrante.size)
        for (i in 0 until size) {
            dotProduct += perfil[i] * perfilEntrante[i]
            norm1 += perfil[i] * perfil[i]
            norm2 += perfilEntrante[i] * perfilEntrante[i]
        }

        val similitud = if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else 0.0

        Log.d(TAG, "🎯 Similitud espectral: ${"%.3f".format(similitud)} Umbral: $UMBRAL_SIMILITUD")

        return similitud >= UMBRAL_SIMILITUD
    }

    private fun leerWav(inputStream: InputStream): FloatArray? {
        return try {
            val bytes = inputStream.readBytes()
            if (bytes.size < 44) return null

            var dataOffset = 12
            var dataSize = 0

            while (dataOffset < bytes.size - 8) {
                val chunkId = String(bytes, dataOffset, 4)
                val chunkSize = ByteBuffer.wrap(bytes, dataOffset + 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                if (chunkId == "data") {
                    dataSize = chunkSize
                    dataOffset += 8
                    break
                }
                dataOffset += 8 + chunkSize
            }

            if (dataSize == 0) return null

            val numMuestras = minOf(dataSize / 2, (bytes.size - dataOffset) / 2)
            val buffer = ByteBuffer.wrap(bytes, dataOffset, numMuestras * 2)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val floatArray = FloatArray(numMuestras)
            for (i in 0 until numMuestras) {
                if (buffer.remaining() >= 2) {
                    floatArray[i] = buffer.short / 32768.0f
                }
            }
            floatArray

        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo WAV: ${e.message}")
            null
        }
    }

    fun tieneMuestras(): Boolean = perfilEspectral != null
}