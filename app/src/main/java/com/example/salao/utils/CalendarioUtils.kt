package com.example.salao.utils

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

fun gerarDiasDoMes(calendar: Calendar): List<Date> {
    val dias = mutableListOf<Date>()
    val tempCalendar = calendar.clone() as Calendar


    tempCalendar.set(Calendar.DAY_OF_MONTH, 1)
    tempCalendar.set(Calendar.HOUR_OF_DAY, 0)
    tempCalendar.set(Calendar.MINUTE, 0)
    tempCalendar.set(Calendar.SECOND, 0)
    tempCalendar.set(Calendar.MILLISECOND, 0)

    val firstDayOfWeekInMonth = tempCalendar.get(Calendar.DAY_OF_WEEK)
    val daysToPrepend = if (firstDayOfWeekInMonth == Calendar.SUNDAY) {
        6
    } else {
        firstDayOfWeekInMonth - Calendar.MONDAY
    }

    val previousMonthCalendar = tempCalendar.clone() as Calendar
    previousMonthCalendar.add(Calendar.MONTH, -1)
    previousMonthCalendar.set(Calendar.DAY_OF_MONTH, previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    previousMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
    previousMonthCalendar.set(Calendar.MINUTE, 0)
    previousMonthCalendar.set(Calendar.SECOND, 0)
    previousMonthCalendar.set(Calendar.MILLISECOND, 0)

    for (i in 0 until daysToPrepend) {
        dias.add(0, previousMonthCalendar.time)
        previousMonthCalendar.add(Calendar.DAY_OF_MONTH, -1)
    }

    val maxDaysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (i in 1..maxDaysInMonth) {
        tempCalendar.set(Calendar.DAY_OF_MONTH, i)
        dias.add(tempCalendar.time)
    }

    val totalDaysSoFar = dias.size
    val daysToAppend = if (totalDaysSoFar % 7 != 0) {
        7 - (totalDaysSoFar % 7)
    } else {
        0
    }

    val nextMonthCalendar = tempCalendar.clone() as Calendar
    nextMonthCalendar.add(Calendar.MONTH, 1)
    nextMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)
    nextMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
    nextMonthCalendar.set(Calendar.MINUTE, 0)
    nextMonthCalendar.set(Calendar.SECOND, 0)
    nextMonthCalendar.set(Calendar.MILLISECOND, 0)

    for (i in 0 until daysToAppend) {
        dias.add(nextMonthCalendar.time)
        nextMonthCalendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    return dias
}