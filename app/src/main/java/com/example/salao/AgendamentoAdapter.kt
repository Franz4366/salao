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
    private var initialAgendamentos: List<AgendamentoItem>,
    private val clickListener: OnAgendamentoClickListener
) : RecyclerView.Adapter<AgendamentoAdapter.AgendamentoViewHolder>() {

    private val selectedItemsIds = mutableSetOf<Int>()
    private val agendamentos: MutableList<AgendamentoItem> = initialAgendamentos.toMutableList()

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

    companion object {
        private const val TAG = "AgendamentoAdapter"
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AgendamentoViewHolder, position: Int) {
        val agendamento = agendamentos[position]

        holder.itemView.isSelected = agendamento.id != null && selectedItemsIds.contains(agendamento.id)


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
        val oldSelectedId = selectedItemsIds.firstOrNull() // Assume only one selection

        if (oldSelectedId == agendamentoId) {
            // Se o item clicado já estava selecionado, desmarcar
            selectedItemsIds.clear()
            Log.d(TAG, "Item ${agendamentoId} desmarcado.")
            notifyItemChanged(agendamentos.indexOfFirst { it.id == agendamentoId })
        } else {
            // Se um novo item foi clicado ou não havia seleção,
            // desmarcar o antigo (se houver) e selecionar o novo
            selectedItemsIds.clear() // Limpa o antigo
            selectedItemsIds.add(agendamentoId) // Adiciona o novo
            Log.d(TAG, "Item ${agendamentoId} selecionado. Antigo: $oldSelectedId")

            if (oldSelectedId != null) {
                val oldPos = agendamentos.indexOfFirst { it.id == oldSelectedId }
                if (oldPos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldPos)
                }
            }
            val newPos = agendamentos.indexOfFirst { it.id == agendamentoId }
            if (newPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(newPos)
            }
        }
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