package com.example.salao

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class LoginProfissional : AppCompatActivity() {

    private lateinit var nomeProfissionalTextView: TextView
    private lateinit var fotoProfissionalImageView: ImageView
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var loggedInUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_profissional)
        esconderBarrasDoSistema(this)

        // Inicializar as Views
        nomeProfissionalTextView = findViewById(R.id.nome_profissional)
        fotoProfissionalImageView = findViewById(R.id.ellipse_2)

        // Obter o ID do usuário logado do SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getString("user_id", null)

        if (loggedInUserId != null) {
            coroutineScope.launch {
                buscarPerfilDoUsuario(loggedInUserId!!) // Chama a nova função
            }
        } else {
            // Lidar com o caso em que não há ID de usuário (algo deu errado no login)
            Log.e("LoginProfissional", "ID do usuário não encontrado nas SharedPreferences.")
            nomeProfissionalTextView.text = "Erro ao obter ID"
            fotoProfissionalImageView.setImageResource(R.drawable.ellipse_2)
            // Talvez redirecionar para a tela de login novamente
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

    @Serializable
    data class UserProfile(
        val id: String,
        val nome: String? = null,
        @SerialName("photo_url") val photo_url: String? = null
        // Adicione outros campos do seu perfil conforme necessário
    )

    private suspend fun buscarPerfilDoUsuario(userId: String) {
        try {
            Log.d("LoginProfissional", "URL da busca de perfil: ${supabaseClient.supabaseUrl}/rest/v1/profiles")
            Log.d("LoginProfissional", "Parâmetro 'select': nome, photo_url")
            Log.d("LoginProfissional", "Parâmetro 'id': eq.$userId")
            val response: HttpResponse = supabaseClient.client.get("${supabaseClient.supabaseUrl}/rest/v1/profiles") {
                parameter("select", "id, nome, photo_url")
                parameter("id", "eq.$userId")
                header("Accept", "application/json")
            }
            Log.d("LoginProfissional", "Status da resposta da busca de perfil: ${response.status}")

            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                throw Exception("Erro ao buscar perfil do usuário: $errorText")
            }

            val profiles: List<UserProfile> = response.body<List<UserProfile>>()
            Log.d("LoginProfissional", "Corpo da resposta da busca de perfil (sucesso): ${response.bodyAsText()}")
            if (profiles.isNotEmpty()) {
                val userProfile = profiles[0]
                nomeProfissionalTextView.text = userProfile.nome ?: "Nome não encontrado"
                userProfile.photo_url?.let { imageUrl ->
                    fotoProfissionalImageView.load(imageUrl) {
                        placeholder(R.drawable.ellipse_2)
                        error(R.drawable.ellipse_2)
                        crossfade(true)
                    }
                } ?: run {
                    fotoProfissionalImageView.setImageResource(R.drawable.ellipse_2)
                }
            } else {
                Log.d("LoginProfissional", "Nenhum perfil encontrado para o ID: $userId")

                nomeProfissionalTextView.text = "Usuário não encontrado"
                fotoProfissionalImageView.setImageResource(R.drawable.ellipse_2)
            }

        } catch (e: Exception) {
            Log.e("LoginProfissional", "Erro ao buscar perfil do usuário: ${e.message}")
            nomeProfissionalTextView.text = "Erro ao carregar nome"
            fotoProfissionalImageView.setImageResource(R.drawable.ellipse_2)
        }
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            // Já está na tela inicial
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