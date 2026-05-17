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

        val titulo = message.notification?.title ?: "👶 Baby Monitor"
        val cuerpo = message.notification?.body ?: "Sonido detectado"

        mostrarNotificacion(titulo, cuerpo)
        guardarAlertaLocal(cuerpo)
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

    private fun guardarAlertaLocal(tipo: String) {
        val hora = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            .format(Date())

        val prefs: SharedPreferences = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val alertas = prefs.getStringSet("ALERTAS", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        alertas.add("$hora|$tipo")
        prefs.edit().putStringSet("ALERTAS", alertas).apply()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Aquí se podría guardar el token en Firebase si se necesita
    }
}