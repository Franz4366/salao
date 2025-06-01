package com.example.salao

import android.content.Context
import com.example.salao.utils.gerarDiasDoMes
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
import com.example.salaa.DiaSemanaAdapter
import com.example.salaa.OnDateClickListener
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.example.salao.network.SupabaseClient
import com.example.salao.utils.esconderBarrasDoSistema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.salao.model.AgendamentoSupabase
import com.example.salao.model.Cliente
import com.example.salao.model.Profile

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

        calendar = Calendar.getInstance() // Inicializa o calendário para a data atual

        setupNavigationIcons()
        setupAgendamentosList()
        setupExcluirButton()

        // Garante que o calendário e os agendamentos sejam carregados na inicialização
        atualizarCalendario()

        val btnAnterior: ImageView = findViewById(R.id.seta_anterior)
        val btnProximo: ImageView = findViewById(R.id.seta_proximo)

        btnAnterior.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            atualizarCalendario()
        }

        btnProximo.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            atualizarCalendario()
            selectedDate = calendar.time
            buscarAgendamentosParaData(selectedDate!!)
        }
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

        val adapter = DiaSemanaAdapter(dias, calendar)
        adapter.setOnDateClickListener(object : OnDateClickListener {
            override fun onDateClick(date: Date) {
                selectedDate = date // Atualiza a data selecionada
                Log.d("Agenda", "Data selecionada no calendário: $date")
                buscarAgendamentosParaData(date) // Carrega agendamentos para a nova data selecionada
            }
        })
        calendarRecyclerView.adapter = adapter

        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        var initialScrollPosition = 0
        var initialSelectedData: Date? = null

        val indexHojeNaLista = dias.indexOfFirst {
            val tempCal = Calendar.getInstance().apply { time = it }
            tempCal.get(Calendar.DAY_OF_MONTH) == hoje.get(Calendar.DAY_OF_MONTH) &&
                    tempCal.get(Calendar.MONTH) == hoje.get(Calendar.MONTH) &&
                    tempCal.get(Calendar.YEAR) == hoje.get(Calendar.YEAR)
        }

        if (indexHojeNaLista != -1) {
             initialScrollPosition = indexHojeNaLista
             initialSelectedData = hoje.time
        } else {
            val primeiroDiaMesPrincipal = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
            }.time

            val indexPrimeiroDiaMesPrincipal = dias.indexOfFirst {
                val tempCal = Calendar.getInstance().apply { time = it }
                val primeiroDiaMesPrincipal = Calendar.getInstance().apply { time = primeiroDiaMesPrincipal }

                tempCal.get(Calendar.DAY_OF_MONTH) == primeiroDiaMesPrincipal.get(Calendar.DAY_OF_MONTH) &&
                tempCal.get(Calendar.MONTH) == primeiroDiaMesPrincipal.get(Calendar.MONTH) &&
                tempCal.get(Calendar.YEAR) == primeiroDiaMesPrincipal.get(Calendar.YEAR)
            }
            if (indexPrimeiroDiaMesPrincipal != -1) {
                initialScrollPosition = indexPrimeiroDiaMesPrincipal
                initialSelectedData = primeiroDiaMesPrincipal
            }
        }
        if (initialSelectedData != null) {
            calendarRecyclerView.scrollToPosition(initialScrollPosition)
            adapter.selectedPosition = initialScrollPosition
            adapter.notifyDataSetChanged()
            selectedDate = initialSelectedData
            buscarAgendamentosParaData(selectedDate!!)
        } else {
            adapter.selectedPosition = null
            adapter.notifyDataSetChanged()
            selectedDate = null
        }
    }
    private fun setupAgendamentosList() {
        listaAgendamentosRecyclerView.layoutManager = LinearLayoutManager(this)
        agendamentoAdapter = AgendamentoAdapter(mutableListOf())
        listaAgendamentosRecyclerView.adapter = agendamentoAdapter

        agendamentoAdapter.setOnItemSelecionadoListener { position, isChecked ->
            val agendamento = agendamentoAdapter.listaAgendamentos[position]
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
                    mostrarToast("Agendamentos excluídos")
                    selectedDate?.let { buscarAgendamentosParaData(it) }
                    agendamentosSelecionados.clear()
                    btnExcluir.isEnabled = false
                    atualizarEstadoBotaoExcluir()
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
            val listaAgendamentoItems = mutableListOf<AgendamentoItem>()
            try {
                // Formata a data recebida para o formato esperado pelo Supabase
                val dataFormatadaParaSupabase = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(data)

                val agendamentosSupabase: List<AgendamentoSupabase> = supabaseClient.getAgendamentosPorData(dataFormatadaParaSupabase)
                Log.d("Agenda", "Agendamentos para a data $dataFormatadaParaSupabase carregados: $agendamentosSupabase")

                for (agendamentoSupabase in agendamentosSupabase) {
                    val cliente: Cliente? = supabaseClient.getClientePorId(agendamentoSupabase.clienteId)
                    val nomeCliente = cliente?.nome ?: "Cliente não encontrado"

                    val profissionalUuid = agendamentoSupabase.profissionalId
                    val profissionalProfile: Profile? = supabaseClient.getProfileById(profissionalUuid)
                    val nomeDoProfissionalExibicao = profissionalProfile?.nome ?: "Profissional não encontrado"

                    listaAgendamentoItems.add(
                        AgendamentoItem(
                            id = agendamentoSupabase.id,
                            clienteNome = nomeCliente,
                            data = agendamentoSupabase.dataAgendamento,
                            hora = agendamentoSupabase.horaAgendamento,
                            profissionalNome = nomeDoProfissionalExibicao,
                            comentario = agendamentoSupabase.comentario
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("Agenda", "Erro ao carregar agendamentos: ${e.message}")
                mostrarToast("Erro ao carregar agendamentos.")
            }
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