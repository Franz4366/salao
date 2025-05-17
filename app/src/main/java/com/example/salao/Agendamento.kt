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
import java.util.Locale
import android.view.ViewGroup
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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.LayoutInflater
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
                // Não precisamos fazer nada aqui
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
            //navigateToLogin(this)
        }
        findViewById<ImageView>(R.id.icon_agendar)?.setOnClickListener {
            //navigateToAgendamento(this)
        }
        findViewById<ImageView>(R.id.icon_calendar)?.setOnClickListener {
            //navigateToAgenda(this)
        }
        findViewById<ImageView>(R.id.icon_add)?.setOnClickListener {
            //navigateToCadastroCliente(this)
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
        val dias = gerarDiasDoMes(calendar)

        tvMes.text = SimpleDateFormat("MMMM", Locale("pt", "BR"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        calendarRecyclerView.adapter = DiaSemanaAdapter(dias)
    }
}
