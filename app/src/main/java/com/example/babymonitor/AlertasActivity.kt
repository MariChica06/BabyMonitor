package com.example.babymonitor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AlertasActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSinAlertas: TextView
    private lateinit var adaptador: AlertasAdapter
    private val listaAlertas = mutableListOf<AlertaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alertas)

        recyclerView = findViewById(R.id.recyclerAlertas)
        tvSinAlertas = findViewById(R.id.tvSinAlertas)

        adaptador = AlertasAdapter(listaAlertas)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adaptador

        resetearNoLeidas()  // <- nuevo
        cargarAlertas()
    }

    private fun resetearNoLeidas() {
        getSharedPreferences("BabyMonitor", MODE_PRIVATE)
            .edit().putInt("ALERTAS_NO_LEIDAS", 0).apply()
    }

    private fun cargarAlertas() {
        val prefs = getSharedPreferences("BabyMonitor", MODE_PRIVATE)
        val alertasGuardadas = prefs.getStringSet("ALERTAS", emptySet()) ?: emptySet()

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
    }
}