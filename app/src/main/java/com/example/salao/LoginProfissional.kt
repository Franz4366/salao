package com.example.salao

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToPerfilUsuario
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
import com.example.salao.model.Profile

class LoginProfissional : AppCompatActivity() {

    private lateinit var nomeProfissionalTextView: TextView
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var loggedInUserId: String? = null
    private var sessionToken: String? = null
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
        agendamentoAdapter = AgendamentoAdapter(mutableListOf(), object : OnAgendamentoClickListener {
            override fun onAgendamentoClick(agendamentoItem: AgendamentoItem) {
            }
        })
        recyclerView.adapter = agendamentoAdapter

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getString("user_id", null)
        sessionToken = sharedPreferences.getString("session_token", null)

        if (loggedInUserId != null && sessionToken != null) {
            Log.d("LoginProfissional", "ID do usuário e Token encontrados. Buscando dados...")
            coroutineScope.launch {
                val listaAgendamentoItems = mutableListOf<AgendamentoItem>()
                try {
                    buscarPerfilDoUsuario(loggedInUserId!!, sessionToken!!)

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dataAtual = dateFormat.format(Date())

                    val agendamentosSupabase = supabaseClient.getAgendamentosPorData(dataAtual, loggedInUserId, sessionToken!!)
                    Log.d("LoginProfissional", "Agendamentos do usuário: $loggedInUserId: $agendamentosSupabase")

                    if (agendamentosSupabase.isEmpty()) {
                        Log.d("LoginProfissional", "Nenhum agendamento encontrado para a data atual.")
                        agendamentoAdapter.atualizarLista(emptyList())

                    } else {
                        agendamentosSupabase.forEach { agendamentoSupabase ->
                            val cliente = supabaseClient.getClientePorId(agendamentoSupabase.clienteId, sessionToken!!)
                            val nomeCliente = cliente?.nome ?: "Cliente não encontrado"
                            val profissionalUuid = agendamentoSupabase.profissionalId

                            val profissionalProfile = supabaseClient.getProfileById(profissionalUuid, sessionToken!!)
                            val nomeDoProfissionalExibicao = profissionalProfile?.nome ?: "Profissional não encontrado"

                            val fullDateTime = "${agendamentoSupabase.dataAgendamento} ${agendamentoSupabase.horaAgendamento}"
                            val parsedDate: Date? = try {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(fullDateTime)
                            } catch (e: Exception) {
                                Log.e("LoginProfissional", "Erro ao parsear data e hora: $fullDateTime", e)
                                null
                            }

                            if (parsedDate != null) {
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
                            } else {
                                Log.e("LoginProfissional", "Agendamento ignorado devido a data/hora inválida: $fullDateTime")
                            }
                        }
                    }
                    agendamentoAdapter.atualizarLista(listaAgendamentoItems)
                } catch (e: Exception) {
                    Log.e("LoginProfissional", "Erro ao buscar agendamentos ou perfil: ${e.message}", e)
                    Toast.makeText(this@LoginProfissional, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
                    nomeProfissionalTextView.text = "Erro ao carregar nome ou agendamentos"
                    handleAuthenticationError(e)
                }
            }
        } else {
            Log.e("LoginProfissional", "ID do usuário ou token de sessão não encontrados nas SharedPreferences. Redirecionando para login.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
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

    private suspend fun buscarPerfilDoUsuario(userId: String, token: String) {
        try {
            val response: HttpResponse = supabaseClient.client.get("${supabaseClient.supabaseUrl}/rest/v1/profiles") {
                parameter("select", "id, nome, photo_url")
                parameter("id", "eq.$userId")
                header("Accept", "application/json")
                header("Authorization", "Bearer $token")
            }

            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                throw Exception("Erro ao buscar perfil do usuário: ${response.status.value} - $errorText")
            }

            val profiles: List<Profile> = response.body<List<Profile>>()
            if (profiles.isNotEmpty()) {
                val userProfile = profiles[0]
                nomeProfissionalTextView.text = userProfile.nome ?: "Nome não encontrado"
                Log.d("LoginProfissional", "Perfil do usuário carregado: ${userProfile.nome}")
            } else {
                nomeProfissionalTextView.text = "Usuário não encontrado"
                Log.w("LoginProfissional", "Nenhum perfil encontrado para o ID: $userId")
                handleAuthenticationError(Exception("Perfil não encontrado para o usuário logado."))
            }

        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar perfil do usuário: ${e.message}", e)
            nomeProfissionalTextView.text = "Erro ao carregar nome"
            handleAuthenticationError(e)
        }
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {

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
        findViewById<ImageView>(R.id.icon_user)?.setOnClickListener {

            navigateToPerfilUsuario(this)
        }
    }

    private fun handleAuthenticationError(e: Exception) {
        if (e.message?.contains("401 Unauthorized", ignoreCase = true) == true ||
            e.message?.contains("JWSError", ignoreCase = true) == true ||
            e.message?.contains("Perfil não encontrado para o usuário logado.", ignoreCase = true) == true) {
            Log.e("LoginProfissional", "Erro de autenticação (401 Unauthorized / JWSError / Perfil não encontrado). Deslogando usuário.")
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                remove("user_id")
                remove("session_token")
                apply()
            }
            Toast.makeText(this, "Sessão expirada ou inválida. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }
}