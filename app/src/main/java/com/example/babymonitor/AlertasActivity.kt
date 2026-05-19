package com.example.babymonitor

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlertasActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSinAlertas: TextView
    private lateinit var tvTitulo: TextView
    private lateinit var btnLimpiar: ImageButton      // ← cambiado de Button a ImageButton
    private lateinit var adaptador: AlertasAdapter
    private val listaAlertas = mutableListOf<AlertaItem>()
    private var codigoSala = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alertas)

        recyclerView = findViewById(R.id.recyclerAlertas)
        tvSinAlertas = findViewById(R.id.tvSinAlertas)
        tvTitulo = findViewById(R.id.tvTitulo)
        btnLimpiar = findViewById(R.id.btnLimpiar)

        val btnHome = findViewById<ImageButton>(R.id.btnHome)  // ← cambiado de btnRetroceder
        btnHome.setOnClickListener { finish() }

        adaptador = AlertasAdapter(listaAlertas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adaptador

        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        codigoSala = prefs.getString("CODIGO_SALA", "") ?: ""

        if (codigoSala.isNotEmpty()) {
            resetearNoLeidas(codigoSala)
            cargarAlertas(codigoSala)
        } else {
            tvSinAlertas.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
            btnLimpiar.isEnabled = false
        }

        btnLimpiar.setOnClickListener {
            confirmarLimpiar()
        }
    }

    private fun confirmarLimpiar() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar historial")
            .setMessage("¿Eliminar todas las alertas de esta sala?")
            .setPositiveButton("Eliminar") { _, _ ->
                limpiarAlertas()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun limpiarAlertas() {
        getSharedPreferences("BabyMonitor", MODE_PRIVATE)
            .edit()
            .remove("ALERTAS_$codigoSala")
            .putInt("ALERTAS_NO_LEIDAS_$codigoSala", 0)
            .apply()

        listaAlertas.clear()
        adaptador.notifyDataSetChanged()
        actualizarTitulo()

        tvSinAlertas.visibility = android.view.View.VISIBLE
        recyclerView.visibility = android.view.View.GONE
    }

    private fun resetearNoLeidas(codigo: String) {
        getSharedPreferences("BabyMonitor", MODE_PRIVATE)
            .edit().putInt("ALERTAS_NO_LEIDAS_$codigo", 0).apply()
    }

    private fun cargarAlertas(codigo: String) {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val alertasGuardadas = prefs.getStringSet("ALERTAS_$codigo", emptySet()) ?: emptySet()

        listaAlertas.clear()

        if (alertasGuardadas.isEmpty()) {
            tvSinAlertas.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            tvSinAlertas.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE

            alertasGuardadas.forEach { alerta ->
                val partes = alerta.split("|")
                if (partes.size >= 2) {
                    listaAlertas.add(AlertaItem(partes[0], partes[1]))
                }
            }

            listaAlertas.sortByDescending { it.hora }
            adaptador.notifyDataSetChanged()
        }

        actualizarTitulo()
    }

    private fun actualizarTitulo() {
        val cantidad = listaAlertas.size
        tvTitulo.text = if (cantidad > 0) {
            "Historial de Alertas ($cantidad)"
        } else {
            "Historial de Alertas"
        }
    }
}