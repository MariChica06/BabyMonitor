package com.example.babymonitor

import android.graphics.Color
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
        val viewColor: View = itemView.findViewById(R.id.viewColorTipo)
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

        // Color lateral según tipo de alerta
        when {
            alerta.tipo.contains("llanto", ignoreCase = true) ||
                    alerta.tipo.contains("bebé", ignoreCase = true) -> {
                holder.viewColor.setBackgroundColor(Color.parseColor("#E74C3C")) // rojo
                holder.tvTipo.setTextColor(Color.parseColor("#E74C3C"))
            }
            alerta.tipo.contains("fuerte", ignoreCase = true) ||
                    alerta.tipo.contains("sonido", ignoreCase = true) -> {
                holder.viewColor.setBackgroundColor(Color.parseColor("#E67E22")) // naranja
                holder.tvTipo.setTextColor(Color.parseColor("#E67E22"))
            }
            else -> {
                holder.viewColor.setBackgroundColor(Color.parseColor("#9B59B6")) // morado
                holder.tvTipo.setTextColor(Color.parseColor("#2C3E50"))
            }
        }
    }

    override fun getItemCount(): Int = lista.size
}