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
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorService : Service() {

    private var audioRecord: AudioRecord? = null
    private var estaMonitoreando = false
    private var codigoSala: String = ""
    private val CANAL_ID = "BabyMonitorCanal"
    private val UMBRAL_SONIDO = 3000 // sensibilidad

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        codigoSala = intent?.getStringExtra("CODIGO_SALA") ?: ""

        crearCanalNotificacion()
        startForeground(1, crearNotificacionPersistente())

        iniciarEscucha()

        return START_STICKY
    }

    private fun crearCanalNotificacion() {
        val canal = NotificationChannel(
            CANAL_ID,
            "Monitor de Bebé",
            NotificationManager.IMPORTANCE_LOW
        )
        canal.description = "Monitoreo activo en segundo plano"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(canal)
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
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        estaMonitoreando = true
        audioRecord?.startRecording()

        // Hilo separado para no bloquear la app
        Thread {
            val buffer = ShortArray(bufferSize)
            while (estaMonitoreando) {
                val leidos = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (leidos > 0) {
                    val amplitud = buffer.take(leidos).maxOrNull()?.toInt() ?: 0
                    if (amplitud > UMBRAL_SONIDO) {
                        enviarAlerta(amplitud)
                        Thread.sleep(5000) // espera 5 seg antes de otra alerta
                    }
                }
            }
        }.start()
    }

    private fun enviarAlerta(amplitud: Int) {
        if (codigoSala.isEmpty()) return

        val tipo = when {
            amplitud > 8000 -> "🔴 Sonido muy fuerte"
            amplitud > 5000 -> "🟠 Llanto detectado"
            else -> "🟡 Sonido detectado"
        }

        val hora = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(Date())

        // Guardar en Firebase
        val database = FirebaseDatabase.getInstance("https://babymonitor-9ed16-default-rtdb.firebaseio.com/")
        val ref = database.getReference("salas/$codigoSala/alertas")
        val alerta = mapOf(
            "hora" to hora,
            "tipo" to tipo,
            "amplitud" to amplitud
        )
        ref.push().setValue(alerta)
    }

    override fun onDestroy() {
        super.onDestroy()
        estaMonitoreando = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}