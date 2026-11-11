package com.kichikan.negah.domain.report

import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.economy.EconomyTransactionKind
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberReportCalculatorTest {
    private val utc = ZoneId.of("UTC")

    private fun transaction(
        id: String,
        date: LocalDate,
        currency: EconomyCurrency,
        amount: Long,
    ) = EconomyTransaction(
        id = id,
        memberId = "member-1",
        currency = currency,
        amountDelta = amount,
        kind = if (amount < 0) EconomyTransactionKind.SPEND else EconomyTransactionKind.EARN,
        sourceType = "TEST",
        sourceId = id,
        createdAtEpochMillis = date.atStartOfDay(utc).toInstant().toEpochMilli(),
    )

    @Test
    fun monthlySummaryIncludesStartAndExcludesNextMonth() {
        val transactions = listOf(
            transaction("a", LocalDate.of(2026, 4, 1), EconomyCurrency.POINTS, 12),
            transaction("b", LocalDate.of(2026, 4, 30), EconomyCurrency.POINTS, -2),
            transaction("c", LocalDate.of(2026, 5, 1), EconomyCurrency.POINTS, 100),
            transaction("d", LocalDate.of(2026, 3, 31), EconomyCurrency.POINTS, 50),
        )

        val summary = MemberReportCalculator.forMonth(transactions, YearMonth.of(2026, 4), utc)

        assertEquals(10L, summary.pointsDelta)
        assertEquals(30, summary.pointsSeries.size)
        assertEquals(12L, summary.pointsSeries.first())
        assertEquals(-2L, summary.pointsSeries.last())
    }

    @Test
    fun annualSummaryKeepsCurrenciesIndependentAndGroupsByMonth() {
        val transactions = listOf(
            transaction("a", LocalDate.of(2026, 1, 2), EconomyCurrency.XP, 20),
            transaction("b", LocalDate.of(2026, 7, 5), EconomyCurrency.POINTS, 10),
            transaction("c", LocalDate.of(2026, 7, 6), EconomyCurrency.POINTS, -3),
            transaction("d", LocalDate.of(2026, 12, 31), EconomyCurrency.DIAMONDS, 4),
            transaction("e", LocalDate.of(2027, 1, 1), EconomyCurrency.DIAMONDS, 100),
        )

        val summary = MemberReportCalculator.forYear(transactions, 2026, utc)

        assertEquals(20L, summary.xpDelta)
        assertEquals(7L, summary.pointsDelta)
        assertEquals(4L, summary.diamondsDelta)
        assertEquals(12, summary.diamondsSeries.size)
        assertEquals(7L, summary.pointsSeries[6])
    }
}
