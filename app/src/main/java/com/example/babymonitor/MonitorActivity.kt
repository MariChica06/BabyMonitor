package com.example.babymonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
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
    private var codigoSala: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor)

        // Obtener el código de sala que viene de MainActivity
        codigoSala = intent.getStringExtra("CODIGO_SALA") ?: ""

        // Conectar con los elementos del XML
        tvEstado = findViewById(R.id.tvEstado)
        tvCodigo = findViewById(R.id.tvCodigo)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnDetener = findViewById(R.id.btnDetener)

        // Mostrar el código de sala
        tvCodigo.text = "Código de sala: $codigoSala"
        tvEstado.text = "Estado: Detenido"
        btnDetener.isEnabled = false

        // Botón iniciar monitoreo
        btnIniciar.setOnClickListener {
            if (verificarPermisoMicrofono()) {
                iniciarMonitoreo()
            }
        }

        // Botón detener monitoreo
        btnDetener.setOnClickListener {
            detenerMonitoreo()
        }
    }

    private fun verificarPermisoMicrofono(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
            false
        } else {
            true
        }
    }

    private fun iniciarMonitoreo() {
        tvEstado.text = "Estado: Monitoreando... 🟢"
        btnIniciar.isEnabled = false
        btnDetener.isEnabled = true

        // Iniciar el servicio en segundo plano
        val intent = Intent(this, MonitorService::class.java)
        intent.putExtra("CODIGO_SALA", codigoSala)
        ContextCompat.startForegroundService(this, intent)

        Toast.makeText(this, "Monitoreo iniciado", Toast.LENGTH_SHORT).show()
    }

    private fun detenerMonitoreo() {
        tvEstado.text = "Estado: Detenido 🔴"
        btnIniciar.isEnabled = true
        btnDetener.isEnabled = false

        // Detener el servicio
        val intent = Intent(this, MonitorService::class.java)
        stopService(intent)

        Toast.makeText(this, "Monitoreo detenido", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
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
}