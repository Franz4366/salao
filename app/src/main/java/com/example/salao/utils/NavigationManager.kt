package com.example.salao.com.example.salao.utils

import android.content.Context
import android.content.Intent
import com.example.salao.Agenda
import com.example.salao.Agendamento
import com.example.salao.CadastroCli
import com.example.salao.LoginProfissional

object NavigationManager {

    fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginProfissional::class.java)
        context.startActivity(intent)
    }

    fun navigateToAgendamento(context: Context) {
        val intent = Intent(context, Agendamento::class.java)
        context.startActivity(intent)
    }

    fun navigateToAgenda(context: Context) {
        val intent = Intent(context, Agenda::class.java)
        context.startActivity(intent)
    }

    fun navigateToCadastroCliente(context: Context) {
        val intent = Intent(context, CadastroCli::class.java)
        context.startActivity(intent)
    }

    fun navigateToCadastroCliente(context: Context, clienteId: Int) {
        val intent = Intent(context, CadastroCli::class.java)
        intent.putExtra("CLIENTE_ID", clienteId)
        context.startActivity(intent)
    }
}