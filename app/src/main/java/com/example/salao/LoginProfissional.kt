package com.example.salao

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.salao.utils.BirthdayClientsAdapter
import com.example.salao.utils.esconderBarrasDoSistema
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale

class LoginProfissional : AppCompatActivity() {

    private lateinit var nomeProfissionalTextView: TextView
    private lateinit var fotoPerfilProfissionalImageView: ImageView
    private lateinit var tvNoAgendamentosToday: TextView
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // --- REMOVIDO: Não é mais necessário, pois SupabaseClient gerencia isso ---
    // private var loggedInUserId: String? = null
    // private var sessionToken: String? = null

    private lateinit var agendamentoAdapter: AgendamentoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var birthdayClientsRecyclerView: RecyclerView
    private lateinit var birthdayClientsAdapter: BirthdayClientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_professional_dashboard)
        esconderBarrasDoSistema(this)

        val currentAccessToken = SupabaseClient.getAccessToken()
        val loggedInUserId = SupabaseClient.getLoggedInUserId()

        birthdayClientsRecyclerView = findViewById(R.id.birthday_clients_recycler_view)
        birthdayClientsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        birthdayClientsAdapter = BirthdayClientsAdapter(emptyList())
        birthdayClientsRecyclerView.adapter = birthdayClientsAdapter

        nomeProfissionalTextView = findViewById(R.id.nome_profissional)
        fotoPerfilProfissionalImageView = findViewById(R.id.foto_user)
        tvNoAgendamentosToday = findViewById(R.id.tv_no_agendamentos_today)

        recyclerView = findViewById(R.id.lista_agendamentos_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        agendamentoAdapter = AgendamentoAdapter(mutableListOf(), object : OnAgendamentoClickListener {
            override fun onAgendamentoClick(agendamentoItem: AgendamentoItem) {
                // Lidar com o clique, se necessário, por exemplo, navegar para detalhes do Agendamento
            }
        })
        recyclerView.adapter = agendamentoAdapter

        // --- REMOVIDO: Não é mais lido de SharedPreferences aqui, SupabaseClient gerencia ---
        // val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        // loggedInUserId = sharedPreferences.getString("user_id", null)
        // sessionToken = sharedPreferences.getString("session_token", null)

        if (loggedInUserId != null && currentAccessToken != null) { // Use currentAccessToken diretamente
            Log.d("LoginProfissional", "ID do usuário e Token encontrados. Buscando dados...")
            coroutineScope.launch {
                try {
                    buscarPerfilDoUsuario(loggedInUserId, currentAccessToken) // Passe currentAccessToken
                    fetchAgendamentos() // Esta função obterá o token por si mesma
                    fetchBirthdayClients() // Esta função obterá o token por si mesma
                } catch (e: Exception) {
                    Log.e("LoginProfissional", "Erro durante a inicialização: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginProfissional, "Erro ao carregar dados iniciais: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    handleAuthenticationError(e) // Chame seu manipulador de erro centralizado
                }
            }
        } else {
            Log.e("LoginProfissional", "ID do usuário ou token de sessão não encontrados. Redirecionando para login.")
            // --- ATUALIZADO: Chame handleAuthenticationError para um fluxo de logout consistente ---
            handleAuthenticationError(Exception("ID do usuário ou token de sessão não encontrados. Redirecionando para o login."))
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
            // --- ATUALIZADO: Chame SupabaseClient diretamente ---
            val profile = SupabaseClient.getProfileById(userId, token)
            withContext(Dispatchers.Main) {
                if (profile != null) {
                    nomeProfissionalTextView.text = profile.nome ?: "Nome não encontrado"

                    if (profile.fotoUrl != null && profile.fotoUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(profile.fotoUrl)
                            .transform(CircleTransform())
                            .placeholder(R.drawable.ellipse_2)
                            .error(R.drawable.ellipse_2)
                            .into(fotoPerfilProfissionalImageView)
                        Log.d("LoginProfissional", "Foto do perfil carregada de: ${profile.fotoUrl}")
                    } else {
                        fotoPerfilProfissionalImageView.setImageResource(R.drawable.ellipse_2)
                        Log.d("LoginProfissional", "URL da foto do perfil não encontrada, usando placeholder.")
                    }
                } else {
                    nomeProfissionalTextView.text = "Usuário não encontrado"
                    fotoPerfilProfissionalImageView.setImageResource(R.drawable.ellipse_2)
                    Log.w("LoginProfissional", "Nenhum perfil encontrado para o ID: $userId")
                    handleAuthenticationError(Exception("Perfil não encontrado para o usuário logado."))
                }
            }
        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar perfil do usuário: ${e.message}", e)
            withContext(Dispatchers.Main) {
                nomeProfissionalTextView.text = "Erro ao carregar nome"
                fotoPerfilProfissionalImageView.setImageResource(R.drawable.ellipse_2)
                handleAuthenticationError(e) // Centralize o tratamento de erros
            }
        }
    }

    // Mantenha a classe CircleTransform aqui, ou mova para um arquivo de utilidade compartilhado se usado em outro lugar.
    class CircleTransform : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = Math.min(source.width, source.height)
            val x = (source.width - size) / 2
            val y = (source.height - size) / 2
            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = android.graphics.BitmapShader(
                squaredBitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = shader
            paint.isAntiAlias = true
            val r = size / 2f
            canvas.drawCircle(r, r, r, paint)
            squaredBitmap.recycle()
            return bitmap
        }
        override fun key(): String {
            return "circle"
        }
    }


    private suspend fun fetchAgendamentos() {
        val listaAgendamentoItems = mutableListOf<AgendamentoItem>()
        // --- ATUALIZADO: Obtenha o token diretamente do SupabaseClient ---
        val currentAccessToken = SupabaseClient.getAccessToken()
        val loggedInUserId = SupabaseClient.getLoggedInUserId() // Obtenha o ID do usuário também do SupabaseClient

        if (currentAccessToken == null || loggedInUserId == null) {
            Log.e("LoginProfissional", "Token de sessão ou ID do usuário são nulos. Não é possível buscar agendamentos.")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginProfissional, "Sessão inválida. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
                handleAuthenticationError(Exception("Token de sessão ou ID do usuário é nulo."))
            }
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dataAtual = dateFormat.format(Date())

            // --- ATUALIZADO: Chame SupabaseClient diretamente ---
            val agendamentosSupabase = SupabaseClient.getAgendamentosPorData(dataAtual, loggedInUserId, currentAccessToken)
            Log.d("LoginProfissional", "Agendamentos para o usuário $loggedInUserId na data atual: ${agendamentosSupabase.size}")

            withContext(Dispatchers.Main) {
                if (agendamentosSupabase.isEmpty()) {
                    Log.d("LoginProfissional", "Nenhum agendamento encontrado para a data atual. Exibindo mensagem.")
                    agendamentoAdapter.atualizarLista(emptyList())
                    recyclerView.visibility = View.GONE
                    tvNoAgendamentosToday.visibility = View.VISIBLE
                } else {
                    agendamentosSupabase.forEach { agendamentoSupabase ->
                        // --- ATUALIZADO: Chame SupabaseClient diretamente ---
                        val cliente = SupabaseClient.getClientePorId(agendamentoSupabase.clienteId, currentAccessToken)
                        val nomeCliente = cliente?.nome ?: "Cliente não encontrado"

                        // --- ATUALIZADO: Chame SupabaseClient diretamente ---
                        val profissionalProfile = SupabaseClient.getProfileById(agendamentoSupabase.profissionalId, currentAccessToken)
                        val nomeDoProfissionalExibicao = profissionalProfile?.nome ?: "Profissional não encontrado"

                        val fullDateTime = "${agendamentoSupabase.dataAgendamento} ${agendamentoSupabase.horaAgendamento}"
                        val parsedDate: Date? = try {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(fullDateTime)
                        } catch (e: Exception) {
                            Log.e("LoginProfissional", "Erro ao parsear data e hora do agendamento: $fullDateTime", e)
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
                    listaAgendamentoItems.sortBy { it.hora }
                    agendamentoAdapter.atualizarLista(listaAgendamentoItems)
                    recyclerView.visibility = View.VISIBLE
                    tvNoAgendamentosToday.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar agendamentos: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginProfissional, "Erro ao carregar agendamentos: ${e.message}", Toast.LENGTH_LONG).show()
                recyclerView.visibility = View.GONE
                tvNoAgendamentosToday.visibility = View.VISIBLE
                tvNoAgendamentosToday.text = "Erro ao carregar agendamentos."
                handleAuthenticationError(e) // Centralize o tratamento de erros
            }
        }
    }

    private suspend fun fetchBirthdayClients() {
        // --- ATUALIZADO: Obtenha o token diretamente do SupabaseClient ---
        val currentAccessToken = SupabaseClient.getAccessToken()
        if (currentAccessToken == null) {
            Log.e("LoginProfissional", "Token de sessão é nulo. Não é possível buscar aniversariantes.")
            withContext(Dispatchers.Main) {
                handleAuthenticationError(Exception("Token de sessão é nulo para clientes de aniversário."))
            }
            return
        }

        try {
            // --- ATUALIZADO: Chame SupabaseClient diretamente ---
            val allClients = SupabaseClient.getAllClientes(currentAccessToken)
            Log.d("LoginProfissional", "Total de clientes encontrados para verificar aniversário: ${allClients.size}")

            val today = LocalDate.now()
            val birthdayClients = allClients.filter { client ->
                client.dataNascimento?.let { dobString ->
                    try {
                        val parsedDate = LocalDate.parse(dobString)
                        Log.d("LoginProfissional", "Verificando aniversário para ${client.nome}: ${parsedDate.monthValue}/${parsedDate.dayOfMonth} (Data DB: $dobString)")
                        parsedDate.monthValue == today.monthValue && parsedDate.dayOfMonth == today.dayOfMonth
                    } catch (e: DateTimeParseException) {
                        Log.e("LoginProfissional", "Erro ao analisar data de nascimento para ${client.nome} (Data: '$dobString'): ${e.message}")
                        false
                    } catch (e: Exception) {
                        Log.e("LoginProfissional", "Erro inesperado ao processar data de nascimento para ${client.nome} (Data: '$dobString'): ${e.message}")
                        false
                    }
                } ?: false
            }

            Log.d("LoginProfissional", "Aniversariantes encontrados hoje: ${birthdayClients.size}")

            withContext(Dispatchers.Main) {
                // Use o adaptador que já existe e chame o método updateClients
                birthdayClientsAdapter.updateClients(birthdayClients)
            }
        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar clientes aniversariantes: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginProfissional, "Erro ao carregar aniversariantes: ${e.message}", Toast.LENGTH_LONG).show()
                // Em caso de erro, ainda passamos uma lista vazia para o adaptador para exibir a mensagem de "não há"
                birthdayClientsAdapter.updateClients(emptyList())
                handleAuthenticationError(e) // Centralize o tratamento de erros
            }
        }
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            // Já está no dashboard, não faz nada ou navega para si mesmo para atualizar se necessário
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
            e.message?.contains("Perfil não encontrado para o usuário logado.", ignoreCase = true) == true ||
            e.message?.contains("Token de sessão é nulo", ignoreCase = true) == true) { // Adicionada verificação de mensagem específica
            Log.e("LoginProfissional", "Erro de autenticação detectado. Limpando tokens e redirecionando para login.")
            // --- ATUALIZADO: Use SupabaseClient.clearTokens() para gerenciamento centralizado de tokens ---
            SupabaseClient.clearTokens()
            Toast.makeText(this, "Sessão expirada ou inválida. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java) // Assumindo que MainActivity é sua tela de login
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpa a pilha de atividades
            startActivity(intent)
            finish() // Finaliza a atividade atual
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }
}