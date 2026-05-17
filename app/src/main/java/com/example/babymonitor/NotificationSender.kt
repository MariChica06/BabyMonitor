package com.example.babymonitor

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object NotificationSender {

    private const val FCM_URL =
        "https://fcm.googleapis.com/v1/projects/babymonitor-9ed16/messages:send"

    fun enviarNotificacion(
        context: Context,
        tokenReceptor: String,
        titulo: String,
        cuerpo: String
    ) {
        // Ejecutar en hilo separado para no bloquear la app
        Thread {
            try {
                // Leer el archivo de credenciales desde assets
                val assetManager = context.assets
                val files = assetManager.list("") ?: emptyArray()

                // Buscar el archivo json de firebase admin
                val jsonFileName = files.firstOrNull {
                    it.endsWith(".json") && it.contains("firebase")
                }

                if (jsonFileName == null) {
                    Log.e("FCM", "No se encontró el archivo de credenciales")
                    return@Thread
                }

                val inputStream: InputStream = assetManager.open(jsonFileName)

                // Obtener el token de acceso usando las credenciales
                val credentials = GoogleCredentials
                    .fromStream(inputStream)
                    .createScoped(
                        listOf("https://www.googleapis.com/auth/firebase.messaging")
                    )
                credentials.refresh()
                val accessToken = credentials.accessToken.tokenValue

                // Crear el mensaje JSON
                val mensaje = JSONObject()
                val message = JSONObject()
                val notification = JSONObject()

                notification.put("title", titulo)
                notification.put("body", cuerpo)
                message.put("notification", notification)
                message.put("token", tokenReceptor)
                mensaje.put("message", message)

                // Enviar la petición HTTP a FCM
                val url = URL(FCM_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(mensaje.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    Log.d("FCM", "✅ Notificación enviada correctamente")
                } else {
                    Log.e("FCM", "❌ Error al enviar: $responseCode")
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e("FCM", "Error: ${e.message}")
            }
        }.start()
    }
}