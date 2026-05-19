package com.example.babymonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val CANAL_ID = "BabyMonitorAlertas"

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val codigo = prefs.getString("CODIGO_SALA", "") ?: ""

        // Ignorar notificaciones si no hay código de sala activo
        if (codigo.isEmpty()) return

        val titulo = message.notification?.title ?: "👶 Baby Monitor"
        val cuerpo = message.notification?.body ?: "Sonido detectado"

        mostrarNotificacion(titulo, cuerpo)
        guardarAlertaLocal(cuerpo, codigo)
        incrementarNoLeidas(codigo)
    }

    private fun mostrarNotificacion(titulo: String, cuerpo: String) {
        val manager = getSystemService(NotificationManager::class.java)

        val canal = NotificationChannel(
            CANAL_ID,
            "Alertas Baby Monitor",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(canal)

        val notificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notificacion)
    }

    private fun guardarAlertaLocal(tipo: String, codigo: String) {
        val hora = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(Date())

        val prefs: SharedPreferences = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val key = "ALERTAS_$codigo"
        val alertas = prefs.getStringSet(key, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        alertas.add("$hora|$tipo")
        prefs.edit().putStringSet(key, alertas).apply()
    }

    private fun incrementarNoLeidas(codigo: String) {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val key = "ALERTAS_NO_LEIDAS_$codigo"
        val actual = prefs.getInt(key, 0)
        prefs.edit().putInt(key, actual + 1).apply()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}