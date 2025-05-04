package com.example.salao

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.utils.gerarDiasDoMes
import java.util.*
import java.text.SimpleDateFormat
import com.example.salao.utils.esconderBarrasDoSistema
import kotlin.jvm.java


class Agendamento : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agendamento)

        // Escondendo as barras de sistema
        esconderBarrasDoSistema(this)

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMes = findViewById(R.id.tv_mes)

        // Iniciar layout horizontal
        calendarRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Inicializar os dias da semana
        atualizarCalendario()

        // Navegação entre meses
        val btnAnterior: ImageView = findViewById(R.id.seta_anterior)
        val btnProximo: ImageView = findViewById(R.id.seta_proximo)

        btnAnterior.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            atualizarCalendario()
        }

        btnProximo.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            atualizarCalendario()
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

    private fun atualizarCalendario() {
        val dias = gerarDiasDoMes(calendar) // Chama a função do arquivo utilitário

        // Atualizar o nome do mês
        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        calendarRecyclerView.adapter = DiaSemanaAdapter(dias)
    }
}