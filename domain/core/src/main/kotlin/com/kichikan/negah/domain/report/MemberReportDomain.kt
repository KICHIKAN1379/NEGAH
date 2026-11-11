package com.kichikan.negah.domain.report

import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MemberReportSummary(
    val xpDelta: Long,
    val pointsDelta: Long,
    val diamondsDelta: Long,
    val xpSeries: List<Long>,
    val pointsSeries: List<Long>,
    val diamondsSeries: List<Long>,
)

object MemberReportCalculator {
    fun forMonth(
        transactions: List<EconomyTransaction>,
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MemberReportSummary = calculate(
        transactions = transactions,
        start = month.atDay(1),
        endExclusive = month.plusMonths(1).atDay(1),
        bucketCount = month.lengthOfMonth(),
        bucketIndex = { it.dayOfMonth - 1 },
        zoneId = zoneId,
    )

    fun forYear(
        transactions: List<EconomyTransaction>,
        year: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MemberReportSummary {
        require(year in 1..9998) { "Year must be in the supported calendar range" }
        return calculate(
            transactions = transactions,
            start = LocalDate.of(year, 1, 1),
            endExclusive = LocalDate.of(year + 1, 1, 1),
            bucketCount = 12,
            bucketIndex = { it.monthValue - 1 },
            zoneId = zoneId,
        )
    }

    private fun calculate(
        transactions: List<EconomyTransaction>,
        start: LocalDate,
        endExclusive: LocalDate,
        bucketCount: Int,
        bucketIndex: (LocalDate) -> Int,
        zoneId: ZoneId,
    ): MemberReportSummary {
        val xp = MutableList(bucketCount) { 0L }
        val points = MutableList(bucketCount) { 0L }
        val diamonds = MutableList(bucketCount) { 0L }

        transactions.forEach { transaction ->
            val date = Instant.ofEpochMilli(transaction.createdAtEpochMillis)
                .atZone(zoneId)
                .toLocalDate()
            if (date < start || date >= endExclusive) return@forEach
            val index = bucketIndex(date)
            when (transaction.currency) {
                EconomyCurrency.XP -> xp[index] = Math.addExact(xp[index], transaction.amountDelta)
                EconomyCurrency.POINTS -> points[index] = Math.addExact(points[index], transaction.amountDelta)
                EconomyCurrency.DIAMONDS -> diamonds[index] = Math.addExact(diamonds[index], transaction.amountDelta)
            }
        }

        return MemberReportSummary(
            xpDelta = xp.sum(),
            pointsDelta = points.sum(),
            diamondsDelta = diamonds.sum(),
            xpSeries = xp,
            pointsSeries = points,
            diamondsSeries = diamonds,
        )
    }
}
