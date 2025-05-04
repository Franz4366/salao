package com.example.salao.utils

import com.example.salao.DiaSemana
import java.text.SimpleDateFormat
import java.util.*

fun gerarDiasDoMes(calendar: Calendar): List<DiaSemana> {
    val dias = mutableListOf<DiaSemana>()

    // Clonar calendário e configurar no início do mês
    val tempCal = calendar.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)

    val ultimoDia = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val hoje = Calendar.getInstance()

    for (dia in 1..ultimoDia) {
        tempCal.set(Calendar.DAY_OF_MONTH, dia)

        val nomeDia = SimpleDateFormat("EEE", Locale("pt", "BR"))
            .format(tempCal.time)
            .replace(".", "")

        val numeroDia = tempCal.get(Calendar.DAY_OF_MONTH)

        val ehHoje = hoje.get(Calendar.YEAR) == tempCal.get(Calendar.YEAR) &&
                hoje.get(Calendar.DAY_OF_YEAR) == tempCal.get(Calendar.DAY_OF_YEAR)

        dias.add(DiaSemana(nomeDia.uppercase(), numeroDia, ehHoje))
    }

    // Reorganizar: colocar o dia atual como o primeiro da lista
    val indexHoje = dias.indexOfFirst { it.ehHoje }
    return if (indexHoje != -1) {
        val reordenado = dias.subList(indexHoje, dias.size) + dias.subList(0, indexHoje)
        reordenado
    } else {
        dias // caso hoje não esteja no mês atual
    }
}