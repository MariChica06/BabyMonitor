package com.example.babymonitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var btnModoMonitor: Button
    private lateinit var btnModoPadre: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = android.graphics.Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        btnModoMonitor = findViewById(R.id.btnModoMonitor)
        btnModoPadre = findViewById(R.id.btnModoPadre)

        btnModoMonitor.setOnClickListener {
            val codigoSala = generarCodigo()
            val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
            prefs.edit().putString("CODIGO_SALA", codigoSala).apply()
            prefs.edit().putString("ROL", "monitor").apply()

            val intent = Intent(this, MonitorActivity::class.java)
            intent.putExtra("CODIGO_SALA", codigoSala)
            startActivity(intent)
        }

        btnModoPadre.setOnClickListener {
            val intent = Intent(this, ReceptorActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarEstadoBotones()
    }

    private fun actualizarEstadoBotones() {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val monitoreando = prefs.getBoolean("MONITOREANDO", false)
        val conectadoReceptor = prefs.getBoolean("RECEPTOR_CONECTADO", false)

        when {
            // Hay monitoreo activo → solo puede detenerlo entrando a Monitor
            monitoreando -> {
                btnModoMonitor.isEnabled = true   // puede entrar a ver/detener
                btnModoPadre.isEnabled = false
                btnModoPadre.alpha = 0.4f
                btnModoMonitor.alpha = 1f
            }
            // Hay receptor conectado → solo puede desconectarse entrando a Receptor
            conectadoReceptor -> {
                btnModoMonitor.isEnabled = false
                btnModoMonitor.alpha = 0.4f
                btnModoPadre.isEnabled = true    // puede entrar a ver/desconectar
                btnModoPadre.alpha = 1f
            }
            // Nada activo → ambos disponibles
            else -> {
                btnModoMonitor.isEnabled = true
                btnModoPadre.isEnabled = true
                btnModoMonitor.alpha = 1f
                btnModoPadre.alpha = 1f
            }
        }
    }

    private fun generarCodigo(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}