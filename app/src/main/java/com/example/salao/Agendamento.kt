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
import java.util.*
import java.text.SimpleDateFormat
import com.example.salao.utils.esconderBarrasDoSistema
import kotlin.jvm.java
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.example.salao.network.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.collections.map

class Agendamento : AppCompatActivity() {

    // Variáveis globais para os componentes da interface
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()

    // Variáveis para a pesquisa de clientes
    private lateinit var pesquisa: AutoCompleteTextView
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private fun buscarClientes(prefixo: String) {
        coroutineScope.launch {
            try {
                val clientes = supabaseClient.buscarClientesPorNome(prefixo)
                val nomesClientes = clientes.map { it.nome } // Extrai apenas os nomes da lista de clientes
                atualizarSugestoes(nomesClientes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun atualizarSugestoes(nomes: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
        pesquisa.setAdapter(adapter)
        adapter.notifyDataSetChanged() // Notifica o adapter que os dados mudaram
    }
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Garante que o conteúdo ocupe toda a tela
        setContentView(R.layout.activity_agendamento)

        pesquisa = findViewById(R.id.pesquisa)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)

        pesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não precisamos fazer nada aqui
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não precisamos fazer nada aqui
            }

            override fun afterTextChanged(s: Editable?) {
                val textoDigitado = s.toString()
                if (textoDigitado.length >= 2) { // Buscar apenas se o usuário digitou pelo menos 3 caracteres
                    buscarClientes(textoDigitado)
                } else {
                    // Limpar as sugestões se o texto for muito curto
                    val adapter = ArrayAdapter<String>(
                        this@Agendamento,
                        android.R.layout.simple_dropdown_item_1line,
                        emptyList()
                    )
                    pesquisa.setAdapter(adapter)
                }
            }
        })
        setupNavigationIcons()
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            navigateToLogin(this)
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