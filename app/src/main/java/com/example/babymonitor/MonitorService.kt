package com.example.babymonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MonitorService : Service() {

    private var audioRecord: AudioRecord? = null
    private var estaMonitoreando = false
    private var codigoSala: String = ""
    private val CANAL_ID = "BabyMonitorCanal"
    private val SAMPLE_RATE = 16000
    private var soundClassifier: SoundClassifier? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        codigoSala = intent?.getStringExtra("CODIGO_SALA") ?: ""
        crearCanalNotificacion()
        startForeground(1, crearNotificacionPersistente())
        soundClassifier = SoundClassifier()
        iniciarEscucha()
        return START_STICKY
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(
            CANAL_ID, "Monitor de Bebé",
            NotificationManager.IMPORTANCE_LOW
        )
        canal.description = "Monitoreo activo en segundo plano"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }

    private fun crearNotificacionPersistente(): Notification {
        return NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle("👶 Baby Monitor Activo")
            .setContentText("Escuchando en sala: $codigoSala")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun iniciarEscucha() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 4
        )

        estaMonitoreando = true
        audioRecord?.startRecording()

        Thread {
            val buffer = ShortArray(SAMPLE_RATE) // 1 segundo de audio
            var ultimaAlerta = 0L

            Log.d("BABY_MONITOR", "🎙️ Iniciando escucha con FFT...")
            Thread.sleep(1000)

            // Avisar que está calibrado
            val intentBase = Intent("NIVEL_SONIDO")
            intentBase.putExtra("amplitud", 0)
            intentBase.putExtra("calibrado", true)
            sendBroadcast(intentBase)

            Log.d("BABY_MONITOR", "✅ Listo para escuchar")

            while (estaMonitoreando) {
                val leidos = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (leidos > 0) {
                    // Calcular RMS para indicador visual
                    val rms = Math.sqrt(
                        buffer.take(leidos)
                            .map { it.toDouble() * it.toDouble() }
                            .average()
                    ).toInt()

                    // Actualizar UI
                    val intentNivel = Intent("NIVEL_SONIDO")
                    intentNivel.putExtra("amplitud", rms)
                    sendBroadcast(intentNivel)

                    val ahora = System.currentTimeMillis()
                    if (rms > 400 && (ahora - ultimaAlerta) > 8000) {
                        Log.d("BABY_MONITOR", "🔊 RMS=$rms analizando...")

                        val resultado = soundClassifier?.clasificar(buffer)
                            ?: ResultadoSonido.DESCONOCIDO

                        Log.d("BABY_MONITOR", "🎯 Resultado: $resultado")

                        when (resultado) {
                            ResultadoSonido.LLANTO_BEBE -> {
                                enviarAlerta("🍼 Llanto de bebé detectado", rms)
                                ultimaAlerta = ahora
                            }
                            ResultadoSonido.GOLPE_FUERTE -> {
                                enviarAlerta("💥 Golpe fuerte detectado", rms)
                                ultimaAlerta = ahora
                            }
                            ResultadoSonido.SONIDO_FUERTE -> {
                                enviarAlerta("🔊 Sonido fuerte detectado", rms)
                                ultimaAlerta = ahora
                            }
                            else -> Log.d("BABY_MONITOR", "✅ Sonido normal")
                        }
                    }
                }
            }
            Log.d("BABY_MONITOR", "🛑 Monitoreo detenido")
        }.start()
    }

    private fun enviarAlerta(tipo: String, rms: Int) {
        if (codigoSala.isEmpty()) return

        Log.d("BABY_MONITOR", "🚨 Enviando alerta: $tipo")

        val hora = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()
        ).format(java.util.Date())

        val database = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://babymonitor-9ed16-default-rtdb.firebaseio.com")
        val ref = database.getReference("salas/$codigoSala")

        ref.child("alertas").push().setValue(
            mapOf("hora" to hora, "tipo" to tipo, "amplitud" to rms)
        )

        ref.child("tokenReceptor").get().addOnSuccessListener { snapshot ->
            val tokenReceptor = snapshot.value as? String
            if (!tokenReceptor.isNullOrEmpty()) {
                NotificationSender.enviarNotificacion(
                    context = this,
                    tokenReceptor = tokenReceptor,
                    titulo = "👶 Baby Monitor — Alerta",
                    cuerpo = tipo
                )
            } else {
                Log.e("BABY_MONITOR", "❌ No hay token receptor")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        estaMonitoreando = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        soundClassifier?.cerrar()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}