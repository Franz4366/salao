package com.example.salao

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salao.network.SupabaseClient
import com.example.salao.utils.esconderBarrasDoSistema
import android.app.TimePickerDialog
import java.util.*
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.view.View
import android.widget.FrameLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.LinearLayout
import com.example.salao.utils.gerarDiasDoMes
import com.squareup.picasso.Picasso
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.widget.Toast
import androidx.compose.foundation.gestures.snapping.SnapPosition.Center.position
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.squareup.picasso.Transformation

class Agendamento : AppCompatActivity() {

    // Variáveis globais para os componentes da interface
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var pesquisa: AutoCompleteTextView
    private lateinit var resultPesquisa: View
    private lateinit var cxHora: TextView
    private lateinit var btAgendar: FrameLayout
    private lateinit var iconHome: ImageView
    private lateinit var iconCalendar: ImageView
    private lateinit var iconAgendar: ImageView
    private lateinit var iconAdd: ImageView
    private lateinit var iconUser: ImageView
    private lateinit var containerProfissionais: LinearLayout
    private var selectedDate: Date? = null
    private var selectedClientName: String? = null
    private var selectedProfessional: Profile? = null



    // Variáveis para a pesquisa de clientes
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null

    @Serializable
    data class Profile(
        @SerialName("nome") val nome: String,
        @SerialName("cargo") val cargo: String,
        @SerialName("photo_url") val fotoUrl: String?
    )

    private fun buscarClientes(prefixo: String) {
        coroutineScope.launch {
            try {
                val clientes = supabaseClient.buscarClientesPorNome(prefixo)
                val nomesClientes =
                    clientes.map { it.nome } // Extrai apenas os nomes da lista de clientes
                atualizarSugestoes(nomesClientes)
            } catch (e: Exception) {
                Log.e("Agendamento", "Erro ao buscar clientes: ${e.message}")
            }
        }
    }

    private fun atualizarSugestoes(nomes: List<String>) {
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
        pesquisa.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agendamento)

