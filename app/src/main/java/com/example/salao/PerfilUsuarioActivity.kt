package com.example.salao

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import com.example.salao.com.example.salao.utils.NavigationManager
import com.example.salao.model.Profile
import com.example.salao.model.ProfileUpdate
import com.example.salao.network.SupabaseClient // Certifique-se de que esta importação está correta
import com.example.salao.utils.esconderBarrasDoSistema
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class PerfilUsuarioActivity : AppCompatActivity() {

    private lateinit var etNome: TextInputEditText
    private lateinit var etTelefone: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etCargo: TextInputEditText
    private lateinit var fotoUser: ImageView
    private lateinit var btnSalvar: MaterialButton
    private lateinit var btnDeslogar: MaterialButton

    // --- REMOVIDO: Não instanciamos mais o SupabaseClient aqui ---
    // private val supabaseClient = SupabaseClient()
    // --- REMOVIDO: ID do usuário e token de sessão serão obtidos diretamente do SupabaseClient ---
    // private var loggedInUserId: String? = null
    // private var sessionToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_usuario)
        esconderBarrasDoSistema(this)

        etNome = findViewById(R.id.et_nome)
        etTelefone = findViewById(R.id.et_telefone)
        etEmail = findViewById(R.id.et_email)
        etCargo = findViewById(R.id.et_cargo)
        fotoUser = findViewById(R.id.foto_user)
        btnSalvar = findViewById(R.id.btn_salvar)
        btnDeslogar = findViewById(R.id.btn_deslogar)

        // --- ATUALIZADO: Obtenha o ID do usuário e o token diretamente do SupabaseClient ---
        val currentUserId = SupabaseClient.getLoggedInUserId()
        val currentSessionToken = SupabaseClient.getAccessToken()

        if (currentUserId != null && currentSessionToken != null) {
            carregarDadosUsuario(currentUserId, currentSessionToken)
        } else {
            mostrarToast("Usuário não logado. Redirecionando...")
            Log.e("PerfilUsuarioActivity", "ID do usuário ou token de sessão não encontrados via SupabaseClient.")
            handleAuthenticationError() // Redireciona e limpa (se já não estiver limpo)
            return
        }

        btnSalvar.setOnClickListener {
            salvarAlteracoes()
        }

        btnDeslogar.setOnClickListener {
            deslogarUsuario()
        }

        setupNavigationIcons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun carregarDadosUsuario(userId: String, token: String) {
        lifecycleScope.launch {
            try {
                // --- ATUALIZADO: Chame SupabaseClient.getProfileById diretamente ---
                val profile: Profile? = SupabaseClient.getProfileById(userId, token)

                if (profile != null) {
                    etNome.setText(profile.nome)
                    etTelefone.setText(profile.telefone)
                    etEmail.setText(profile.email)
                    etCargo.setText(profile.cargo)

                    Log.d("PerfilUsuarioActivity", "URL da foto do perfil: ${profile.fotoUrl}")
                    val fotoUrlLimpa = profile.fotoUrl?.trim()
                    Log.d("PerfilUsuarioActivity", "URL da foto do perfil limpa: $fotoUrlLimpa")

                    if (!fotoUrlLimpa.isNullOrBlank()) {
                        Picasso.get()
                            .load(fotoUrlLimpa)
                            .placeholder(R.drawable.ellipse_2)
                            .error(R.drawable.ellipse_2)
                            .into(fotoUser)
                    } else {
                        Log.d("PerfilUsuarioActivity", "URL da foto do perfil está vazia ou nula. Usando imagem padrão.")
                        fotoUser.setImageResource(R.drawable.ellipse_2)
                    }
                } else {
                    mostrarToast("Dados do perfil não encontrados.")
                    Log.e("PerfilUsuarioActivity", "Perfil não encontrado para o ID: $userId")
                    handleAuthenticationError()
                }
            } catch (e: Exception) {
                mostrarToast("Erro ao carregar perfil: ${e.message}")
                Log.e("PerfilUsuarioActivity", "Erro ao carregar perfil: ${e.message}", e)
                if (e.message?.contains("401 Unauthorized", ignoreCase = true) == true ||
                    e.message?.contains("JWSError", ignoreCase = true) == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun salvarAlteracoes() {
        val novoNome = etNome.text.toString().trim()
        val novoTelefone = etTelefone.text.toString().trim()
        val novoEmail = etEmail.text.toString().trim()
        val novoCargo = etCargo.text.toString().trim()

        if (novoNome.isBlank() || novoTelefone.isBlank() || novoEmail.isBlank() || novoCargo.isBlank()) {
            mostrarToast("Por favor, preencha todos os campos.")
            return
        }

        // --- ATUALIZADO: Obtenha o ID do usuário e o token diretamente do SupabaseClient ---
        val currentUserId = SupabaseClient.getLoggedInUserId()
        val currentSessionToken = SupabaseClient.getAccessToken()

        if (currentUserId == null || currentSessionToken == null) {
            mostrarToast("Erro: ID do usuário ou token de sessão não encontrados para salvar.")
            handleAuthenticationError()
            return
        }

        lifecycleScope.launch {
            try {
                val profileUpdate = ProfileUpdate(
                    nome = novoNome,
                    telefone = novoTelefone,
                    email = novoEmail,
                    cargo = novoCargo
                )

                // --- ATUALIZADO: Chame SupabaseClient.updateProfile diretamente ---
                val sucesso = SupabaseClient.updateProfile(currentUserId, profileUpdate, currentSessionToken)

                if (sucesso) {
                    mostrarToast("Perfil atualizado com sucesso!")
                    Log.d("PerfilUsuarioActivity", "Perfil atualizado para ID: $currentUserId")

                    // Redirecionar para o ProfessionalDashboardActivity ou recarregar os dados se necessário
                    NavigationManager.navigateToLogin(this@PerfilUsuarioActivity) // Navega para o Dashboard principal
                    finish() // Finaliza esta atividade
                } else {
                    mostrarToast("Falha ao atualizar perfil.")
                    Log.e("PerfilUsuarioActivity", "Falha ao atualizar perfil para ID: $currentUserId")
                }
            } catch (e: Exception) {
                mostrarToast("Erro ao salvar perfil: ${e.message}")
                Log.e("PerfilUsuarioActivity", "Erro ao salvar perfil: ${e.message}", e)
                if (e.message?.contains("401 Unauthorized", ignoreCase = true) == true ||
                    e.message?.contains("JWSError", ignoreCase = true) == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun deslogarUsuario() {
        handleAuthenticationError() // Esta função já faz o deslogamento completo
    }

    private fun handleAuthenticationError() {
        Log.e("PerfilUsuarioActivity", "Erro de autenticação detectado. Deslogando usuário.")
        // --- ATUALIZADO: Chame SupabaseClient.clearTokens() para centralizar a limpeza ---
        SupabaseClient.clearTokens()
        mostrarToast("Sessão expirada. Faça login novamente.")
        NavigationManager.navigateToLogin(this) // Navega para a tela de login
        finishAffinity() // Fecha todas as atividades na pilha para evitar retorno
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            // --- ATUALIZADO: Navega para ProfessionalDashboardActivity, que é a "Home" após o login ---
            NavigationManager.navigateToLogin(this)
        }
        findViewById<ImageView>(R.id.icon_agendar)?.setOnClickListener {
            NavigationManager.navigateToAgendamento(this)
        }
        findViewById<ImageView>(R.id.icon_calendar)?.setOnClickListener {
            NavigationManager.navigateToAgenda(this)
        }
        findViewById<ImageView>(R.id.icon_add)?.setOnClickListener {
            NavigationManager.navigateToCadastroCliente(this)
        }
        findViewById<ImageView>(R.id.icon_user)?.setOnClickListener {
            // Já está na tela de perfil, não faz nada
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }

    private fun Context.mostrarToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }
}