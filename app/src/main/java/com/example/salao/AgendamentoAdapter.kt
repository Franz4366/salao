package com.example.salao

import android.util.Log // Importe Log para depuração
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat // Importe SimpleDateFormat
import java.util.Date // Importe Date
import java.util.Locale // Importe Locale

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
        holder.tvProfissional.text = agendamento.profissionalNome
        holder.tvObservacao.text = agendamento.comentario ?: ""

        try {
            val dataOriginalString = agendamento.data
            val horaOriginalString = agendamento.hora

            // Formato de entrada da data
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            // Formato de saída da data
            val outputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            val parsedDate: Date? = inputDateFormat.parse(dataOriginalString)

            if (parsedDate != null) {
                val dataFormatada = outputDateFormat.format(parsedDate)
                holder.tvDataHora.text = "$dataFormatada ${agendamento.hora}" // Concatena a data formatada com a hora
            } else {
                holder.tvDataHora.text = "Data Inválida ${agendamento.hora}" // Fallback para data nula
                Log.e("AgendamentoAdapter", "Falha ao parsear a data: $dataOriginalString")
            }
        } catch (e: Exception) {
            // Trate exceções de parsing se a data não estiver no formato esperado
            holder.tvDataHora.text = "Erro na Data/Hora" // Fallback genérico para erro
            Log.e("AgendamentoAdapter", "Erro ao formatar data/hora: ${e.message}")
        }
        // ----------------------------------------------------

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
    val comentario: String? = null
)
