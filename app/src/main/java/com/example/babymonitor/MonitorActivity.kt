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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MonitorActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView
    private lateinit var tvCodigo: TextView
    private lateinit var btnIniciar: ImageButton      // ← corregido de Button a ImageButton
    private lateinit var btnDetener: ImageButton      // ← corregido de Button a ImageButton
    private lateinit var btnPause: ImageButton        // ← agregado
    private lateinit var tvNivelSonido: TextView
    private lateinit var waveformView: WaveformView
    private lateinit var recordingDot: View
    private var codigoSala: String = ""
    private var nivelActual = 0
    private var pausado = false                       // ← para controlar estado de pausa

    private val sonidoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val amplitud = intent?.getIntExtra("amplitud", 0) ?: 0
            val calibrado = intent?.getBooleanExtra("calibrado", false) ?: false

            if (calibrado) {
                tvEstado.text = "Estado: Calibrado y listo 🟢"
                return
            }

            if (pausado) return                       // ← si está pausado no actualiza

            nivelActual = when {
                amplitud > nivelActual -> amplitud
                else -> (nivelActual * 0.3).toInt()
            }

            val nivelMostrar = nivelActual.coerceIn(0, 10000)
            tvNivelSonido.text = "Nivel: $nivelMostrar"

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
        tvCodigo = findViewById(R.id.tvCodigo)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)
        btnPause = findViewById(R.id.btnPause)        // ← agregado
        tvNivelSonido = findViewById(R.id.tvNivelSonido)
        waveformView = findViewById(R.id.waveformView)
        recordingDot = findViewById(R.id.recordingDot)

        val btnHome = findViewById<ImageButton>(R.id.btnHome)
        btnHome.setOnClickListener { finish() }

        tvCodigo.text = "$codigoSala"
        tvEstado.text = "Estado: Detenido 🔴"
        btnDetener.isEnabled = false
        btnPause.isEnabled = false                    // ← deshabilitado al inicio

        btnIniciar.setOnClickListener {
            if (verificarPermisoMicrofono()) {
                iniciarMonitoreo()
            }
        }

        btnPause.setOnClickListener {                 // ← lógica de pausa
            if (!pausado) {
                pausado = true
                tvEstado.text = "Estado: Pausado ⏸️"
                waveformView.stopWave()
            } else {
                pausado = false
                tvEstado.text = "Estado: Monitoreando 🟢"
                waveformView.startWave()
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
        pausado = false                               // ← resetear pausa
        tvEstado.text = "Estado: Calibrando... 🟡"
        btnIniciar.isEnabled = false
        btnDetener.isEnabled = true
        btnPause.isEnabled = true                     // ← habilitar pausa
        recordingDot.visibility = View.VISIBLE
        waveformView.startWave()

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
            tvEstado.text = "Estado: Monitoreando 🟢"
        }.addOnFailureListener {
            Toast.makeText(this, "❌ Error al conectar con Firebase", Toast.LENGTH_SHORT).show()
            tvEstado.text = "Estado: Error de conexión ❌"
            btnIniciar.isEnabled = true
            btnDetener.isEnabled = false
            btnPause.isEnabled = false                // ← deshabilitar pausa si falla
            recordingDot.visibility = View.GONE
            waveformView.stopWave()
        }
    }

    private fun detenerMonitoreo() {
        pausado = false                               // ← resetear pausa
        tvEstado.text = "Estado: Detenido 🔴"
        btnIniciar.isEnabled = true
        btnDetener.isEnabled = false
        btnPause.isEnabled = false                    // ← deshabilitar pausa
        tvNivelSonido.text = "Nivel: 0"
        recordingDot.visibility = View.GONE
        waveformView.stopWave()

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