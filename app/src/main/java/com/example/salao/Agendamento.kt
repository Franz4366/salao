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
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.example.salaa.DiaSemanaAdapter
import com.example.salaa.OnDateClickListener
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToPerfilUsuario
import com.squareup.picasso.Transformation
import com.example.salao.model.Profile

class Agendamento : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var tvMes: TextView
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var pesquisa: AutoCompleteTextView
    private lateinit var camp_obs: EditText
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
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null
    private var selectedProfessionalView: View? = null

    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private var sessionToken: String? = null

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agendamento)

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sessionToken = sharedPreferences.getString("session_token", null)

        if (sessionToken == null) {
            Log.e("Agendamento", "Token de sessão não encontrado. Redirecionando para login.")
            Toast.makeText(this, "Sessão expirada ou inválida. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            navigateToLogin(this)
            finish()
            return
        }

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        tvMes = findViewById(R.id.tv_mes)
        pesquisa = findViewById(R.id.pesquisa)
        camp_obs = findViewById(R.id.camp_obs)
        cxHora = findViewById(R.id.cx_hora)
        btAgendar = findViewById(R.id.bt_agendar)
        iconHome = findViewById(R.id.icon_home)
        iconCalendar = findViewById(R.id.icon_calendar)
        iconAgendar = findViewById(R.id.icon_agendar)
        iconAdd = findViewById(R.id.icon_add)
        iconUser = findViewById(R.id.icon_user)
        containerProfissionais = findViewById(R.id.container_profissionais)

        selectedDate = Calendar.getInstance().time

        pesquisa.setOnItemClickListener { adapterView, _, position, _ ->
            this@Agendamento.selectedClientName = adapterView.getItemAtPosition(position) as String
            Log.d("Agendamento", "Cliente selecionado (OnItemClickListener): $selectedClientName")
        }
        cxHora.text = "8:00"
        selectedHour = 8
        selectedMinute = 0

        cxHora.setOnClickListener {
            showTimePickerDialog()
        }
        pesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val textoDigitado = s.toString()
                if (textoDigitado.length >= 2) {
                    buscarClientes(textoDigitado)
                } else {
                    val adapter = ArrayAdapter<String>(this@Agendamento, android.R.layout.simple_dropdown_item_1line, emptyList())
                    pesquisa.setAdapter(adapter)
                }
            }
        })
        setupNavigationIcons()
        carregarProfissionais()

        btAgendar.setOnClickListener {
            salvarAgendamento()
        }

        calendarRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        calendarRecyclerView.post {
            atualizarCalendario()
            scrollToCurrentDate()
        }
    }

    override fun onResume() {
        super.onResume()
        esconderBarrasDoSistema(this)
        calendarRecyclerView.post {
            scrollToCurrentDate()
        }
    }

    private fun exibirMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    private fun salvarAgendamento() {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            exibirMensagem("Erro: Token de autenticação não encontrado. Faça login novamente.")
            navigateToLogin(this)
            finish()
            return
        }

        if (selectedDate != null && selectedClientName != null && selectedHour != null && selectedProfessional != null) {
            val dataAgendamento = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR")).format(selectedDate!!)
            val horaAgendamento = String.format("%02d:%02d", selectedHour, selectedMinute)
            val profissionalIdParaAgendamento = selectedProfessional!!.id
            val observacoes = findViewById<EditText>(R.id.camp_obs).text.toString()

            Log.d("Agendamento", "Tentando buscar cliente com nome: $selectedClientName")

            coroutineScope.launch {
                try {
                    val cliente = supabaseClient.getClientePorNome(selectedClientName!!, currentSessionToken)
                    if (cliente != null) {
                        val clienteId = cliente.id

                        val sucesso = supabaseClient.criarAgendamento(
                            clienteId = clienteId,
                            dataAgendamento = dataAgendamento,
                            horaAgendamento = horaAgendamento,
                            profissionalId = profissionalIdParaAgendamento,
                            comentario = observacoes,
                            userJwtToken = currentSessionToken
                        )

                        Log.d("Agendamento", "Resultado de criarAgendamento: $sucesso")


                        if (sucesso) {
                            Log.d("Agendamento", "Agendamento criado com sucesso! Iniciando DetalhesAgendamentoActivity.")
                            limparCamposAgendamento()

                            val intent = Intent(this@Agendamento, DetalhesAgendamentoActivity::class.java).apply {
                                putExtra("clienteNome", cliente.nome)
                                putExtra("data", dataAgendamento)
                                putExtra("hora", horaAgendamento)
                                putExtra("profissionalNome", selectedProfessional!!.nome)
                                putExtra("comentario", observacoes)
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            exibirMensagem("Erro ao realizar o agendamento.")
                        }
                    } else {
                        exibirMensagem("Cliente não encontrado.")
                    }
                } catch (e: Exception) {
                    Log.e("Agendamento", "Erro ao salvar agendamento: ${e.message}", e)
                    exibirMensagem("Erro ao salvar o agendamento: ${e.message}")
                    if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                        Log.e("Agendamento", "Erro de autenticação (401 Unauthorized / JWSError). Deslogando usuário.")
                        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            remove("user_id")
                            remove("session_token")
                            apply()
                        }
                        Toast.makeText(this@Agendamento, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
                        navigateToLogin(this@Agendamento)
                        finish()
                    }
                }
            }
        } else {
            exibirMensagem("Por favor, selecione a data, o cliente, a hora e o profissional.")
        }
    }

    private fun buscarClientes(prefixo: String) {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            handleAuthenticationError()
            Log.e("Agendamento", "Token de sessão é nulo ao buscar clientes. Redirecionando para login.")
            Toast.makeText(this, "Erro de autenticação. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            navigateToLogin(this)
            finish()
            return
        }

        coroutineScope.launch {
            try {
                val clientes = supabaseClient.buscarClientesPorNome(prefixo, currentSessionToken)
                val nomesClientes = clientes.map { it.nome }
                atualizarSugestoes(nomesClientes)
            } catch (e: Exception) {
                Log.e("Agendamento", "Erro ao buscar clientes: ${e.message}", e)
                Toast.makeText(this@Agendamento, "Erro ao buscar clientes: ${e.message}", Toast.LENGTH_LONG).show()
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun atualizarSugestoes(nomes: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
        pesquisa.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }

    private fun limparCamposAgendamento() {
        selectedDate = Calendar.getInstance().time
        selectedClientName = null
        selectedProfessional = null
        selectedHour = 8
        selectedMinute = 0
        pesquisa.text.clear()
        camp_obs.text.clear()
        cxHora.text = "8:00"
        selectedProfessionalView?.setBackgroundResource(android.R.color.transparent)
        selectedProfessionalView = null


        atualizarCalendario()
        calendarRecyclerView.post {
            scrollToCurrentDate()
        }
    }

    private fun carregarProfissionais() {
        val currentSessionToken = sessionToken
        if (currentSessionToken == null) {
            Log.e("Agendamento", "Token de sessão é nulo ao carregar profissionais. Redirecionando para login.")
            Toast.makeText(this, "Erro de autenticação. Por favor, faça login novamente.", Toast.LENGTH_LONG).show()
            navigateToLogin(this)
            finish()
            return
        }

        coroutineScope.launch {
            try {
                val profissionais = supabaseClient.getProfissionais(currentSessionToken)
                containerProfissionais.removeAllViews()

                if (profissionais.isNotEmpty()) {
                    profissionais.forEach { profissional ->
                        val profissionalView = criarViewProfissional(profissional)
                        containerProfissionais.addView(profissionalView)
                    }
                } else {
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
                Log.e("Agendamento", "Erro ao carregar profissionais: ${e.message}", e)
                Toast.makeText(this@Agendamento, "Erro ao carregar profissionais: ${e.message}", Toast.LENGTH_LONG).show()
                val mensagemTextView = TextView(this@Agendamento)
                mensagemTextView.text = "Erro ao carregar profissionais."
                mensagemTextView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                mensagemTextView.gravity = View.TEXT_ALIGNMENT_CENTER
                containerProfissionais.addView(mensagemTextView)
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun handleAuthenticationError() {
        Log.e("Agendamento", "Erro de autenticação (401 Unauthorized / JWSError). Deslogando usuário.")
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("user_id")
            remove("session_token")
            apply()
        }
        Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
        navigateToLogin(this)
        finish()
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
                .transform(CircleTransform())
                .placeholder(R.drawable.ellipse_14)
                .error(R.drawable.ellipse_14)
                .into(fotoProfissional)
        } else {
            fotoProfissional.setImageResource(R.drawable.ellipse_14)
        }
        view.setOnClickListener {
            selectedProfessionalView?.setBackgroundResource(android.R.color.transparent)
            view.setBackgroundResource(R.drawable.background_profissional_selecionado)
            selectedProfessional = profissional
            selectedProfessionalView = view
            Log.d("Agendamento", "Profissional selecionado: ${profissional.nome}")
        }
        return view
    }

    class CircleTransform : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = Math.min(source.width, source.height)

            val x = (source.width - size) / 2
            val y = (source.height - size) / 2

            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
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
        }

        findViewById<ImageView>(R.id.icon_calendar)?.setOnClickListener {
            navigateToAgenda(this)
        }

        findViewById<ImageView>(R.id.icon_add)?.setOnClickListener {
            navigateToCadastroCliente(this)
        }
        findViewById<ImageView>(R.id.icon_user)?.setOnClickListener {
            navigateToPerfilUsuario(this)
        }

        val btnAnterior: ImageView = findViewById(R.id.seta_anterior)
        val btnProximo: ImageView = findViewById(R.id.seta_proximo)

        btnAnterior.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            val newMonthFirstDay = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, 1)
            }.time
            selectedDate = newMonthFirstDay
            atualizarCalendario()
            calendarRecyclerView.post {
                scrollToCurrentDate()
            }
        }

        btnProximo.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            val newMonthFirstDay = Calendar.getInstance().apply {
                time = calendar.time
                set(Calendar.DAY_OF_MONTH, 1)
            }.time
            selectedDate = newMonthFirstDay
            atualizarCalendario()
            calendarRecyclerView.post {
                scrollToCurrentDate()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }

    private fun atualizarCalendario() {
        val dias = gerarDiasDoMes(calendar)

        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        val adapter = DiaSemanaAdapter(dias, calendar)
        adapter.setOnDateClickListener(object : OnDateClickListener {
            override fun onDateClick(date: Date) {
                val oldSelectedPosition = adapter.selectedPosition
                selectedDate = date

                val clickedIndex = dias.indexOfFirst {
                    val cal1 = Calendar.getInstance().apply { time = it }
                    val cal2 = Calendar.getInstance().apply { time = date }
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
                }

                if (clickedIndex != -1) {
                    if (oldSelectedPosition != null && oldSelectedPosition != clickedIndex) {
                        adapter.selectedPosition = null
                        adapter.notifyItemChanged(oldSelectedPosition)
                    }
                    adapter.selectedPosition = clickedIndex
                    adapter.notifyItemChanged(clickedIndex)
                }
                Log.d("Agendamento", "Data selecionada: $date")
            }
        })
        calendarRecyclerView.adapter = adapter

        val targetDateForInitialSelection = selectedDate

        if (targetDateForInitialSelection != null) {
            val indexToSetInitially = dias.indexOfFirst {
                val tempCal = Calendar.getInstance().apply { time = it }
                val targetCal = Calendar.getInstance().apply { time = targetDateForInitialSelection }
                tempCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                        tempCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
                        tempCal.get(Calendar.DAY_OF_MONTH) == targetCal.get(Calendar.DAY_OF_MONTH)
            }

            if (indexToSetInitially != -1) {
                adapter.selectedPosition = indexToSetInitially
                adapter.notifyDataSetChanged()
            } else {
                adapter.selectedPosition = null
                adapter.notifyDataSetChanged()
            }
        } else {
            adapter.selectedPosition = null
            adapter.notifyDataSetChanged()
        }
    }

    private fun scrollToCurrentDate() {
        val layoutManager = calendarRecyclerView.layoutManager as LinearLayoutManager
        val dias = gerarDiasDoMes(calendar)

        val targetDate = selectedDate ?: Calendar.getInstance().time

        val indexToScroll = dias.indexOfFirst {
            val cal1 = Calendar.getInstance().apply { time = it }
            val cal2 = Calendar.getInstance().apply { time = targetDate }
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                    cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
        }

        if (indexToScroll != -1) {
            calendarRecyclerView.post {
                val itemView = layoutManager.findViewByPosition(indexToScroll)
                if (itemView != null) {
                    val itemWidth = itemView.width
                    val recyclerViewWidth = calendarRecyclerView.width
                    val offset = (recyclerViewWidth / 2) - (itemWidth / 2)

                    layoutManager.scrollToPositionWithOffset(indexToScroll, offset)
                } else {
                    val itemWidthFallback = resources.getDimensionPixelSize(R.dimen.calendar_item_width)
                    val recyclerViewWidth = calendarRecyclerView.width
                    val offsetFallback = (recyclerViewWidth / 2) - (itemWidthFallback / 2)
                    layoutManager.scrollToPositionWithOffset(indexToScroll, offsetFallback)
                }
            }
        }
    }
}