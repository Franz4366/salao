package com.example.salao

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DiaSemanaAdapter(private val dias: List<DiaSemana>) :
    RecyclerView.Adapter<DiaSemanaAdapter.DiaViewHolder>() {

    private var selectedPosition: Int? = null

    inner class DiaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDia: TextView = itemView.findViewById(R.id.tv_dia_semana)
        val tvNumero: TextView = itemView.findViewById(R.id.tv_dia_numero)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (selectedPosition == position) {
                        // Se clicou no mesmo, desmarca
                        selectedPosition = null
                    } else {
                        selectedPosition = position
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dia_semana, parent, false)
        return DiaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaViewHolder, position: Int) {
        val dia = dias[position]
        holder.tvDia.text = dia.nome
        holder.tvNumero.text = dia.numero.toString()

        // Limpa primeiro
        holder.tvNumero.background = null
        holder.tvNumero.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

        // Dia atual (hoje)?
        if (dia.ehHoje) {
            if (selectedPosition == null) {
                // Nenhum selecionado: hoje fica com círculo + ponto vermelho
                holder.tvNumero.setBackgroundResource(R.drawable.circle_white)
                holder.tvNumero.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ponto_vermelho)
            } else {
                // Se outro dia está selecionado, hoje só tem ponto vermelho
                holder.tvNumero.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ponto_vermelho)
            }
        }

        // Dia selecionado?
        if (selectedPosition == position) {
            holder.tvNumero.setBackgroundResource(R.drawable.circle_white)
        }
    }

    override fun getItemCount(): Int = dias.size
}