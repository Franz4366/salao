package com.example.salao

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
// Removido: import androidx.lifecycle.ViewModelProvider
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgenda
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToAgendamento
import com.example.salao.com.example.salao.utils.NavigationManager.navigateToPerfilUsuario
import com.example.salao.utils.esconderBarrasDoSistema
import com.example.salao.model.Cliente
import com.example.salao.model.NovoCliente
// Removido: import com.example.salao.viewmodel.AuthViewModel
import com.example.salao.network.SupabaseClient // Adicionando import do SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CadastroCli : AppCompatActivity() {

    private lateinit var nomeEditText: AutoCompleteTextView
    private lateinit var telefoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var dataNascEditText: EditText
    private lateinit var btnSalvarCliente: FrameLayout
    private lateinit var btnDeletarCliente: FrameLayout

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var listaClientes: List<Cliente> = emptyList()
    private var clienteIdParaDeletar: String? = null
    // Removido: private lateinit var authViewModel: AuthViewModel

    companion object {
        private const val TAG = "CadastroCli"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro_cli)
        esconderBarrasDoSistema(this)

        // Inicializa os componentes da UI IMEDIATAMENTE após setContentView
        inicializarComponentesUI()

        // Removido: authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // --- ALTERADO: Removido o observer do AuthViewModel.
        // A lógica de carregamento de dados do cliente agora depende diretamente
        // da existência do token no SupabaseClient.
        val currentAccessToken = SupabaseClient.getAccessToken()
        if (currentAccessToken == null) {
            // Usuário NÃO logado, redireciona para a tela de login
            Log.e(TAG, "CadastroCli: Token de acesso NÃO encontrado. Redirecionando para login.")
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG)
                .show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finaliza esta Activity
        } else {
            // Usuário está logado. Pode prosseguir com as operações que dependem do token.
            // Carrega dados de cliente se houver na intent.
            val intentClienteId = intent.getStringExtra("CLIENTE_ID")
            Log.d(TAG, "ID do cliente da Intent (onCreate): $intentClienteId")
            if (!intentClienteId.isNullOrEmpty()) {
                clienteIdParaDeletar = intentClienteId
                btnDeletarCliente.isEnabled = true
                carregarDadosCliente(intentClienteId)
            } else {
                btnDeletarCliente.isEnabled = false
            }
        }
    }

    private fun inicializarComponentesUI() {
        nomeEditText = findViewById(R.id.nome)
        telefoneEditText = findViewById(R.id.telefone)
        emailEditText = findViewById(R.id.email)
        dataNascEditText = findViewById(R.id.data_nasc)
        btnSalvarCliente = findViewById(R.id.rectangle_9)
        btnDeletarCliente = findViewById(R.id.rectangle_7)

        btnDeletarCliente.isEnabled = false // Estado inicial

        btnSalvarCliente.setOnClickListener { cadastrarNovoCliente() }
        nomeEditText.addTextChangedListener(nomeTextWatcher)
        nomeEditText.onItemClickListener = nomeItemClickListener
        dataNascEditText.setOnClickListener { mostrarDatePickerDialog() }
        btnDeletarCliente.setOnClickListener {
            clienteIdParaDeletar?.let { id ->
                confirmarExclusao(id)
            } ?: run {
                Toast.makeText(this, "Nenhum cliente selecionado para exclusão", Toast.LENGTH_SHORT).show()
            }
        }
        setupNavigationIcons()
    }

    private fun setupNavigationIcons() {
        findViewById<ImageView>(R.id.icon_home)?.setOnClickListener {
            val intent = Intent(this, LoginProfissional::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        findViewById<ImageView>(R.id.icon_agendar)?.setOnClickListener {
            navigateToAgendamento(this)
        }
        findViewById<ImageView>(R.id.icon_calendar)?.setOnClickListener {
            navigateToAgenda(this)
        }
        findViewById<ImageView>(R.id.icon_add)?.setOnClickListener {
            // Já estamos na tela de cadastro de cliente, não faz nada
        }

        findViewById<ImageView>(R.id.icon_user)?.setOnClickListener {
            navigateToPerfilUsuario(this)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        val clienteSelecionado =
            listaClientes.find { it.nome.equals(nomeSelecionado, ignoreCase = true) }

        clienteSelecionado?.let {
            preencherCamposCliente(it)
            clienteIdParaDeletar = it.id
            Log.d(TAG, "Cliente selecionado. ID para deletar: $clienteIdParaDeletar")
            btnDeletarCliente.isEnabled = true
        } ?: run {
            limparCampos()
            Log.d(TAG, "Nenhum cliente correspondente encontrado para preenchimento.")
        }
    }

    private fun buscarClientes(prefixo: String) {
        // --- ALTERADO: Obtém o token diretamente do SupabaseClient ---
        val currentAccessToken = SupabaseClient.getAccessToken()
        if (currentAccessToken == null) {
            handleAuthenticationError()
            return
        }

        coroutineScope.launch {
            try {
                // Chama o método do SupabaseClient passando o token
                listaClientes = SupabaseClient.buscarClientesPorNome(prefixo, currentAccessToken)
                val nomesClientes = listaClientes.map { it.nome }
                withContext(Dispatchers.Main) {
                    atualizarSugestoesNome(nomesClientes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao buscar clientes: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CadastroCli, "Erro ao buscar clientes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun atualizarSugestoesNome(nomes: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomes)
        nomeEditText.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }

    private fun preencherCamposCliente(cliente: Cliente) {
        nomeEditText.setText(cliente.nome)
        telefoneEditText.setText(cliente.telefone ?: "")
        emailEditText.setText(cliente.email ?: "")
        dataNascEditText.setText(formatarDataParaExibicao(cliente.dataNascimento))
    }

    private fun limparCampos() {
        nomeEditText.text.clear()
        telefoneEditText.text.clear()
        emailEditText.text.clear()
        dataNascEditText.text.clear()
        clienteIdParaDeletar = null
        btnDeletarCliente.isEnabled = false
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
                date?.let { outputFormat.format(it) } ?: dataSupabase
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao formatar data para exibição: ${e.message}", e)
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
                date?.let { outputFormat.format(it) } ?: dataLocal
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao formatar data para Supabase: ${e.message}", e)
                dataLocal
            }
        } else {
            ""
        }
    }

    private fun cadastrarNovoCliente() {
        // --- ALTERADO: Obtém o token diretamente do SupabaseClient ---
        val currentAccessToken = SupabaseClient.getAccessToken()
        if (currentAccessToken == null) {
            handleAuthenticationError()
            return
        }

        val nome = nomeEditText.text.toString().trim()
        val telefone = telefoneEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val dataNascimento = dataNascEditText.text.toString().trim()
        val dataFormatadaSupabase = formatarDataParaSupabase(dataNascimento)

        if (nome.isEmpty()) {
            Toast.makeText(this, "O nome é obrigatório", Toast.LENGTH_SHORT).show()
            return
        }

        val novoCliente = NovoCliente(
            nome = nome,
            telefone = telefone.ifEmpty { null },
            email = email.ifEmpty { null },
            dataNascimento = dataFormatadaSupabase.ifEmpty { null }
        )

        coroutineScope.launch {
            try {
                // Chama o método do SupabaseClient passando o token
                SupabaseClient.cadastrarCliente(novoCliente, currentAccessToken)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CadastroCli,
                        "Cliente cadastrado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    limparCampos()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao cadastrar cliente: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CadastroCli, "Erro ao cadastrar cliente: ${e.message}", Toast.LENGTH_LONG).show()
                }
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun confirmarExclusao(clienteId: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja deletar este cliente?")
            .setPositiveButton("Sim") { _, _ ->
                Log.d(TAG, "Tentando deletar cliente com ID: $clienteId")
                deletarCliente(clienteId)
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun deletarCliente(clienteId: String) {
        // --- ALTERADO: Obtém o token diretamente do SupabaseClient ---
        val currentAccessToken = SupabaseClient.getAccessToken()
        if (currentAccessToken == null) {
            handleAuthenticationError()
            return
        }

        Log.d(TAG, "Deletar cliente chamado com ID: $clienteId")
        coroutineScope.launch {
            try {
                // Chama o método do SupabaseClient passando o token
                SupabaseClient.deletarCliente(clienteId, currentAccessToken)
                Log.d(TAG, "Cliente com ID $clienteId deletado com sucesso!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CadastroCli,
                        "Cliente deletado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    limparCampos()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao deletar cliente: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CadastroCli, "Erro ao deletar cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    handleAuthenticationError()
                }
            }
        }
    }

    private fun handleAuthenticationError() {
        Log.e(TAG, "Erro de autenticação detectado. Limpando tokens e redirecionando para login.")
        // --- ALTERADO: Chama SupabaseClient.clearTokens() e redireciona ---
        SupabaseClient.clearTokens()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza esta Activity
        Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
    }

    private fun carregarDadosCliente(clienteId: String) {
        // --- ALTERADO: Obtém o token diretamente do SupabaseClient ---
        val currentAccessToken = SupabaseClient.getAccessToken()
        if (currentAccessToken == null) {
            handleAuthenticationError()
            return
        }
        coroutineScope.launch {
            try {
                // Chama o método do SupabaseClient passando o token
                val cliente = SupabaseClient.getClientePorId(clienteId, currentAccessToken)
                withContext(Dispatchers.Main) {
                    cliente?.let {
                        preencherCamposCliente(it)
                        clienteIdParaDeletar = it.id
                    } ?: run {
                        Toast.makeText(
                            this@CadastroCli,
                            "Cliente não encontrado.",
                            Toast.LENGTH_SHORT
                        ).show()
                        limparCampos()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar dados do cliente: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CadastroCli,
                        "Erro ao carregar dados do cliente: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (e.message?.contains("401 Unauthorized") == true || e.message?.contains("JWSError") == true) {
                    handleAuthenticationError()
                }
            }
        }
    }
}