package com.example.salao

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToCadastroCliente
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToLogin
import com.example.salao.network.SupabaseClient
import com.example.salao.utils.esconderBarrasDoSistema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// NOVOS IMPORTS PARA AS CLASSES DE MODELO UNIFICADAS
import com.example.salao.model.Cliente // Importando Cliente do pacote model
import com.example.salao.model.NovoCliente // Importando NovoCliente do pacote model

class CadastroCli : AppCompatActivity() {

    private lateinit var nomeEditText: AutoCompleteTextView
    private lateinit var telefoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var dataNascEditText: EditText
    private lateinit var btnSalvarCliente: FrameLayout
    private lateinit var btnDeletarCliente: FrameLayout
    private val supabaseClient = SupabaseClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    // Agora referenciando Cliente do pacote model
    private var listaClientes: List<Cliente> = emptyList()
    private var clienteIdParaDeletar: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro_cli)
        esconderBarrasDoSistema(this)

        // Inicialização das Views
        nomeEditText = findViewById(R.id.nome)
        telefoneEditText = findViewById(R.id.telefone)
        emailEditText = findViewById(R.id.email)
        dataNascEditText = findViewById(R.id.data_nasc)
        btnSalvarCliente = findViewById(R.id.rectangle_9)
        btnDeletarCliente = findViewById(R.id.rectangle_7)

        // Inicialmente o botão Deletar está desabilitado
        btnDeletarCliente.isEnabled = false

        // Configurar o botão Salvar
        btnSalvarCliente.setOnClickListener { cadastrarNovoCliente() }

        // Configurar listeners para AutoCompleteTextView (Nome)
        nomeEditText.addTextChangedListener(nomeTextWatcher)
        nomeEditText.onItemClickListener = nomeItemClickListener

        // Configurar listener para EditText (Data de Nascimento)
        dataNascEditText.setOnClickListener { mostrarDatePickerDialog() }

        // Configurar listener para o botão Deletar
        btnDeletarCliente.setOnClickListener {
            clienteIdParaDeletar?.takeIf { it != -1 }?.let { id ->
                confirmarExclusao(id)
            } ?: run {
                Toast.makeText(
                    this@CadastroCli,
                    "Nenhum cliente selecionado para deletar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
            // Ação para o ícone de adicionar (já está na tela de cadastro de cliente)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Tentar obter ID do cliente do Intent (se houver) - útil se a tela for acessada diretamente com um ID
        clienteIdParaDeletar = intent.getIntExtra("CLIENTE_ID", -1)
        Log.d("CadastroCli", "ID do cliente para deletar (onCreate): $clienteIdParaDeletar")
        if (clienteIdParaDeletar != -1) {
            btnDeletarCliente.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            esconderBarrasDoSistema(this)
        }
    }

    // Listeners
    private val nomeTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val textoDigitado = s.toString()
            if (textoDigitado.length >= 2) {
                buscarClientes(textoDigitado)
            } else {
                atualizarSugestoesNome(emptyList())
                if (textoDigitado.isEmpty()) {
                    limparCampos()
                }
            }
        }
    }

    private val nomeItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
        val nomeSelecionado = nomeEditText.adapter.getItem(position) as String
        preencherCamposCliente(nomeSelecionado)
        // Obter o ID do cliente selecionado da sua lista (listaClientes)
        val clienteSelecionado =
            listaClientes.find { it.nome.equals(nomeSelecionado, ignoreCase = true) }
        clienteSelecionado?.let {
            clienteIdParaDeletar = it.id
            Log.d("CadastroCli", "Cliente selecionado. ID para deletar: $clienteIdParaDeletar")
            // Habilitar o botão Deletar agora que um cliente foi selecionado
            btnDeletarCliente.isEnabled = true
        } ?: run {
            clienteIdParaDeletar = null // Definir como null ou -1 para indicar que nenhum cliente válido está selecionado
            btnDeletarCliente.isEnabled = false
            Log.d("CadastroCli", "Nenhum cliente correspondente encontrado.")
        }
    }

    // Funções de busca e atualização de UI
    private fun buscarClientes(prefixo: String) {
        coroutineScope.launch {
            try {
                // Agora SupabaseClient.buscarClientesPorNome retorna List<Cliente> do pacote model
                listaClientes = supabaseClient.buscarClientesPorNome(prefixo)
                val nomesClientes = listaClientes.map { it.nome }
                atualizarSugestoesNome(nomesClientes)
            } catch (e: Exception) {
                Log.e("CadastroCli", "Erro ao buscar clientes: ${e.message}", e)
                Toast.makeText(this@CadastroCli, "Erro ao buscar clientes", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun atualizarSugestoesNome(nomes: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
        nomeEditText.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }

    private fun preencherCamposCliente(nomeSelecionado: String) {
        val cliente = listaClientes.find { it.nome.equals(nomeSelecionado, ignoreCase = true) }
        cliente?.let {
            telefoneEditText.setText(it.telefone ?: "") // Telefone já é String?
            emailEditText.setText(it.email ?: "")
            dataNascEditText.setText(formatarDataParaExibicao(it.dataNascimento))
        }
    }

    private fun limparCampos() {
        nomeEditText.text.clear()
        telefoneEditText.text.clear()
        emailEditText.text.clear()
        dataNascEditText.text.clear()
        clienteIdParaDeletar = null // Limpar o ID do cliente para deletar
        btnDeletarCliente.isEnabled = false // Desabilitar o botão deletar
    }

    private fun mostrarDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, yearSelected, monthOfYear, dayOfMonthSelected ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(yearSelected, monthOfYear, dayOfMonthSelected)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dataNascEditText.setText(dateFormat.format(selectedDate.time))
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun formatarDataParaExibicao(dataSupabase: String?): String {
        return if (!dataSupabase.isNullOrEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(dataSupabase)
                date?.let { outputFormat.format(it) } ?: dataSupabase // Tratar parse nulo
            } catch (e: Exception) {
                Log.e("CadastroCli", "Erro ao formatar data para exibição: ${e.message}", e)
                dataSupabase
            }
        } else {
            ""
        }
    }

    private fun formatarDataParaSupabase(dataLocal: String): String {
        return if (dataLocal.isNotEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = inputFormat.parse(dataLocal)
                date?.let { outputFormat.format(it) } ?: dataLocal // Tratar parse nulo
            } catch (e: Exception) {
                Log.e("CadastroCli", "Erro ao formatar data para Supabase: ${e.message}", e)
                dataLocal
            }
        } else {
            ""
        }
    }

    // Funções de CRUD (Create, Delete)
    private fun cadastrarNovoCliente() {
        val nome = nomeEditText.text.toString().trim()
        val telefone = telefoneEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val dataNascimento = dataNascEditText.text.toString().trim()
        val dataFormatadaSupabase = formatarDataParaSupabase(dataNascimento)

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome é obrigatório", Toast.LENGTH_SHORT).show()
            return
        }

        // Usando a classe NovoCliente do pacote model
        // Ajustei o construtor para incluir 'nome' que estava faltando
        val novoCliente = NovoCliente(
            nome = nome, // Adicionado o nome
            telefone = telefone.ifEmpty { null },
            email = email.ifEmpty { null },
            dataNascimento = dataFormatadaSupabase.ifEmpty { null }
        )

        coroutineScope.launch {
            try {
                supabaseClient.cadastrarCliente(novoCliente)
                Toast.makeText(
                    this@CadastroCli,
                    "Cliente cadastrado com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()
                limparCampos()
            } catch (e: Exception) {
                Log.e("CadastroCli", "Erro ao cadastrar cliente: ${e.message}", e)
                Toast.makeText(this@CadastroCli, "Erro ao cadastrar cliente", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun confirmarExclusao(clienteId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja deletar este cliente?")
            .setPositiveButton("Sim") { _, _ ->
                Log.d("CadastroCli", "Tentando deletar cliente com ID: $clienteId")
                deletarCliente(clienteId)
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun deletarCliente(clienteId: Int) {
        Log.d("CadastroCli", "Deletar cliente chamado com ID: $clienteId")
        coroutineScope.launch {
            try {
                supabaseClient.deletarCliente(clienteId)
                Log.d("CadastroCli", "Cliente com ID $clienteId deletado com sucesso!")
                Toast.makeText(
                    this@CadastroCli,
                    "Cliente deletado com sucesso!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Log.e("CadastroCli", "Erro ao deletar cliente: ${e.message}", e)
                Toast.makeText(this@CadastroCli, "Erro ao deletar cliente", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}