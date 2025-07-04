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
import com.example.salao.network.SupabaseClient
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

    private val supabaseClient = SupabaseClient()
    private var loggedInUserId: String? = null
    private var sessionToken: String? = null

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

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getString("user_id", null)
        sessionToken = sharedPreferences.getString("session_token", null)

        if (loggedInUserId != null && sessionToken != null) {
            carregarDadosUsuario(loggedInUserId!!, sessionToken!!)
        } else {
            mostrarToast("Usuário não logado. Redirecionando...")
            Log.e("PerfilUsuarioActivity", "ID do usuário ou token de sessão não encontrados nas SharedPreferences.")
            NavigationManager.navigateToLogin(this)
            finish()
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
                val profile: Profile? = supabaseClient.getProfileById(userId, token)

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

        if (loggedInUserId == null || sessionToken == null) {
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

                val sucesso = supabaseClient.updateProfile(loggedInUserId!!, profileUpdate, sessionToken!!)

                if (sucesso) {
                    mostrarToast("Perfil atualizado com sucesso!")
                    Log.d("PerfilUsuarioActivity", "Perfil atualizado para ID: $loggedInUserId")


                    finish()
                } else {
                    mostrarToast("Falha ao atualizar perfil.")
                    Log.e("PerfilUsuarioActivity", "Falha ao atualizar perfil para ID: $loggedInUserId")
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
        handleAuthenticationError()
    }

    private fun handleAuthenticationError() {
        Log.e("PerfilUsuarioActivity", "Erro de autenticação (401 Unauthorized / JWSError). Deslogando usuário.")
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("user_id")
            remove("session_token")
            apply()
        }
        mostrarToast("Sessão expirada. Faça login novamente.")
        NavigationManager.navigateToLogin(this)
        finishAffinity()
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
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