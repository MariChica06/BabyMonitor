package com.example.babymonitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlertasAdapter(private val lista: List<AlertaItem>) :
    RecyclerView.Adapter<AlertasAdapter.AlertaViewHolder>() {

    class AlertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHora: TextView = itemView.findViewById(R.id.tvHoraAlerta)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipoAlerta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alerta, parent, false)
        return AlertaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = lista[position]
        holder.tvHora.text = alerta.hora
        holder.tvTipo.text = alerta.tipo
    }

    override fun getItemCount(): Int = lista.size
}