        // Inicializar as Views
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMes = findViewById(R.id.tv_mes)
        pesquisa = findViewById(R.id.pesquisa)
        resultPesquisa = findViewById(R.id.result_pesquisa)
        cxHora = findViewById(R.id.cx_hora)
        btAgendar = findViewById(R.id.bt_agendar)
        iconHome = findViewById(R.id.icon_home)
        iconCalendar = findViewById(R.id.icon_calendar)
        iconAgendar = findViewById(R.id.icon_agendar)
        iconAdd = findViewById(R.id.icon_add)
        iconUser = findViewById(R.id.icon_user)
        containerProfissionais = findViewById(R.id.container_profissionais)

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
                pesquisa.setOnItemClickListener { adapterView, _, position, _ ->
                    this@Agendamento.selectedClientName = adapterView.getItemAtPosition(position) as String
                    Log.d("Agendamento", "Cliente selecionado: $selectedClientName")
                    // Opcional: Você pode limpar o foco do AutoCompleteTextView ou esconder o teclado aqui
                }

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não precisamos fazer nada aqui
            }

            override fun afterTextChanged(s: Editable?) {
                val textoDigitado = s.toString()
                if (textoDigitado.length >= 2) {
                    buscarClientes(textoDigitado)
                } else {
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
        carregarProfissionais()

        btAgendar.setOnClickListener {
            salvarAgendamento()
        }
    }

    private fun salvarAgendamento() {
        if (selectedDate != null && selectedClientName != null && selectedHour != null && selectedProfessional != null) {
            // Todos os dados necessários foram selecionados
            val dataAgendamento = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR")).format(selectedDate!!)
            val horaAgendamento = String.format("%02d:%02d", selectedHour, selectedMinute)
            val nomeProfissional = selectedProfessional!!.nome // Supondo que você queira salvar o nome do profissional

            Log.d("Agendamento", "Dados para salvar: Data=$dataAgendamento, Hora=$horaAgendamento, Cliente=$selectedClientName, Profissional=$nomeProfissional")

            // Aqui você chamará o SupabaseClient para salvar os dados
            // supabaseClient.criarAgendamento(clienteId, dataAgendamento, horaAgendamento, nomeProfissional)

            // Por enquanto, vamos apenas mostrar uma mensagem de sucesso
            exibirMensagem("Agendamento realizado com sucesso!")

            // Opcional: Limpar os campos
            selectedDate = null
            selectedClientName = null
            selectedProfessional = null
            selectedHour = null
            selectedMinute = null
            pesquisa.text.clear()
            cxHora.text = getString(R.string.selecione_a_hora) // Ou o texto inicial da hora
            // Opcional: Atualizar o calendário ou a lista de profissionais se necessário
        } else {
            // Algum dado não foi selecionado
            exibirMensagem("Por favor, selecione a data, o cliente, a hora e o profissional.")
        }
    }

    private fun exibirMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    private fun carregarProfissionais() {
        coroutineScope.launch {
            try {
                val profissionais = supabaseClient.getProfissionais()
                containerProfissionais.removeAllViews()

                if (profissionais.isNotEmpty()) {
                    profissionais.forEach { profissional ->
                        val profissionalView = criarViewProfissional(profissional)
                        containerProfissionais.addView(profissionalView)
                    }
                } else {
                    // Exibe uma mensagem caso não haja profissionais cadastrados
                    val mensagemTextView = TextView(this@Agendamento)
                    mensagemTextView.text = "Nenhum profissional cadastrado."
                    mensagemTextView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    mensagemTextView.gravity = View.TEXT_ALIGNMENT_CENTER
                    containerProfissionais.addView(mensagemTextView)
                }
            } catch (e: Exception) {
                Log.e("Agendamento", "Erro ao carregar profissionais: ${e.message}")
                // Lide com o erro (ex: exibir uma mensagem para o usuário)
                val mensagemTextView = TextView(this@Agendamento)
                mensagemTextView.text = "Erro ao carregar profissionais."
                mensagemTextView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                mensagemTextView.gravity = View.TEXT_ALIGNMENT_CENTER
                containerProfissionais.addView(mensagemTextView)
            }
        }
    }

    private fun criarViewProfissional(profissional: Profile): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_profissional, containerProfissionais, false)
        val fotoProfissional = view.findViewById<ImageView>(R.id.foto_profissional)
        val nomeProfissional = view.findViewById<TextView>(R.id.nome_profissional)

        nomeProfissional.text = profissional.nome
        Log.d("Agendamento", "Nome do profissional: ${profissional.nome}, URL da foto: ${profissional.fotoUrl}")
        if (profissional.fotoUrl != null) {
            Picasso.get()
                .load(profissional.fotoUrl)
                .transform(CircleTransform()) // Aplica a transformação para deixar a imagem circular
                .placeholder(R.drawable.ellipse_14)
                .error(R.drawable.ellipse_14)
                .into(fotoProfissional)
        } else {
            fotoProfissional.setImageResource(R.drawable.ellipse_14)
        }
        view.setOnClickListener {
            selectedProfessional = profissional
            Log.d("Agendamento", "Profissional selecionado: ${profissional.nome}")
            // Aqui você pode adicionar alguma lógica para destacar o profissional selecionado, se desejar
        }

        return view
    }

    // Classe para transformar a imagem em um círculo
    class CircleTransform : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = Math.min(source.width, source.height)

            val x = (source.width - size) / 2
            val y = (source.height - size) / 2

            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888) // Correção aqui
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = android.graphics.BitmapShader(
                squaredBitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = shader
            paint.isAntiAlias = true

            val r = size / 2f
            canvas.drawCircle(r, r, r, paint)

            squaredBitmap.recycle()
            return bitmap
        }

        override fun key(): String {
            return "circle"
        }
    }


    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = selectedHour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedMinute ?: calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                cxHora.text = formattedTime
            },
            hour,
            minute,
            true
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

        esconderBarrasDoSistema(this)

        calendarRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        atualizarCalendario()

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
        val dias = gerarDiasDoMes(calendar) // Esta função retorna List<Date>

        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        val adapter = DiaSemanaAdapter(dias) // Passa a lista de Date diretamente
        adapter.setOnDateClickListener(object : OnDateClickListener {
            override fun onDateClick(date: Date) {
                selectedDate = date
                Log.d("Agendamento", "Data selecionada: $date")
                // Aqui você pode adicionar alguma lógica para destacar a data selecionada visualmente, se desejar
            }
        })
        calendarRecyclerView.adapter = adapter
    }
}