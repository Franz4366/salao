package com.example.salao

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.network.SupabaseClient
import com.example.salao.utils.esconderBarrasDoSistema
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// NOVOS IMPORTS PARA AS CLASSES DE MODELO UNIFICADAS
import com.example.salao.model.Profile // Agora importando do pacote model!
import com.example.salao.model.Cliente // Para o método getClientePorId
import com.example.salao.model.AgendamentoSupabase // Para os agendamentos

class LoginProfissional : AppCompatActivity() {

    private lateinit var nomeProfissionalTextView: TextView
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var loggedInUserId: String? = null
    private lateinit var agendamentoAdapter: AgendamentoAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_profissional)
        esconderBarrasDoSistema(this)

        nomeProfissionalTextView = findViewById(R.id.nome_profissional)
        recyclerView = findViewById(R.id.lista_agendamentos_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        agendamentoAdapter = AgendamentoAdapter(mutableListOf())
        recyclerView.adapter = agendamentoAdapter

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getString("user_id", null)

        if (loggedInUserId != null) {
            coroutineScope.launch {
                val listaAgendamentoItems = mutableListOf<AgendamentoItem>() // Alterado para 'val' pois a lista é mutável internamente
                try {
                    buscarPerfilDoUsuario(loggedInUserId!!)

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dataAtual = dateFormat.format(Date())

                    val agendamentosSupabase = supabaseClient.getAgendamentosPorData(dataAtual, loggedInUserId)
                    Log.d("LoginProfissional", "Agendamentos do usuário: $loggedInUserId: $agendamentosSupabase")

                    if (agendamentosSupabase.isEmpty()) {
                        Log.d("LoginProfissional", "Nenhum agendamento encontrado para a data atual.")
                        // Aqui, você pode querer limpar o adapter ou mostrar uma mensagem
                        agendamentoAdapter.atualizarLista(emptyList()) // Limpa a lista se não houver agendamentos
                        return@launch
                    }

                    agendamentosSupabase.forEach { agendamentoSupabase ->
                        val cliente = supabaseClient.getClientePorId(agendamentoSupabase.clienteId)
                        val nomeCliente = cliente?.nome ?: "Cliente não encontrado"
                        val profissionalUuid = agendamentoSupabase.profissionalId
                        // Use a classe Profile unificada aqui
                        val profissionalProfile = supabaseClient.getProfileById(profissionalUuid)
                        val nomeDoProfissionalExibicao = profissionalProfile?.nome ?: "Profissional não encontrado"


                        val fullDateTime = "${agendamentoSupabase.dataAgendamento} ${agendamentoSupabase.horaAgendamento}"
                        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val parsedDate: Date = dateTimeFormatter.parse(fullDateTime)!!

                        // Estas duas linhas são redundantes se você já tem os componentes separados
                        // val datePart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)
                        // val timePart = SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)


                        listaAgendamentoItems.add(
                            AgendamentoItem(
                                id = agendamentoSupabase.id,
                                clienteNome = nomeCliente,
                                data = agendamentoSupabase.dataAgendamento, // Usando a data original
                                hora = agendamentoSupabase.horaAgendamento, // Usando a hora original
                                profissionalNome = nomeDoProfissionalExibicao,
                                comentario = agendamentoSupabase.comentario
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("LoginProfissional", "Erro ao buscar agendamentos ou perfil: ${e.message}")
                }
                agendamentoAdapter.atualizarLista(listaAgendamentoItems)
            }
        } else {
            Log.e("LoginProfissional", "ID do usuário não encontrado nas SharedPreferences.")
            nomeProfissionalTextView.text = "Erro ao obter ID"
        }

        setupNavigationIcons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    // Método para buscar perfil do usuário, agora usando a classe Profile unificada
    private suspend fun buscarPerfilDoUsuario(userId: String) {
        try {
            val response: HttpResponse = supabaseClient.client.get("${supabaseClient.supabaseUrl}/rest/v1/profiles") {
                parameter("select", "id, nome, photo_url")
                parameter("id", "eq.$userId")
                header("Accept", "application/json")
            }

            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                throw Exception("Erro ao buscar perfil do usuário: $errorText")
            }

            // Use a classe Profile unificada aqui
            val profiles: List<Profile> = response.body<List<Profile>>()
            if (profiles.isNotEmpty()) {
                val userProfile = profiles[0]
                nomeProfissionalTextView.text = userProfile.nome ?: "Nome não encontrado"
            } else {
                nomeProfissionalTextView.text = "Usuário não encontrado"
            }

        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar perfil do usuário: ${e.message}")
            nomeProfissionalTextView.text = "Erro ao carregar nome"
        }
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            // Ação para o ícone home (já está na tela home)
        }
        findViewById<ImageView>(R.id.icon_agendar)?.setOnClickListener {
            navigateToAgendamento(this)
        }
        findViewById<ImageView>(R.id.icon_calendar)?.setOnClickListener {
            navigateToAgenda(this)
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
}