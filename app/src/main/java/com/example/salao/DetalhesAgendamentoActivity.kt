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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_agendamento)

        esconderBarrasDoSistema(this)

        tvCliente = findViewById(R.id.tv_detalhe_cliente)
        tvDataHora = findViewById(R.id.tv_detalhe_data_hora)
        tvProfissional = findViewById(R.id.tv_detalhe_profissional)
        tvObservacao = findViewById(R.id.tv_detalhe_observacao)
        labelObservacao = findViewById(R.id.label_observacao)
        layoutParaPrint = findViewById(R.id.layout_para_print)

        val clienteNome = intent.getStringExtra("clienteNome")
        val data = intent.getStringExtra("data")
        val hora = intent.getStringExtra("hora")
        val profissionalNome = intent.getStringExtra("profissionalNome")
        val comentario = intent.getStringExtra("comentario")

        tvCliente.text = clienteNome

        try {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            val parsedDate: Date? = inputDateFormat.parse(data)
            if (parsedDate != null) {
                tvDataHora.text = "${outputDateFormat.format(parsedDate)} às $hora"
            } else {
                tvDataHora.text = "$data às $hora (Formato inválido)"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao formatar data: ${e.message}")
            tvDataHora.text = "$data às $hora"
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
        saveAndShareScreenshot()
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

    private fun takeScreenshotOfView(view: View): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao tirar screenshot: ${e.message}")
            return null
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File? {
        val filename = "agendamento_${System.currentTimeMillis()}.png"
        val imagesFolder = File(cacheDir, "screenshots")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
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
            Log.e(TAG, "Erro ao salvar bitmap: ${e.message}")
            return null
        }
    }

    private fun shareImage(file: File) {
        val uri: Uri? = try {
            FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "O arquivo selecionado não pode ser compartilhado: ${e.message}")
            null
        }

        if (uri != null) {
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar Agendamento via"))
        } else {
            mostrarToast("Não foi possível compartilhar a imagem.")
        }
    }

    fun Context.mostrarToast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }
    companion object {
        private const val TAG = "DetalhesAgendamentoActivity"
    }
}