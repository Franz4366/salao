package com.example.salaa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface OnDateClickListener {
    fun onDateClick(date: Date)
}

class DiaSemanaAdapter(private val diasSemana: List<Date>, private val mesPrincipalCalendar: Calendar) :
    RecyclerView.Adapter<DiaSemanaAdapter.DiaViewHolder>() {

    var selectedPosition: Int? = null // Posição do dia selecionado pelo usuário
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
                    val previousSelectedPosition = selectedPosition // Guarda a posição anterior
                    var newSelectedPosition: Int? = null // Variável para a nova posição selecionada

                    if (selectedPosition == position) {
                        // O item clicado já estava selecionado (o usuário quer desselecionar)
                        // Primeiro, desmarcamos a seleção explícita
                        selectedPosition = null
                        // Agora, tentamos mover a seleção para o dia de hoje
                        val hojeIndex = getTodayIndex()
                        if (hojeIndex != -1) {
                            newSelectedPosition = hojeIndex // O dia de hoje se torna a nova seleção (círculo branco)
                            listener?.onDateClick(diasSemana[hojeIndex]) // Notifica com a data de hoje
                        } else {
                            // Se "hoje" não está visível, e a seleção é desmarcada,
                            // podemos notificar com a data que foi "desfocada"
                            listener?.onDateClick(diasSemana[position])
                        }
                    } else {
                        // Um novo item foi clicado (o usuário quer selecionar uma nova data)
                        newSelectedPosition = position
                        selectedPosition = position
                        listener?.onDateClick(diasSemana[position])
                    }

                    // --- Notificar as mudanças para o RecyclerView ---
                    // 1. Notificar o item que estava selecionado anteriormente (se houver) para que ele seja redesenhado
                    previousSelectedPosition?.let {
                        notifyItemChanged(it)
                    }

                    // 2. Notificar o item que foi clicado (que pode ser o que acabou de ser desselecionado
                    //    ou o que acabou de ser selecionado)
                    notifyItemChanged(position)

                    // 3. SE a nova seleção não for o item clicado e não for o item que estava selecionado antes,
                    //    então notifique essa nova posição (isto é para o caso de o foco ir para "hoje")
                    if (newSelectedPosition != null && newSelectedPosition != position && newSelectedPosition != previousSelectedPosition) {
                        notifyItemChanged(newSelectedPosition)
                    }
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
        val calendarDia = Calendar.getInstance().apply { time = date }

        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val nomeDia = SimpleDateFormat("EEE", Locale("pt", "BR")).format(date).uppercase(Locale.getDefault())
        val numeroDia = calendarDia.get(Calendar.DAY_OF_MONTH)

        val ehHoje = hoje.get(Calendar.YEAR) == calendarDia.get(Calendar.YEAR) &&
                hoje.get(Calendar.MONTH) == calendarDia.get(Calendar.MONTH) &&
                hoje.get(Calendar.DAY_OF_MONTH) == numeroDia

        val ehDoMesPrincipal = mesPrincipalCalendar.get(Calendar.YEAR) == calendarDia.get(Calendar.YEAR) &&
                mesPrincipalCalendar.get(Calendar.MONTH) == calendarDia.get(Calendar.MONTH)

        val isSelecionado = selectedPosition == position // Este item é a seleção ativa (círculo branco)

        holder.nomeDiaTextView.text = nomeDia
        holder.numeroDiaTextView.text = numeroDia.toString()

        // --- Resetar estilos para evitar reciclagem incorreta ---
        holder.nomeDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        holder.numeroDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        holder.numeroDiaTextView.background = null
        holder.numeroDiaTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0) // Remove qualquer ponto/ícone

        // --- Lógica de coloração para dias de outros meses ---
        if (!ehDoMesPrincipal) {
            holder.nomeDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.grey_dark))
            holder.numeroDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.grey_dark))
        }

        // --- Lógica de Estilo Principal: Prioridades ---
        if (isSelecionado) {
            // PRIORIDADE 1: Este item está ativamente selecionado pelo usuário (círculo branco)
            holder.numeroDiaTextView.setBackgroundResource(R.drawable.circle_white)
            holder.numeroDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            holder.nomeDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        } else if (ehHoje && selectedPosition == null) {
            // PRIORIDADE 2: Se NÃO HÁ nenhuma seleção explícita E este é o dia de hoje,
            //               então "hoje" é o selecionado padrão (círculo branco)
            holder.numeroDiaTextView.setBackgroundResource(R.drawable.circle_white)
            holder.numeroDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            holder.nomeDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
        } else if (ehHoje) {
            // PRIORIDADE 3: Se é o dia de hoje, MAS existe uma seleção explícita em OUTRO lugar,
            //               mostra o ponto vermelho em "hoje".
            holder.numeroDiaTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ponto_vermelho)
            // Mantém as cores normais do mês principal se for o caso
            if (ehDoMesPrincipal) {
                holder.nomeDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
                holder.numeroDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            }
        } else {
            // PRIORIDADE 4: Estilos padrão para dias que não estão selecionados e não são "hoje"
            if (ehDoMesPrincipal) {
                holder.nomeDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
                holder.numeroDiaTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            }
        }
    }

    override fun getItemCount(): Int = diasSemana.size

    // Função auxiliar para encontrar o índice do dia atual
    private fun getTodayIndex(): Int {
        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return diasSemana.indexOfFirst {
            val tempCal = Calendar.getInstance().apply { time = it }
            tempCal.get(Calendar.YEAR) == hoje.get(Calendar.YEAR) &&
                    tempCal.get(Calendar.MONTH) == hoje.get(Calendar.MONTH) &&
                    tempCal.get(Calendar.DAY_OF_MONTH) == hoje.get(Calendar.DAY_OF_MONTH)
        }
    }
}