package com.example.babymonitor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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
    private lateinit var tvNivelSonido: TextView
    private lateinit var waveformView: WaveformView        // ← nuevo
    private lateinit var recordingDot: View                // ← nuevo
    private var codigoSala: String = ""
    private var nivelActual = 0

    private val sonidoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val amplitud = intent?.getIntExtra("amplitud", 0) ?: 0
            val calibrado = intent?.getBooleanExtra("calibrado", false) ?: false

            if (calibrado) {
                tvEstado.text = "Estado: Calibrado y listo 🟢"
                return
            }

            nivelActual = when {
                amplitud > nivelActual -> amplitud
                else -> (nivelActual * 0.3).toInt()
            }

            val nivelMostrar = nivelActual.coerceIn(0, 10000)
            tvNivelSonido.text = "Nivel: $nivelMostrar"

            // ← nuevo: pasar amplitud real al círculo de ondas
            val ampNormalizada = nivelMostrar / 10000f
            waveformView.addAmplitude(ampNormalizada)

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
        tvCodigo = findViewById(R.id.tvCodigo)              // ← id corregido
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)
        tvNivelSonido = findViewById(R.id.tvNivelSonido)
        waveformView = findViewById(R.id.waveformView)      // ← nuevo
        recordingDot = findViewById(R.id.recordingDot)      // ← nuevo

        val btnRetroceder = findViewById<ImageButton>(R.id.btnHome)
        btnRetroceder.setOnClickListener { finish() }

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
        recordingDot.visibility = View.VISIBLE              // ← nuevo
        waveformView.startWave()                            // ← nuevo

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
            val intent = Intent(this, MonitorService::class.java)
            intent.putExtra("CODIGO_SALA", codigoSala)
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "✅ Monitoreo iniciado", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "❌ Error al conectar con Firebase", Toast.LENGTH_SHORT).show()
            tvEstado.text = "Estado: Error de conexión ❌"
            btnIniciar.isEnabled = true
            btnDetener.isEnabled = false
            recordingDot.visibility = View.GONE             // ← nuevo
            waveformView.stopWave()                         // ← nuevo
        }
    }

    private fun detenerMonitoreo() {
        tvEstado.text = "Estado: Detenido 🔴"
        btnIniciar.isEnabled = true
        btnDetener.isEnabled = false
        tvNivelSonido.text = "Nivel: 0"
        recordingDot.visibility = View.GONE                 // ← nuevo
        waveformView.stopWave()                             // ← nuevo

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