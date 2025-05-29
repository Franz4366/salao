package com.example.salao.utils

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

// Mantenha como uma função de nível superior, ou se preferir, envolva em um 'object'
// como fiz nas sugestões anteriores se você quiser chamá-la de CalendarioUtils.gerarDiasDoMesCompleto
fun gerarDiasDoMes(calendar: Calendar): List<Date> {
    val dias = mutableListOf<Date>()
    val tempCalendar = calendar.clone() as Calendar // Clonar para não modificar o objeto original

    // --- Parte 1: Adicionar dias do mês anterior para preencher a primeira semana ---

    // Define tempCalendar para o primeiro dia do mês atual
    tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
    // Zera as horas para garantir que as comparações de Date funcionem bem
    tempCalendar.set(Calendar.HOUR_OF_DAY, 0)
    tempCalendar.set(Calendar.MINUTE, 0)
    tempCalendar.set(Calendar.SECOND, 0)
    tempCalendar.set(Calendar.MILLISECOND, 0)

    // Ajusta para que Segunda-feira seja o primeiro dia da semana (índice 0-6).
    // Calendar.SUNDAY = 1, Calendar.MONDAY = 2, ..., Calendar.SATURDAY = 7
    // Se o Calendar.DAY_OF_WEEK do 1º dia é 1 (domingo), daysToPrepend é 6 (precisa de seg-sáb do mês anterior)
    // Se o Calendar.DAY_OF_WEEK do 1º dia é 2 (segunda), daysToPrepend é 0
    val firstDayOfWeekInMonth = tempCalendar.get(Calendar.DAY_OF_WEEK)
    val daysToPrepend = if (firstDayOfWeekInMonth == Calendar.SUNDAY) {
        6 // Se o mês começa no domingo e queremos segunda como início, precisamos de 6 dias (Seg a Sáb)
    } else {
        firstDayOfWeekInMonth - Calendar.MONDAY // Ex: terça (3) - segunda (2) = 1 dia do mês anterior
    }

    val previousMonthCalendar = tempCalendar.clone() as Calendar
    previousMonthCalendar.add(Calendar.MONTH, -1) // Vai para o mês anterior
    // Vai para o último dia do mês anterior e zera as horas
    previousMonthCalendar.set(Calendar.DAY_OF_MONTH, previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    previousMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
    previousMonthCalendar.set(Calendar.MINUTE, 0)
    previousMonthCalendar.set(Calendar.SECOND, 0)
    previousMonthCalendar.set(Calendar.MILLISECOND, 0)

    for (i in 0 until daysToPrepend) {
        // Adiciona os dias no início da lista, voltando no tempo
        dias.add(0, previousMonthCalendar.time)
        previousMonthCalendar.add(Calendar.DAY_OF_MONTH, -1) // Volta um dia
    }

    // --- Parte 2: Adicionar todos os dias do mês atual ---
    val maxDaysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (i in 1..maxDaysInMonth) {
        tempCalendar.set(Calendar.DAY_OF_MONTH, i)
        dias.add(tempCalendar.time)
    }

    // --- Parte 3: Adicionar dias do próximo mês para preencher a última semana ---
    // Calculamos quantos dias já temos na lista (dias do mês anterior + dias do mês atual)
    val totalDaysSoFar = dias.size
    // Queremos que o calendário tenha um número fixo de células que seja um múltiplo de 7 (6 semanas * 7 dias = 42).
    // Ou simplesmente preencher até o final da última semana iniciada.
    val daysToAppend = if (totalDaysSoFar % 7 != 0) {
        7 - (totalDaysSoFar % 7) // Dias necessários para completar a última semana
    } else {
        0 // Já está em um múltiplo de 7, não precisa adicionar dias extras
    }

    val nextMonthCalendar = tempCalendar.clone() as Calendar
    nextMonthCalendar.add(Calendar.MONTH, 1) // Vai para o próximo mês
    nextMonthCalendar.set(Calendar.DAY_OF_MONTH, 1) // Primeiro dia do próximo mês
    nextMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
    nextMonthCalendar.set(Calendar.MINUTE, 0)
    nextMonthCalendar.set(Calendar.SECOND, 0)
    nextMonthCalendar.set(Calendar.MILLISECOND, 0)

    for (i in 0 until daysToAppend) {
        dias.add(nextMonthCalendar.time)
        nextMonthCalendar.add(Calendar.DAY_OF_MONTH, 1) // Avança um dia
    }

    return dias
}