package com.example.salao

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.salao.network.SupabaseAuthClient
import kotlinx.coroutines.launch
import com.example.salao.utils.esconderBarrasDoSistema
import android.Manifest

class MainActivity : AppCompatActivity() {

    private val authClient = SupabaseAuthClient()
    private val REQUEST_NOTIFICATION_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        esconderBarrasDoSistema(this)

        requestNotificationPermission()
        Log.d("MainActivity", "MainActivity onCreate executado.")

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getString("user_id", null)
        val sessionToken = sharedPreferences.getString("session_token", null)

        if (loggedInUserId != null && sessionToken != null) {
            Log.d("MainActivity", "Usuário já logado. Redirecionando para LoginProfissional.")

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
                    Toast.makeText(
                        this@MainActivity,
                        "Email de recuperação enviado com sucesso! Verifique sua caixa de entrada.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao enviar email de recuperação: ${e.message}", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao enviar email: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
                    val authResponse = authClient.login(email, password)
                    Toast.makeText(
                        this@MainActivity,
                        "Login realizado com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()

                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("user_id", authResponse.user.id)
                        putString("session_token", authResponse.access_token)

                        apply()
                    }

                    Log.d(
                        "MainActivity",
                        "Dados de login salvos. User ID: ${authResponse.user.id}, Token salvo."
                    )


                    startActivity(Intent(this@MainActivity, LoginProfissional::class.java))
                    finish()

                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro durante o login: ${e.message}", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao fazer login: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    progressBar.visibility = View.INVISIBLE
                    loginButton.isEnabled = true
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida
                Log.d("MainActivity", "Permissão de notificação concedida")
            } else {
                // Permissão negada. Você pode mostrar uma explicação ao usuário, se desejar.
                Log.d("MainActivity", "Permissão de notificação negada")
            }
        }
    }
}
