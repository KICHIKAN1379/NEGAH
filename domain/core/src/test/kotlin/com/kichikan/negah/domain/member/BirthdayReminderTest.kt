package com.kichikan.negah.domain.member

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BirthdayReminderTest {
    private fun member(id: String, birth: String?, nickname: String? = null, archived: Boolean = false) = Member(
        id = id, fullName = "عضو $id", nickname = nickname,
        totalXp = 0, spendablePoints = 0, diamonds = 0,
        createdAtEpochMillis = 0, updatedAtEpochMillis = 0, isArchived = archived,
        birthDateJalali = birth,
    )

    @Test
    fun jalaliAnchorsMatchKnownGregorianDates() {
        assertEquals(LocalDate.of(2026, 3, 21), JalaliCalendar.toGregorian(1405, 1, 1))
        assertEquals(LocalDate.of(2026, 11, 12), JalaliCalendar.toGregorian(1405, 8, 21))
        assertEquals(LocalDate.of(2025, 3, 21), JalaliCalendar.toGregorian(1404, 1, 1))
        assertEquals(LocalDate.of(2024, 3, 20), JalaliCalendar.toGregorian(1403, 1, 1))
        assertEquals(1405, JalaliCalendar.yearOf(LocalDate.of(2026, 11, 12)))
        assertEquals(1404, JalaliCalendar.yearOf(LocalDate.of(2026, 3, 20)))
    }

    @Test
    fun leapYearsFollowTheKnownPattern() {
        assertTrue(JalaliCalendar.isLeap(1403))
        assertTrue(JalaliCalendar.isLeap(1408))
        assertEquals(false, JalaliCalendar.isLeap(1404))
        assertEquals(30, JalaliCalendar.daysInMonth(1403, 12))
        assertEquals(29, JalaliCalendar.daysInMonth(1404, 12))
    }

    @Test
    fun remindsThreeDaysBeforeAndNotOnOtherDays() {
        val m = member("a", "1390/08/24")
        assertEquals(3, BirthdayReminderCalculator.upcoming(listOf(m), LocalDate.of(2026, 11, 12)).single().daysUntil)
        assertEquals(1, BirthdayReminderCalculator.upcoming(listOf(m), LocalDate.of(2026, 11, 14)).single().daysUntil)
        assertTrue(BirthdayReminderCalculator.upcoming(listOf(m), LocalDate.of(2026, 11, 11)).isEmpty())
        assertTrue(BirthdayReminderCalculator.upcoming(listOf(m), LocalDate.of(2026, 11, 15)).isEmpty())
    }

    @Test
    fun wrapsAcrossTheJalaliYearEnd() {
        val m = member("a", "1390/01/02")
        val reminder = BirthdayReminderCalculator.upcoming(listOf(m), LocalDate.of(2027, 3, 19)).single()
        assertEquals(LocalDate.of(2027, 3, 21), JalaliCalendar.toGregorian(1406, 1, 1))
        assertEquals(LocalDate.of(2027, 3, 22), reminder.birthdayDate)
        assertEquals(3, reminder.daysUntil)
    }

    @Test
    fun esfandThirtyFallsBackInNonLeapYearsAndPersianDigitsAreAccepted() {
        val m = member("a", "۱۳۹۰/۱۲/۳۰", nickname = "علی")
        val reminder = BirthdayReminderCalculator.upcoming(listOf(m), LocalDate.of(2026, 3, 17)).single()
        assertEquals(LocalDate.of(2026, 3, 20), reminder.birthdayDate)
        assertEquals("علی", reminder.displayName)
    }

    @Test
    fun skipsArchivedMissingAndMalformedBirthDates() {
        val today = LocalDate.of(2026, 11, 12)
        val members = listOf(
            member("a", "1390/08/24", archived = true),
            member("b", null),
            member("c", "garbage"),
            member("d", "1390/13/01"),
        )
        assertTrue(BirthdayReminderCalculator.upcoming(members, today).isEmpty())
        assertNull(BirthdayReminderCalculator.parseMonthDay("1390/08"))
    }

    @Test
    fun jalaliTextRoundTripsAndRejectsInvalidDates() {
        assertEquals(LocalDate.of(2026, 11, 12), JalaliDateText.parse("۱۴۰۵/۰۸/۲۱"))
        assertEquals("1405/08/21", JalaliDateText.format(LocalDate.of(2026, 11, 12)))
        assertEquals("1404/12/29", JalaliDateText.format(LocalDate.of(2026, 3, 20)))
        assertEquals("1405/01/01", JalaliDateText.format(LocalDate.of(2026, 3, 21)))
        assertNull(JalaliDateText.parse("1405/07/31"))
        assertNull(JalaliDateText.parse("1404/12/30"))
        assertEquals(LocalDate.of(2025, 3, 20), JalaliDateText.parse("1403/12/30"))
        assertNull(JalaliDateText.parse("abc"))
    }
}
