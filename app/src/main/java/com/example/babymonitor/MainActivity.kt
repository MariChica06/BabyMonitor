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

        btnModoMonitor = findViewById(R.id.btnModoMonitor)
        btnModoPadre = findViewById(R.id.btnModoPadre)

        btnModoMonitor.setOnClickListener {
            // Generar código de sala aleatorio de 6 dígitos
            val codigoSala = generarCodigo()

            // Guardarlo en preferencias
            val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
            prefs.edit().putString("CODIGO_SALA", codigoSala).apply()
            prefs.edit().putString("ROL", "monitor").apply()

            // Ir a la pantalla Monitor
            val intent = Intent(this, MonitorActivity::class.java)
            intent.putExtra("CODIGO_SALA", codigoSala)
            startActivity(intent)
        }

        btnModoPadre.setOnClickListener {
            // Ir a la pantalla Receptor
            val intent = Intent(this, ReceptorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun generarCodigo(): String {
        return Random.nextInt(100000, 999999).toString()
    }
}