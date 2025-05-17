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
        val nomeDiaTextView: TextView = itemView.findViewById(R.id.tv_dia_semana)
        val numeroDiaTextView: TextView = itemView.findViewById(R.id.tv_dia_numero)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectedPosition = if (selectedPosition == position) null else position
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
        val isHoje = dia.ehHoje
        val isSelecionado = selectedPosition == position

        holder.nomeDiaTextView.text = dia.nome
        holder.numeroDiaTextView.text = dia.numero.toString()

        holder.numeroDiaTextView.background = null
        holder.numeroDiaTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

        if (isHoje) {
            holder.numeroDiaTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ponto_vermelho)
            if (selectedPosition == null) {
                holder.numeroDiaTextView.setBackgroundResource(R.drawable.circle_white)
            }
        }

        if (isSelecionado) {
            holder.numeroDiaTextView.setBackgroundResource(R.drawable.circle_white)
        }
    }

    override fun getItemCount(): Int = dias.size
}
