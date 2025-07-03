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
// import com.example.salao.Agendamento.CircleTransform // Se a CircleTransform for de outro arquivo, mantenha
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToPerfilUsuario
import com.example.salao.model.AgendamentoSupabase
import com.example.salao.model.Cliente
import com.example.salao.model.Profile
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
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var loggedInUserId: String? = null
    private var sessionToken: String? = null
    private lateinit var agendamentoAdapter: AgendamentoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var birthdayClientsRecyclerView: RecyclerView
    private lateinit var birthdayClientsAdapter: BirthdayClientsAdapter // Mantenha esta linha

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_profissional)
        esconderBarrasDoSistema(this)

        birthdayClientsRecyclerView = findViewById(R.id.birthday_clients_recycler_view)
        birthdayClientsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Inicialize o adaptador AQUI com uma lista vazia
        birthdayClientsAdapter = BirthdayClientsAdapter(emptyList())
        birthdayClientsRecyclerView.adapter = birthdayClientsAdapter // Atribua o adaptador ao RecyclerView

        // REMOVA ESTA LINHA: tvNoBirthdaysToday = findViewById(R.id.tv_no_birthdays_today) // REMOVA ESTA LINHA!

        nomeProfissionalTextView = findViewById(R.id.nome_profissional)
        fotoPerfilProfissionalImageView = findViewById(R.id.foto_user)
        tvNoAgendamentosToday = findViewById(R.id.tv_no_agendamentos_today)

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
                try {
                    buscarPerfilDoUsuario(loggedInUserId!!, sessionToken!!)
                    fetchAgendamentos()
                    fetchBirthdayClients() // Esta função agora usará o 'birthdayClientsAdapter' que já existe
                } catch (e: Exception) {
                    Log.e("LoginProfissional", "Erro durante a inicialização: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginProfissional, "Erro ao carregar dados iniciais: ${e.message}", Toast.LENGTH_LONG).show()
                    }
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
            val profile = supabaseClient.getProfileById(userId, token)
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
                handleAuthenticationError(e)
            }
        }
    }

    // Mantenha a classe CircleTransform aqui, se for usada apenas nesta Activity.
    // Se for usada em mais lugares, crie um arquivo separado (utils/CircleTransform.kt)
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
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dataAtual = dateFormat.format(Date())

            val currentSessionToken = sessionToken
            if (currentSessionToken == null) {
                Log.e("LoginProfissional", "Token de sessão é nulo. Não é possível buscar agendamentos.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginProfissional, "Sessão inválida. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
                }
                return
            }

            val agendamentosSupabase = supabaseClient.getAgendamentosPorData(dataAtual, loggedInUserId, currentSessionToken)
            Log.d("LoginProfissional", "Agendamentos para o usuário $loggedInUserId na data atual: ${agendamentosSupabase.size}")

            withContext(Dispatchers.Main) {
                if (agendamentosSupabase.isEmpty()) {
                    Log.d("LoginProfissional", "Nenhum agendamento encontrado para a data atual. Exibindo mensagem.")
                    agendamentoAdapter.atualizarLista(emptyList())
                    recyclerView.visibility = View.GONE
                    tvNoAgendamentosToday.visibility = View.VISIBLE
                } else {
                    agendamentosSupabase.forEach { agendamentoSupabase ->
                        val cliente = supabaseClient.getClientePorId(agendamentoSupabase.clienteId, currentSessionToken)
                        val nomeCliente = cliente?.nome ?: "Cliente não encontrado"

                        val profissionalProfile = supabaseClient.getProfileById(agendamentoSupabase.profissionalId, currentSessionToken)
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
            }
        }
    }

    private suspend fun fetchBirthdayClients() {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            Log.e("LoginProfissional", "Token de sessão é nulo. Não é possível buscar aniversariantes.")
            return
        }

        try {
            val allClients = supabaseClient.getAllClientes(currentSessionToken)
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
                // Remova qualquer lógica de visibilidade para birthdayClientsRecyclerView e tvNoBirthdaysToday aqui
                // birthdayClientsRecyclerView.visibility = View.VISIBLE // Isso é agora gerenciado pelo adaptador, que sempre desenha um item
                // tvNoBirthdaysToday.visibility = View.GONE // Esta TextView não existe mais/não é usada para isso
            }
        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar clientes aniversariantes: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginProfissional, "Erro ao carregar aniversariantes: ${e.message}", Toast.LENGTH_LONG).show()
                // Em caso de erro, ainda passamos uma lista vazia para o adaptador para exibir a mensagem de "não há"
                birthdayClientsAdapter.updateClients(emptyList())
                // Remova a lógica de visibilidade para o erro também
                // birthdayClientsRecyclerView.visibility = View.GONE
                // tvNoBirthdaysToday.visibility = View.VISIBLE
                // tvNoBirthdaysToday.text = "Erro ao carregar aniversariantes."
            }
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