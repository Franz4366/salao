package com.example.salao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.example.salao.network.SupabaseClient
import com.example.salao.utils.esconderBarrasDoSistema
import com.example.salao.utils.gerarDiasDoMes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class Agenda : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var listaAgendamentosRecyclerView: RecyclerView
    private lateinit var agendamentoAdapter: AgendamentoAdapter
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var selectedDate: Date? = null
    private lateinit var btnExcluir: FrameLayout
    private val agendamentosSelecionados = mutableListOf<AgendamentoItem>()
    private val formatoBancoDeDados = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val formatoExibicao = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agenda)

        esconderBarrasDoSistema(this)

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMes = findViewById(R.id.tv_mes)
        listaAgendamentosRecyclerView = findViewById(R.id.lista_agendamentos_recycler_view)
        btnExcluir = findViewById(R.id.btn_excluir)


        calendarRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        calendar = Calendar.getInstance()

        atualizarCalendario()

        val btnAnterior: ImageView = findViewById(R.id.seta_anterior)
        val btnProximo: ImageView = findViewById(R.id.seta_proximo)

        btnAnterior.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            atualizarCalendario()
            buscarAgendamentosParaData(calendar.time)
        }

        btnProximo.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            atualizarCalendario()
            buscarAgendamentosParaData(calendar.time)
        }

        setupNavigationIcons()
        setupAgendamentosList()
        setupExcluirButton()

        buscarAgendamentosParaData(calendar.time)
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            navigateToLogin(this)
        }
        findViewById<ImageView>(R.id.icon_agendar)?.setOnClickListener {
            navigateToAgendamento(this)
        }
        findViewById<ImageView>(R.id.icon_calendar)?.setOnClickListener {


        }
        findViewById<ImageView>(R.id.icon_add)?.setOnClickListener {
            navigateToCadastroCliente(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }

    private fun atualizarCalendario() {
        val dias = gerarDiasDoMes(calendar)

        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        val adapter = DiaSemanaAdapter(dias)
        adapter.setOnDateClickListener(object : OnDateClickListener {
            override fun onDateClick(date: Date) {

                selectedDate = date
                Log.d("Agenda", "Data selecionada: $date")
                buscarAgendamentosParaData(date)
            }
        })
        calendarRecyclerView.adapter = adapter
    }

    private fun setupAgendamentosList() {
        listaAgendamentosRecyclerView.layoutManager = LinearLayoutManager(this)
        agendamentoAdapter = AgendamentoAdapter(mutableListOf())  // Inicialize com uma lista vazia
        listaAgendamentosRecyclerView.adapter = agendamentoAdapter

        agendamentoAdapter.setOnItemSelecionadoListener { position, isChecked ->
            val agendamento = agendamentoAdapter.listaAgendamentos[position] // Use a lista do adapter
            if (isChecked) {
                agendamentosSelecionados.add(agendamento)
            } else {
                agendamentosSelecionados.remove(agendamento)
            }
            atualizarEstadoBotaoExcluir()
        }
    }

    private fun atualizarEstadoBotaoExcluir() {
        btnExcluir.isEnabled = agendamentosSelecionados.isNotEmpty()
    }

    private fun setupExcluirButton() {
        btnExcluir.isEnabled = false
        btnExcluir.setOnClickListener {
            Log.d("Agenda", "Botão Excluir clicado!")
            excluirAgendamentosSelecionados()
        }
    }

    private fun excluirAgendamentosSelecionados() {
        val idsSelecionados = agendamentosSelecionados.map { it.id }.filterNotNull()
        Log.d("Agenda", "Excluindo agendamentos com IDs: $idsSelecionados")

        lifecycleScope.launch {
            if (idsSelecionados.isNotEmpty()) {
                val resultado = supabaseClient.deletarAgendamentos(idsSelecionados)
                if (resultado) {
                    Log.d("Agenda", "Agendamentos excluídos com sucesso!")
                    buscarAgendamentosParaData(calendar.time)
                    agendamentosSelecionados.clear()
                    btnExcluir.isEnabled = false
                    mostrarToast("Agendamentos excluídos")
                } else {
                    Log.e("Agenda", "Erro ao excluir agendamentos.")
                    mostrarToast("Erro ao excluir agendamentos")
                }
            } else {
                Log.d("Agenda", "Nenhum agendamento selecionado para excluir.")
                mostrarToast("Nenhum agendamento selecionado")
            }
        }

    }

    private fun buscarAgendamentosParaData(data: Date) {
        coroutineScope.launch {
            var listaAgendamentoItems = mutableListOf<AgendamentoItem>()
            try {
                // Obtém a data atual ou a data desejada para a agenda
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dataParaAgenda = dateFormat.format(Date()) // Ou uma data selecionada pelo usuário

                val agendamentosSupabase = supabaseClient.getAgendamentosPorData(dataParaAgenda)
                Log.d("Agenda", "Agendamentos para a data $dataParaAgenda: $agendamentosSupabase")

                for (agendamentoSupabase in agendamentosSupabase) {
                    val cliente = supabaseClient.getClientePorId(agendamentoSupabase.clienteId)
                    val nomeCliente = cliente?.nome ?: "Cliente não encontrado"

                    // === Ponto Crucial: Buscar o nome do profissional ===
                    val profissionalUuid = agendamentoSupabase.profissionalId
                    val profissionalProfile = supabaseClient.getProfileById(profissionalUuid)
                    val nomeDoProfissionalExibicao = profissionalProfile?.nome ?: "Profissional não encontrado"

                    listaAgendamentoItems.add(
                        AgendamentoItem(
                            id = agendamentoSupabase.id,
                            clienteNome = nomeCliente,
                            data = agendamentoSupabase.dataAgendamento,
                            hora = agendamentoSupabase.horaAgendamento,
                            profissionalNome = nomeDoProfissionalExibicao, // <== Use o nome buscado aqui
                            comentario = agendamentoSupabase.comentario
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("Agenda", "Erro ao carregar agendamentos: ${e.message}")
                // Opcional: exibir uma mensagem de erro ao usuário
            }
            // Atualizar o adaptador da RecyclerView com os novos dados
            agendamentoAdapter.atualizarLista(listaAgendamentoItems)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    fun Context.mostrarToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }
}
