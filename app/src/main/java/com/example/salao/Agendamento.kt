package com.example.salao

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import android.app.TimePickerDialog
import java.util.*
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.view.View
import android.widget.FrameLayout
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.example.salao.utils.gerarDiasDoMes

class Agendamento : AppCompatActivity() {

    // Variáveis globais para os componentes da interface
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var pesquisa: AutoCompleteTextView
    private lateinit var resultPesquisa: View
    private lateinit var cxHora: TextView
    private lateinit var profissional: TextView
    private lateinit var ellipse14: View
    private lateinit var ellipse15: View
    private lateinit var ellipse16: View
    private lateinit var ellipse17: View
    private lateinit var btAgendar: FrameLayout
    private lateinit var iconHome: ImageView
    private lateinit var iconCalendar: ImageView
    private lateinit var iconAgendar: ImageView
    private lateinit var iconAdd: ImageView
    private lateinit var iconUser: ImageView

    // Variáveis para a pesquisa de clientes
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null


    private fun buscarClientes(prefixo: String) {
        coroutineScope.launch {
            try {
                val clientes = supabaseClient.buscarClientesPorNome(prefixo)
                val nomesClientes =
                    clientes.map { it.nome } // Extrai apenas os nomes da lista de clientes
                atualizarSugestoes(nomesClientes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun atualizarSugestoes(nomes: List<String>) {
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
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

        // Inicializar as Views
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMes = findViewById(R.id.tv_mes)
        pesquisa = findViewById(R.id.pesquisa)
        resultPesquisa = findViewById(R.id.result_pesquisa)
        cxHora = findViewById(R.id.cx_hora)
        profissional = findViewById(R.id.profissiona)
        ellipse14 = findViewById(R.id.ellipse_14)
        ellipse15 = findViewById(R.id.ellipse_15)
        ellipse16 = findViewById(R.id.ellipse_16)
        ellipse17 = findViewById(R.id.ellipse_17)
        btAgendar = findViewById(R.id.bt_agendar)
        iconHome = findViewById(R.id.icon_home)
        iconCalendar = findViewById(R.id.icon_calendar)
        iconAgendar = findViewById(R.id.icon_agendar)
        iconAdd = findViewById(R.id.icon_add)
        iconUser = findViewById(R.id.icon_user)

        // Configurar a caixa de hora para exibir 8:00 inicialmente
        cxHora.text = "8:00"
        selectedHour = 8
        selectedMinute = 0

        // Configurar o clique do TextView cxHora para exibir o TimePickerDialog
        cxHora.setOnClickListener {
            showTimePickerDialog()
        }


        pesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
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

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = selectedHour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedMinute ?: calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // Atualiza as variáveis com a hora selecionada
                selectedHour = hourOfDay
                selectedMinute = minute
                // Atualiza o texto do TextView cxHora com a hora selecionada
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                cxHora.text = formattedTime
            },
            hour,
            minute,
            true // Use true para 24 horas, false para 12 horas
        )
        timePickerDialog.show()
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
