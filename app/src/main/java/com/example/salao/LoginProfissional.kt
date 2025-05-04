package com.example.salao

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.ImageView
import com.example.salao.utils.esconderBarrasDoSistema
import kotlin.jvm.java


class LoginProfissional : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_profissional)

        esconderBarrasDoSistema(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        // Encontrando os ícones
        val icon_home = findViewById<ImageView>(R.id.icon_home)
        val icon_agendar = findViewById<ImageView>(R.id.icon_agendar)
        val icon_agenda = findViewById<ImageView>(R.id.icon_agenda)
        val icon_add = findViewById<ImageView>(R.id.icon_add)

        // Definindo ações para os ícones
        icon_home.setOnClickListener {
            startActivity(Intent(this, LoginProfissional::class.java))
        }

        icon_agendar.setOnClickListener {
            startActivity(Intent(this, Agendamento::class.java))
        }

        icon_agenda.setOnClickListener {
            startActivity(Intent(this, Agenda::class.java))
        }

        icon_add.setOnClickListener {
            startActivity(Intent(this, CadastroCli::class.java))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }
}