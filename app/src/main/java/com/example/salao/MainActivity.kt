package com.example.salao

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.salao.network.SupabaseAuthClient
import kotlinx.coroutines.launch
import com.example.salao.utils.esconderBarrasDoSistema
import android.widget.EditText
import android.widget.Button
import android.widget.ProgressBar


class MainActivity : AppCompatActivity() {

    private val authClient = SupabaseAuthClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        esconderBarrasDoSistema(this)

        // Verifica se o usuário já está logado
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            startActivity(Intent(this, LoginProfissional::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val emailEditText = findViewById<EditText>(R.id.edit_email)
        val passwordEditText = findViewById<EditText>(R.id.edit_senha)
        val loginButton = findViewById<Button>(R.id.bottom)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val forgotPasswordButton = findViewById<TextView>(R.id.esqueceu_su)

        forgotPasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Digite um email válido!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar.visibility = ProgressBar.VISIBLE

            lifecycleScope.launch {
                try {
                    authClient.recoverPassword(email)
                    Toast.makeText(this@MainActivity, "Email enviado com sucesso!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Erro ao enviar email: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE
            loginButton.isEnabled = false

            Log.d("MainActivity", "Tentando fazer login com email: $email")

            lifecycleScope.launch {
                try {
                    val result = authClient.login(email, password)
                    Toast.makeText(this@MainActivity, "Login realizado com sucesso!", Toast.LENGTH_LONG).show()

                    // Salvar o estado de login no SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("is_logged_in", true)
                    editor.putString("user_email", emailEditText.text.toString())
                    editor.putString("user_id", result.user.id) // Salvar o ID do usuário
                    editor.apply()

                    // Navegar para a próxima tela após o login bem-sucedido
                    startActivity(Intent(this@MainActivity, LoginProfissional::class.java))
                    finish()

                } catch (e: Exception) {
                    Log.e("LOGIN", "Erro: ${e.message}")
                    Toast.makeText(this@MainActivity, "Erro ao fazer login: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    loginButton.isEnabled = true
                }
            }
        }
    }
}