package com.example.salao

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToPerfilUsuario
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
import com.google.android.material.button.MaterialButton

class Agenda : AppCompatActivity(), OnAgendamentoClickListener {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var listaAgendamentosRecyclerView: RecyclerView
    private lateinit var agendamentoAdapter: AgendamentoAdapter
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var selectedDate: Date? = null
    private lateinit var btnExcluir: FrameLayout
    private lateinit var btnCompartilhar: MaterialButton

    private var loggedInUserId: String? = null
    private var sessionToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agenda)

        esconderBarrasDoSistema(this)

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getString("user_id", null)
        sessionToken = sharedPreferences.getString("session_token", null)

        if (sessionToken == null) {
            Log.e("Agenda", "Token de sessão não encontrado. Redirecionando para login.")
            Toast.makeText(this, "Sessão expirada ou inválida. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            navigateToLogin(this)
            finish()
            return
        }

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMes = findViewById(R.id.tv_mes)
        listaAgendamentosRecyclerView = findViewById(R.id.lista_agendamentos_recycler_view)
        btnExcluir = findViewById(R.id.btn_excluir)
        btnCompartilhar = findViewById(R.id.btn_compartilhar_print)

        calendarRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        calendar = Calendar.getInstance()

        setupNavigationIcons()
        setupAgendamentosList()
        setupExcluirButton()
        setupCompartilharButton()

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
        findViewById<ImageView>(R.id.icon_user)?.setOnClickListener {
            navigateToPerfilUsuario(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun atualizarCalendario() {
        val dias = gerarDiasDoMes(calendar)

        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        val adapter = DiaSemanaAdapter(dias, calendar)
        adapter.setOnDateClickListener(object : OnDateClickListener {
            override fun onDateClick(date: Date) {
                selectedDate = date
                Log.d("Agenda", "Data selecionada no calendário: $date")
                buscarAgendamentosParaData(date)
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
                val primeiroDiaMesPrincipal =
                    Calendar.getInstance().apply { time = primeiroDiaMesPrincipal }

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
        agendamentoAdapter = AgendamentoAdapter(mutableListOf(), this)
        listaAgendamentosRecyclerView.adapter = agendamentoAdapter
    }

    override fun onAgendamentoClick(agendamentoItem: AgendamentoItem) {
        Log.d("Agenda", "Agendamento clicado: $agendamentoItem")
        atualizarEstadoBotoesAcao()
    }

    private fun atualizarEstadoBotoesAcao() {
        val temSelecao = agendamentoAdapter.getSelectedItemsIds().isNotEmpty()
        Log.d("Agenda", "Atualizando estado dos botões. Tem seleção: $temSelecao. IDs selecionados: ${agendamentoAdapter.getSelectedItemsIds()}")
        btnExcluir.isEnabled = temSelecao
        btnCompartilhar.isEnabled = temSelecao
        btnExcluir.alpha = if (temSelecao) 1.0f else 0.5f
        btnCompartilhar.alpha = if (temSelecao) 1.0f else 0.5f
    }

    private fun setupExcluirButton() {
        btnExcluir.isEnabled = false
        btnExcluir.alpha = 0.5f
        btnExcluir.setOnClickListener {
            Log.d("Agenda", "Botão Excluir clicado!")
            excluirAgendamentosSelecionados()
        }
    }

    private fun setupCompartilharButton() {
        btnCompartilhar.isEnabled = false
        btnCompartilhar.alpha = 0.5f
        btnCompartilhar.setOnClickListener {
            Log.d("Agenda", "Botão Compartilhar clicado!")
            compartilharAgendamentosSelecionados()
        }
    }

    private fun excluirAgendamentosSelecionados() {
        val idsSelecionados = agendamentoAdapter.getSelectedItemsIds().toList()
        Log.d("Agenda", "Excluindo agendamentos com IDs: $idsSelecionados")

        if (idsSelecionados.isEmpty()) {
            mostrarToast("Nenhum agendamento selecionado para excluir.")
            return
        }

        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            mostrarToast("Erro: Token de autenticação não encontrado. Faça login novamente.")
            navigateToLogin(this)
            finish()
            return
        }

        lifecycleScope.launch {
            try {

                val resultado = supabaseClient.deletarAgendamentos(idsSelecionados)
                if (resultado) {
                    Log.d("Agenda", "Agendamentos excluídos com sucesso!")
                    mostrarToast("Agendamentos excluídos")
                    agendamentoAdapter.clearSelection()
                    selectedDate?.let { buscarAgendamentosParaData(it) }
                    atualizarEstadoBotoesAcao()
                } else {
                    Log.e("Agenda", "Erro ao excluir agendamentos.")
                    mostrarToast("Erro ao excluir agendamentos")
                }
            } catch (e: Exception) {
                Log.e("Agenda", "Exceção ao excluir agendamentos: ${e.message}", e)
                mostrarToast("Erro ao excluir agendamentos: ${e.message}")
            }
        }
    }

    private fun compartilharAgendamentosSelecionados() {
        val idsSelecionados = agendamentoAdapter.getSelectedItemsIds().toList()
        Log.d("Agenda", "Compartilhando agendamentos com IDs: $idsSelecionados")

        if (idsSelecionados.size == 1) {
            val idUnicoSelecionado = idsSelecionados.first()
            val agendamentoParaDetalhes =
                agendamentoAdapter.listaAgendamentos.find { it.id == idUnicoSelecionado }

            if (agendamentoParaDetalhes != null) {
                val intent = Intent(this, DetalhesAgendamentoActivity::class.java).apply {
                    putExtra("clienteNome", agendamentoParaDetalhes.clienteNome)
                    putExtra("data", agendamentoParaDetalhes.data)
                    putExtra("hora", agendamentoParaDetalhes.hora)
                    putExtra("profissionalNome", agendamentoParaDetalhes.profissionalNome)
                    putExtra("comentario", agendamentoParaDetalhes.comentario)
                }
                startActivity(intent)
                agendamentoAdapter.clearSelection()
                atualizarEstadoBotoesAcao()
            } else {
                mostrarToast("Agendamento não encontrado.")
            }
        } else if (idsSelecionados.isEmpty()) {
            mostrarToast("Por favor, selecione um agendamento para compartilhar.")
        } else {
            mostrarToast("Por favor, selecione apenas um agendamento para compartilhar.")
            agendamentoAdapter.clearSelection()
            atualizarEstadoBotoesAcao()
        }
    }

    private fun buscarAgendamentosParaData(data: Date) {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            Log.e("Agenda", "Token de sessão é nulo ao buscar agendamentos. Redirecionando para login.")
            Toast.makeText(this, "Erro de autenticação. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            navigateToLogin(this)
            finish()
            return
        }

        coroutineScope.launch {
            val listaAgendamentoItems = mutableListOf<AgendamentoItem>()
            try {
                val dataFormatadaParaSupabase = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(data)

                val agendamentosSupabase: List<AgendamentoSupabase> = supabaseClient.getAgendamentosPorData(dataFormatadaParaSupabase, null, currentSessionToken)
                Log.d("Agenda", "Agendamentos para a data $dataFormatadaParaSupabase carregados: $agendamentosSupabase")

                for (agendamentoSupabase in agendamentosSupabase) {
                    val cliente: Cliente? = supabaseClient.getClientePorId(agendamentoSupabase.clienteId, currentSessionToken)
                    val nomeCliente = cliente?.nome ?: "Cliente não encontrado"

                    val profissionalUuid = agendamentoSupabase.profissionalId
                    val profissionalProfile: Profile? = supabaseClient.getProfileById(profissionalUuid, currentSessionToken)
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
                Log.e("Agenda", "Erro ao carregar agendamentos: ${e.message}", e)
                Toast.makeText(this@Agenda, "Erro ao carregar agendamentos: ${e.message}", Toast.LENGTH_LONG).show()
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    Log.e("Agenda", "Erro de autenticação (401 Unauthorized / JWSError). Deslogando usuário.")
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        remove("user_id")
                        remove("session_token")
                        apply()
                    }
                    Toast.makeText(this@Agenda, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
                    navigateToLogin(this@Agenda)
                    finish()
                }
            }
            agendamentoAdapter.atualizarLista(listaAgendamentoItems)
            atualizarEstadoBotoesAcao()
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