package com.example.babymonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MonitorActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView
    private lateinit var tvCodigo: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnDetener: Button
    private lateinit var progressSonido: ProgressBar
    private lateinit var tvNivelSonido: TextView
    private var codigoSala: String = ""

    // Receptor para recibir el nivel de sonido del servicio
    private var nivelActual = 0

    private val sonidoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val amplitud = intent?.getIntExtra("amplitud", 0) ?: 0
            val calibrado = intent?.getBooleanExtra("calibrado", false) ?: false

            if (calibrado) {
                tvEstado.text = "Estado: Calibrado y listo 🟢"
                return
            }

            // Suavizar el movimiento de la barra
            nivelActual = when {
                amplitud > nivelActual -> amplitud  // sube rápido
                else -> (nivelActual * 0.3).toInt() // baja rápido también
            }

            val nivelMostrar = nivelActual.coerceIn(0, 10000)
            progressSonido.progress = nivelMostrar
            tvNivelSonido.text = "Nivel: $nivelMostrar"

            when {
                nivelMostrar > 6000 -> tvNivelSonido.setTextColor(
                    getColor(android.R.color.holo_red_dark))
                nivelMostrar > 3000 -> tvNivelSonido.setTextColor(
                    getColor(android.R.color.holo_orange_dark))
                else -> tvNivelSonido.setTextColor(
                    getColor(android.R.color.holo_green_dark))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Modo Monitor"

        codigoSala = intent.getStringExtra("CODIGO_SALA") ?: ""

        tvEstado = findViewById(R.id.tvEstado)
        tvCodigo = findViewById(R.id.tvCodigo)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)
        progressSonido = findViewById(R.id.progressSonido)
        tvNivelSonido = findViewById(R.id.tvNivelSonido)
        val btnRetroceder = findViewById<android.widget.ImageButton>(R.id.btnRetroceder)
        btnRetroceder.setOnClickListener {
            finish()
        }

        tvCodigo.text = "Código de sala: $codigoSala"
        tvEstado.text = "Estado: Detenido 🔴"
        btnDetener.isEnabled = false

        btnIniciar.setOnClickListener {
            if (verificarPermisoMicrofono()) {
                iniciarMonitoreo()
            }
        }

        btnDetener.setOnClickListener {
            detenerMonitoreo()
        }
    }

    override fun onResume() {
        super.onResume()
        // Registrar receptor de nivel de sonido
        val filter = IntentFilter("NIVEL_SONIDO")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sonidoReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(sonidoReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(sonidoReceiver)
    }

    private fun verificarPermisoMicrofono(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 100
            )
            false
        } else {
            true
        }
    }

    private fun iniciarMonitoreo() {
        tvEstado.text = "Estado: Calibrando... 🟡"
        btnIniciar.isEnabled = false
        btnDetener.isEnabled = true

        // Guardar la sala en Firebase para que el receptor pueda encontrarla
        val database = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://babymonitor-9ed16-default-rtdb.firebaseio.com")
        val ref = database.getReference("salas/$codigoSala")

        val salaData = mapOf(
            "activa" to true,
            "creadaEn" to java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date())
        )

        ref.setValue(salaData).addOnSuccessListener {
            // Sala creada en Firebase, ahora iniciamos el servicio
            val intent = Intent(this, MonitorService::class.java)
            intent.putExtra("CODIGO_SALA", codigoSala)
            ContextCompat.startForegroundService(this, intent)

            Toast.makeText(this, "✅ Monitoreo iniciado", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "❌ Error al conectar con Firebase", Toast.LENGTH_SHORT).show()
            tvEstado.text = "Estado: Error de conexión ❌"
            btnIniciar.isEnabled = true
            btnDetener.isEnabled = false
        }
    }

    private fun detenerMonitoreo() {
        tvEstado.text = "Estado: Detenido 🔴"
        btnIniciar.isEnabled = true
        btnDetener.isEnabled = false
        progressSonido.progress = 0
        tvNivelSonido.text = "Nivel: 0"

        stopService(Intent(this, MonitorService::class.java))
        Toast.makeText(this, "Monitoreo detenido", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarMonitoreo()
        } else {
            Toast.makeText(this, "Se necesita permiso del micrófono", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}