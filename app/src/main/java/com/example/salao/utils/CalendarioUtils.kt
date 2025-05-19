package com.example.salao.utils

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

fun gerarDiasDoMes(calendar: Calendar): List<Date> {
    val diasDoMes = mutableListOf<Date>()

    // Clonar calendário e configurar no início do mês
    val tempCal = calendar.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)

    val ultimoDia = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (dia in 1..ultimoDia) {
        tempCal.set(Calendar.DAY_OF_MONTH, dia)
        diasDoMes.add(tempCal.time) // Adiciona o objeto Date diretamente
    }

    // Lógica para reordenar para que o dia atual seja o primeiro
    val hoje = Calendar.getInstance()
    val indexHoje = diasDoMes.indexOfFirst {
        val cal = Calendar.getInstance()
        cal.time = it
        cal.get(Calendar.YEAR) == hoje.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == hoje.get(Calendar.DAY_OF_YEAR)
    }

    return if (indexHoje != -1) {
        val reordenado = diasDoMes.subList(indexHoje, diasDoMes.size) + diasDoMes.subList(0, indexHoje)
        reordenado
    } else {
        diasDoMes
    }
}