package com.kichikan.negah.domain.member

import java.time.LocalDate

/** Parses and formats Solar Hijri dates written as yyyy/MM/dd (Latin, Persian or Arabic-Indic digits). */
object JalaliDateText {
    fun parse(text: String): LocalDate? {
        val normalized = text.trim().map { c ->
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            }
        }.joinToString("")
        val parts = normalized.split("/")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (year !in 1300..1600 || month !in 1..12) return null
        if (day !in 1..JalaliCalendar.daysInMonth(year, month)) return null
        return JalaliCalendar.toGregorian(year, month, day)
    }

    fun format(date: LocalDate): String {
        val (year, month, day) = JalaliCalendar.toJalali(date)
        return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
    }
}
