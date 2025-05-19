package com.example.salao

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface OnDateClickListener {
    fun onDateClick(date: Date)
}

class DiaSemanaAdapter(private val diasSemana: List<Date>) :
    RecyclerView.Adapter<DiaSemanaAdapter.DiaViewHolder>() {

    private var selectedPosition: Int? = null
    private var listener: OnDateClickListener? = null

    fun setOnDateClickListener(listener: OnDateClickListener) {
        this.listener = listener
    }

    inner class DiaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeDiaTextView: TextView = itemView.findViewById(R.id.tv_dia_semana)
        val numeroDiaTextView: TextView = itemView.findViewById(R.id.tv_dia_numero)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectedPosition = if (selectedPosition == position) null else position
                    notifyDataSetChanged()
                    listener?.onDateClick(diasSemana[position]) // Chama o listener com a data
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
        val date = diasSemana[position]
        val calendar = Calendar.getInstance()
        calendar.time = date

        val hoje = Calendar.getInstance()

        val nomeDia = SimpleDateFormat("EEE", Locale("pt", "BR")).format(date).uppercase(Locale.getDefault())
        val numeroDia = calendar.get(Calendar.DAY_OF_MONTH)

        val ehHoje = hoje.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                hoje.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                hoje.get(Calendar.DAY_OF_MONTH) == numeroDia

        val isSelecionado = selectedPosition == position

        holder.nomeDiaTextView.text = nomeDia
        holder.numeroDiaTextView.text = numeroDia.toString()

        holder.numeroDiaTextView.background = null
        holder.numeroDiaTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

        if (ehHoje) {
            holder.numeroDiaTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ponto_vermelho)
            if (selectedPosition == null) {
                holder.numeroDiaTextView.setBackgroundResource(R.drawable.circle_white)
            }
        }

        if (isSelecionado) {
            holder.numeroDiaTextView.setBackgroundResource(R.drawable.circle_white)
        }
    }

    override fun getItemCount(): Int = diasSemana.size
}