package com.example.salao

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface OnAgendamentoClickListener {
    fun onAgendamentoClick(agendamento: AgendamentoItem)
}

class AgendamentoAdapter(
    private var agendamentos: MutableList<AgendamentoItem>,
    private val clickListener: OnAgendamentoClickListener
) : RecyclerView.Adapter<AgendamentoAdapter.AgendamentoViewHolder>() {


    val listaAgendamentos: List<AgendamentoItem>
        get() = agendamentos

    class AgendamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClienteNome: TextView = itemView.findViewById(R.id.tv_cliente_nome)
        val tvDataHora: TextView = itemView.findViewById(R.id.tv_data_hora)
        val tvProfissional: TextView = itemView.findViewById(R.id.tv_profissional)
        val tvObservacao: TextView = itemView.findViewById(R.id.tv_observacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendamentoViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agendamento, parent, false)
        return AgendamentoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AgendamentoViewHolder, position: Int) {
        val agendamento = agendamentos[position]
        holder.tvClienteNome.text = agendamento.clienteNome
        holder.tvProfissional.text = agendamento.profissionalNome
        holder.tvObservacao.text = agendamento.comentario ?: ""

        try {
            val horaOriginalString = agendamento.hora
            val inputTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val parsedTime: Date? = inputTimeFormat.parse(horaOriginalString)

            if (parsedTime != null) {
                val horaFormatada = outputTimeFormat.format(parsedTime)
                holder.tvDataHora.text = horaFormatada
            } else {
                holder.tvDataHora.text = horaOriginalString
                Log.e("AgendamentoAdapter", "Falha ao parsear a hora: $horaOriginalString. Exibindo original.")
            }
        } catch (e: Exception) {
            holder.tvDataHora.text = agendamento.hora
            Log.e("AgendamentoAdapter", "Erro ao formatar hora '${agendamento.hora}': ${e.message}. Exibindo original.")
        }

        holder.itemView.setOnClickListener {
            clickListener.onAgendamentoClick(agendamento)
        }
    }

    override fun getItemCount() = agendamentos.size

    fun atualizarLista(novaLista: List<AgendamentoItem>) {
        agendamentos.clear()
        agendamentos.addAll(novaLista)
        notifyDataSetChanged()
    }
}

data class AgendamentoItem(
    val id: Int? = null,
    val clienteNome: String,
    val data: String,
    val hora: String,
    val profissionalNome: String,
    val comentario: String? = null
)