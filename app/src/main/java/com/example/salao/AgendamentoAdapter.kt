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
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import java.util.*

interface OnAgendamentoClickListener {
    fun onAgendamentoClick(agendamento: AgendamentoItem)
}

class AgendamentoAdapter(
    private var agendamentos: MutableList<AgendamentoItem>,
    private val clickListener: OnAgendamentoClickListener
) : RecyclerView.Adapter<AgendamentoAdapter.AgendamentoViewHolder>() {

    private val selectedItemsIds = mutableSetOf<Int>()

    val listaAgendamentos: List<AgendamentoItem>
        get() = agendamentos

    class AgendamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClienteNome: TextView = itemView.findViewById(R.id.tv_cliente_nome)
        val tvDataHora: TextView = itemView.findViewById(R.id.tv_data_hora)
        val tvProfissional: TextView = itemView.findViewById(R.id.tv_profissional)
        val tvObservacao: TextView = itemView.findViewById(R.id.tv_observacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendamentoViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_agendamento, parent, false)
        return AgendamentoViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AgendamentoViewHolder, position: Int) {
        val agendamento = agendamentos[position]
        val isSelected = agendamento.id != null && selectedItemsIds.contains(agendamento.id)

        val backgroundColorRes = if (selectedItemsIds.contains(agendamento.id)) {
            R.color.selected_item_background
        } else {
            R.color.default_item_background
        }
        holder.itemView.setBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                backgroundColorRes
            )
        )
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
                Log.e(
                    "AgendamentoAdapter",
                    "Falha ao parsear a hora: $horaOriginalString. Exibindo original."
                )
            }
        } catch (e: Exception) {
            holder.tvDataHora.text = agendamento.hora
            Log.e(
                "AgendamentoAdapter",
                "Erro ao formatar hora '${agendamento.hora}': ${e.message}. Exibindo original."
            )
        }

        holder.itemView.setOnClickListener {
            Log.d("AgendamentoAdapter", "Clique simples no item ${agendamento.id}. Alternando seleção.")
            agendamento.id?.let { agendamentoId ->
                toggleSelection(agendamentoId)
                clickListener.onAgendamentoClick(agendamento)
            }
        }
    }

    override fun getItemCount(): Int = agendamentos.size

    @SuppressLint("NotifyDataSetChanged")
    fun atualizarLista(novaListaAgendamentos: List<AgendamentoItem>) {
        agendamentos.clear()
        agendamentos.addAll(novaListaAgendamentos)
        clearSelection()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelection(agendamentoId: Int) {
        if (selectedItemsIds.contains(agendamentoId)) {
            selectedItemsIds.remove(agendamentoId)
            Log.d("AgendamentoAdapter", "Item ${agendamentoId} removido da seleção. Selecionados: $selectedItemsIds")
        } else {
            selectedItemsIds.add(agendamentoId)
            Log.d("AgendamentoAdapter", "Item ${agendamentoId} adicionado à seleção. Selecionados: $selectedItemsIds")
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedItemsIds.clear()
        Log.d("AgendamentoAdapter", "Seleção limpa.")
        notifyDataSetChanged()
    }

    fun getSelectedItemsIds(): Set<Int> {
        return selectedItemsIds
    }
}

data class AgendamentoItem(
    val id: Int?,
    val clienteNome: String,
    val data: String,
    val hora: String,
    val profissionalNome: String,
    val comentario: String? = null
)