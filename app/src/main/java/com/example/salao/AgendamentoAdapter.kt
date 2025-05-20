package com.example.salao

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AgendamentoAdapter(private var agendamentos: MutableList<AgendamentoItem>) :
    RecyclerView.Adapter<AgendamentoAdapter.AgendamentoViewHolder>() {

    private val itensSelecionados = mutableSetOf<Int>()
    private var onItemSelecionadoListener: ((Int, Boolean) -> Unit)? = null

    fun setOnItemSelecionadoListener(listener: (Int, Boolean) -> Unit) {
        this.onItemSelecionadoListener = listener
    }

    fun getItensSelecionados(): List<AgendamentoItem> {
        return itensSelecionados.map { agendamentos[it] }.toList()
    }

    fun removerItens(indices: List<Int>) {
        val sortedIndices = indices.sortedDescending()
        sortedIndices.forEach {
            agendamentos.removeAt(it)
        }
        itensSelecionados.clear()
        notifyDataSetChanged()
    }

    // Nova função pública para acessar a lista de agendamentos
    val listaAgendamentos: List<AgendamentoItem>
        get() = agendamentos

    class AgendamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvClienteNome: TextView = itemView.findViewById(R.id.tv_cliente_nome)
        val tvDataHora: TextView = itemView.findViewById(R.id.tv_data_hora)
        val tvProfissional: TextView = itemView.findViewById(R.id.tv_profissional)
        val tvObservacao: TextView = itemView.findViewById(R.id.tv_observacao)
        val checkboxSelecionar: CheckBox = itemView.findViewById(R.id.checkbox_selecionar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendamentoViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agendamento, parent, false)
        return AgendamentoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AgendamentoViewHolder, position: Int) {
        val agendamento = agendamentos[position]
        holder.tvClienteNome.text = agendamento.clienteNome
        holder.tvDataHora.text = "${agendamento.data} ${agendamento.hora}"
        holder.tvProfissional.text = agendamento.profissionalNome
        holder.tvObservacao.text = agendamento.comentario ?: "" //Tratamento nulo
        holder.checkboxSelecionar.isChecked = itensSelecionados.contains(position)

        holder.checkboxSelecionar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                itensSelecionados.add(position)
            } else {
                itensSelecionados.remove(position)
            }
            onItemSelecionadoListener?.invoke(position, isChecked)
        }
    }

    override fun getItemCount() = agendamentos.size

    fun atualizarLista(novaLista: List<AgendamentoItem>) {
        agendamentos.clear()
        agendamentos.addAll(novaLista)
        itensSelecionados.clear()
        notifyDataSetChanged()
    }
}

data class AgendamentoItem(
    val id: Int? = null,
    val clienteNome: String,
    val data: String,
    val hora: String,
    val profissionalNome: String,
    val comentario: String? = null //Permitir nulo
)
