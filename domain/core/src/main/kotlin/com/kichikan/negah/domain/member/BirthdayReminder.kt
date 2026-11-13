package com.kichikan.negah.domain.member

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure-JVM Solar Hijri helper anchored on 1405/01/01 = 2026-03-21.
 * Leap years follow the 33-year rule, which matches the official calendar
 * for the years this app is used (1400s); it avoids depending on Android ICU
 * so the logic stays unit-testable.
 */
object JalaliCalendar {
    private const val ANCHOR_YEAR = 1405
    private val ANCHOR_DATE: LocalDate = LocalDate.of(2026, 3, 21)

    fun isLeap(year: Int): Boolean = Math.floorMod(25 * year + 11, 33) < 8

    fun daysInYear(year: Int): Int = if (isLeap(year)) 366 else 365

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isLeap(year)) 30 else 29
        else -> throw IllegalArgumentException("Invalid Jalali month: $month")
    }

    fun firstDayOfYear(year: Int): LocalDate {
        var date = ANCHOR_DATE
        var y = ANCHOR_YEAR
        while (y < year) { date = date.plusDays(daysInYear(y).toLong()); y++ }
        while (y > year) { y--; date = date.minusDays(daysInYear(y).toLong()) }
        return date
    }

    fun toGregorian(year: Int, month: Int, day: Int): LocalDate {
        require(month in 1..12) { "Invalid Jalali month: $month" }
        require(day in 1..daysInMonth(year, month)) { "Invalid Jalali day: $day" }
        var dayOfYear = day - 1
        for (m in 1 until month) dayOfYear += daysInMonth(year, m)
        return firstDayOfYear(year).plusDays(dayOfYear.toLong())
    }

    /** Converts [date] to (year, month, day) in the Solar Hijri calendar. */
    fun toJalali(date: LocalDate): Triple<Int, Int, Int> {
        val year = yearOf(date)
        var remaining = ChronoUnit.DAYS.between(firstDayOfYear(year), date).toInt()
        var month = 1
        while (month < 12 && remaining >= daysInMonth(year, month)) {
            remaining -= daysInMonth(year, month)
            month++
        }
        return Triple(year, month, remaining + 1)
    }

    /** Returns the Jalali year of [date]. */
    fun yearOf(date: LocalDate): Int {
        var year = date.year - 621
        while (firstDayOfYear(year) > date) year--
        while (firstDayOfYear(year + 1) <= date) year++
        return year
    }
}

data class BirthdayReminder(
    val memberId: String,
    val displayName: String,
    val birthdayDate: LocalDate,
    val daysUntil: Int,
)

object BirthdayReminderCalculator {
    const val DEFAULT_LEAD_DAYS = 3

    /**
     * Members whose birthday falls 1..[leadDays] days after [today].
     * The birth year is irrelevant; Esfand 30 falls back to Esfand 29 in
     * non-leap years. Members with an unparsable birth date are skipped.
     */
    fun upcoming(
        members: List<Member>,
        today: LocalDate,
        leadDays: Int = DEFAULT_LEAD_DAYS,
    ): List<BirthdayReminder> {
        require(leadDays >= 1) { "leadDays must be positive" }
        val currentYear = JalaliCalendar.yearOf(today)
        return members.mapNotNull { member ->
            if (member.isArchived) return@mapNotNull null
            val (month, day) = parseMonthDay(member.birthDateJalali) ?: return@mapNotNull null
            listOf(currentYear, currentYear + 1).firstNotNullOfOrNull { year ->
                val safeDay = minOf(day, JalaliCalendar.daysInMonth(year, month))
                val date = JalaliCalendar.toGregorian(year, month, safeDay)
                val days = ChronoUnit.DAYS.between(today, date).toInt()
                if (days in 1..leadDays) {
                    BirthdayReminder(
                        memberId = member.id,
                        displayName = member.nickname ?: member.fullName,
                        birthdayDate = date,
                        daysUntil = days,
                    )
                } else null
            }
        }.sortedBy { it.daysUntil }
    }

    internal fun parseMonthDay(value: String?): Pair<Int, Int>? {
        val parts = value?.trim()?.map(::normalizeDigit)?.joinToString("")?.split("/") ?: return null
        if (parts.size != 3) return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return month to day
    }

    private fun normalizeDigit(c: Char): Char = when (c) {
        in '۰'..'۹' -> '0' + (c - '۰')
        in '٠'..'٩' -> '0' + (c - '٠')
        else -> c
    }
}
