package com.example.salao

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.utils.gerarDiasDoMes
import com.example.salao.utils.esconderBarrasDoSistema
import java.text.SimpleDateFormat
import java.util.*

class Agenda : AppCompatActivity() {

    private lateinit var calendar: Calendar
    private lateinit var tvMes: TextView
    private lateinit var calendarRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Garante que o conteúdo ocupe toda a tela
        setContentView(R.layout.activity_agenda)

        esconderBarrasDoSistema(this) // Oculta as barras de sistema

        // Inicializando as variáveis
        calendar = Calendar.getInstance() // Obtém o calendário atual
        tvMes = findViewById(R.id.tv_mes) // Referência para o TextView do mês
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView) // Referência para o RecyclerView

        // Configuração do RecyclerView
        calendarRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Atualizar calendário na tela
        atualizarCalendario()

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

    private fun atualizarCalendario() {
        val dias = gerarDiasDoMes(calendar) // Chama a função do arquivo utilitário

        // Atualizar o nome do mês
        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        // Atualizando o adapter do RecyclerView
        calendarRecyclerView.adapter = DiaSemanaAdapter(dias)
    }
}