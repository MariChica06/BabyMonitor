package com.example.babymonitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class ReceptorActivity : AppCompatActivity() {

    private lateinit var etCodigo: EditText
    private lateinit var btnConectar: Button
    private lateinit var btnDesconectar: Button
    private lateinit var tvEstadoConexion: TextView
    private lateinit var btnVerAlertas: ImageButton
    private lateinit var tvToken: TextView
    private lateinit var tvBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receptor)

        etCodigo = findViewById(R.id.etCodigo)
        btnConectar = findViewById(R.id.btnConectar)
        btnDesconectar = findViewById(R.id.btnDesconectar)
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion)
        btnVerAlertas = findViewById(R.id.btnVerAlertas)
        tvToken = findViewById(R.id.tvToken)
        tvBadge = findViewById(R.id.tvBadge)

        val btnHome = findViewById<android.widget.ImageButton>(R.id.btnHome)
        btnHome.setOnClickListener { finish() }

        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val codigoGuardado = prefs.getString("CODIGO_SALA", "") ?: ""
        val rol = prefs.getString("ROL", "") ?: ""

        if (codigoGuardado.isNotEmpty() && rol == "receptor") {
            setEstadoConectado(codigoGuardado)
        } else {
            setEstadoDesconectado()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                tvToken.text = "Token: $token"
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

        btnDesconectar.setOnClickListener {
            desconectar()
        }

        btnVerAlertas.setOnClickListener {
            startActivity(Intent(this, AlertasActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBadge()
    }

    private fun actualizarBadge() {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val codigo = prefs.getString("CODIGO_SALA", "") ?: ""

        if (codigo.isEmpty()) {
            tvBadge.visibility = android.view.View.GONE
            return
        }

        val noLeidas = prefs.getInt("ALERTAS_NO_LEIDAS_$codigo", 0)
        if (noLeidas > 0) {
            tvBadge.visibility = android.view.View.VISIBLE
            tvBadge.text = if (noLeidas > 99) "99+" else noLeidas.toString()
        } else {
            tvBadge.visibility = android.view.View.GONE
        }
    }

    private fun conectarASala(codigo: String) {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val token = prefs.getString("FCM_TOKEN", "") ?: ""

        val database = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://babymonitor-9ed16-default-rtdb.firebaseio.com/")

        database.getReference("salas/$codigo").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // No se borran alertas al conectar, se conservan por código
                prefs.edit()
                    .putString("CODIGO_SALA", codigo)
                    .putString("ROL", "receptor")
                    .apply()

                database.getReference("salas/$codigo/tokenReceptor").setValue(token)

                Toast.makeText(this, "Conectado a sala $codigo", Toast.LENGTH_SHORT).show()
                setEstadoConectado(codigo)
                actualizarBadge()
            } else {
                tvEstadoConexion.text = "Código inválido"
                Toast.makeText(this, "Código de sala incorrecto", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error de conexión con Firebase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun desconectar() {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val codigo = prefs.getString("CODIGO_SALA", "") ?: ""

        if (codigo.isNotEmpty()) {
            val database = com.google.firebase.database.FirebaseDatabase
                .getInstance("https://babymonitor-9ed16-default-rtdb.firebaseio.com/")
            database.getReference("salas/$codigo/tokenReceptor").removeValue()
        }

        // Solo se elimina la sesión, las alertas del código se conservan
        prefs.edit()
            .remove("CODIGO_SALA")
            .remove("ROL")
            .apply()

        Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
        setEstadoDesconectado()
    }

    private fun setEstadoConectado(codigo: String) {
        etCodigo.isEnabled = false
        etCodigo.setText(codigo)
        btnConectar.visibility = android.view.View.GONE
        btnDesconectar.visibility = android.view.View.VISIBLE
        btnVerAlertas.isEnabled = true
        tvEstadoConexion.text = "Conectado a sala $codigo"
    }

    private fun setEstadoDesconectado() {
        etCodigo.isEnabled = true
        etCodigo.text.clear()
        btnConectar.visibility = android.view.View.VISIBLE
        btnDesconectar.visibility = android.view.View.GONE
        btnVerAlertas.isEnabled = false
        tvEstadoConexion.text = "Sin conectar"
    }
}