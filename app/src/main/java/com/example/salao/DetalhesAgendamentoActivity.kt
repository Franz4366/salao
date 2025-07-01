package com.example.salao

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.salao.utils.esconderBarrasDoSistema
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalhesAgendamentoActivity : AppCompatActivity() {

    private lateinit var tvCliente: TextView
    private lateinit var tvDataHora: TextView
    private lateinit var tvProfissional: TextView
    private lateinit var tvObservacao: TextView
    private lateinit var labelObservacao: TextView
    private lateinit var layoutParaPrint: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveAndShareScreenshot()
        } else {
            mostrarToast("Permissão de armazenamento necessária para salvar o print.")
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_agendamento)

        esconderBarrasDoSistema(this)

        tvCliente = findViewById(R.id.tv_detalhe_cliente)
        tvDataHora = findViewById(R.id.tv_detalhe_data_hora)
        tvProfissional = findViewById(R.id.tv_detalhe_profissional)
        tvObservacao = findViewById(R.id.tv_detalhe_observacao)
        labelObservacao = findViewById(R.id.label_observacao)
        layoutParaPrint = findViewById(R.id.layout_para_print) // O LinearLayout principal para capturar

        val clienteNome = intent.getStringExtra("clienteNome")
        val data = intent.getStringExtra("data")
        val hora = intent.getStringExtra("hora")
        val profissionalNome = intent.getStringExtra("profissionalNome")
        val comentario = intent.getStringExtra("comentario")

        tvCliente.text = clienteNome

        try {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            val parsedDate: Date? = inputDateFormat.parse(data)
            if (parsedDate != null) {
                tvDataHora.text = "${outputDateFormat.format(parsedDate)} às $hora"
            } else {
                tvDataHora.text = "$data às $hora (Formato inválido)"
            }
        } catch (e: Exception) {
            Log.e("DetalhesAgendamento", "Erro ao formatar data: ${e.message}")
            tvDataHora.text = "$data às $hora" // Fallback
        }

        tvProfissional.text = profissionalNome

        if (!comentario.isNullOrBlank()) {
            tvObservacao.text = comentario
            tvObservacao.visibility = View.VISIBLE
            labelObservacao.visibility = View.VISIBLE
        } else {
            tvObservacao.visibility = View.GONE
            labelObservacao.visibility = View.GONE
        }

        // Configurar botões
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_compartilhar_print).setOnClickListener {
            checkAndRequestPermission()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_voltar_agenda).setOnClickListener {
            val intent = Intent(this, Agenda::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveAndShareScreenshot()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                saveAndShareScreenshot()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun saveAndShareScreenshot() {
        val bitmap = takeScreenshotOfView(layoutParaPrint)
        if (bitmap != null) {
            val file = saveBitmapToFile(bitmap)
            if (file != null) {
                shareImage(file)
            } else {
                mostrarToast("Falha ao salvar a imagem.")
            }
        } else {
            mostrarToast("Falha ao gerar a imagem do agendamento.")
        }
    }

    // Função para tirar o screenshot de uma View específica
    private fun takeScreenshotOfView(view: View): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            Log.e("DetalhesAgendamento", "Erro ao tirar screenshot: ${e.message}")
            return null
        }
    }

    // Função para salvar o Bitmap em um arquivo
    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        val filename = "agendamento_${System.currentTimeMillis()}.png"
        val imagesFolder = File(cacheDir, "screenshots") // Salva na pasta cache do app
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs() // Cria o diretório se não existir
        }
        val file = File(imagesFolder, filename)
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            fos.flush()
            fos.close()
            mostrarToast("Print salvo em: ${file.absolutePath}")
            return file
        } catch (e: IOException) {
            Log.e("DetalhesAgendamento", "Erro ao salvar bitmap: ${e.message}")
            return null
        }
    }

    // Função para compartilhar a imagem
    private fun shareImage(file: File) {
        val uri: Uri? = try {
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider", // Precisa ser definido no AndroidManifest.xml
                file
            )
        } catch (e: IllegalArgumentException) {
            Log.e("DetalhesAgendamento", "O arquivo selecionado não pode ser compartilhado: ${e.message}")
            null
        }

        if (uri != null) {
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Concede permissão de leitura temporária
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar Agendamento via"))
        } else {
            mostrarToast("Não foi possível compartilhar a imagem.")
        }
    }

    // Diálogo para informar ao usuário que a permissão foi negada
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissão Negada")
            .setMessage("Para salvar o print, precisamos da permissão de armazenamento. Por favor, conceda-a nas configurações do aplicativo.")
            .setPositiveButton("Ir para Configurações") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Função de extensão para exibir um Toast
    fun Context.mostrarToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }
}