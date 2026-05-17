package com.example.babymonitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class ReceptorActivity : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    private lateinit var btnConectar: Button
    private lateinit var tvEstadoConexion: TextView
    private lateinit var btnVerAlertas: Button
    private lateinit var tvToken: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptor)

        etCodigo = findViewById(R.id.etCodigo)
        btnConectar = findViewById(R.id.btnConectar)
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion)
        btnVerAlertas = findViewById(R.id.btnVerAlertas)
        tvToken = findViewById(R.id.tvToken)
        val btnRetroceder = findViewById<android.widget.ImageButton>(R.id.btnRetroceder)
        btnRetroceder.setOnClickListener {
            finish()
        }

        tvEstadoConexion.text = "Estado: Sin conectar"
        btnVerAlertas.isEnabled = false

        // Obtener el token FCM de este dispositivo
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                tvToken.text = "Token: $token"

                // Guardarlo en preferencias
                val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
                prefs.edit().putString("FCM_TOKEN", token).apply()
            }
        }

        btnConectar.setOnClickListener {
            val codigo = etCodigo.text.toString().trim()

            if (codigo.isEmpty()) {
                Toast.makeText(this, "Ingresa el código de sala", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (codigo.length != 6) {
                Toast.makeText(this, "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            conectarASala(codigo)
        }

        btnVerAlertas.setOnClickListener {
            val intent = Intent(this, AlertasActivity::class.java)
            startActivity(intent)
        }
    }

    private fun conectarASala(codigo: String) {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val token = prefs.getString("FCM_TOKEN", "") ?: ""

        // Verificar que el código existe en Firebase
        val database = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://babymonitor-9ed16-default-rtdb.firebaseio.com/")
        val ref = database.getReference("salas/$codigo")

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // El código existe, conectar
                prefs.edit().putString("CODIGO_SALA", codigo).apply()
                prefs.edit().putString("ROL", "receptor").apply()

                // Guardar token del receptor en Firebase
                database.getReference("salas/$codigo/tokenReceptor").setValue(token)

                tvEstadoConexion.text = "Estado: Conectado a sala $codigo 🟢"
                // Limpiar alertas anteriores al conectar a nueva sala
                prefs.edit().remove("ALERTAS").apply()
                btnVerAlertas.isEnabled = true
                Toast.makeText(this, "✅ Conectado a sala $codigo", Toast.LENGTH_SHORT).show()
            } else {
                // El código NO existe
                tvEstadoConexion.text = "Estado: Código inválido ❌"
                Toast.makeText(this, "❌ Código de sala incorrecto", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error de conexión con Firebase", Toast.LENGTH_SHORT).show()
        }
    }
}