package com.kichikan.negah.domain.attendance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AttendancePeriodSummaryTest {
    private fun record(id: String, session: String, member: String, status: AttendanceStatus) =
        AttendanceRecord(id, session, member, status, null, true, 1L)

    @Test
    fun summarizesOnlyRecordsWhoseSessionDateIsInsideInclusiveRange() {
        val records = listOf(
            record("1", "s1", "m1", AttendanceStatus.PRESENT),
            record("2", "s1", "m2", AttendanceStatus.LATE),
            record("3", "s2", "m1", AttendanceStatus.ABSENT),
            record("4", "s3", "m1", AttendanceStatus.UNRECORDED),
            record("5", "missing-session", "m2", AttendanceStatus.EXCUSED_ABSENCE),
        )

        val summary = summarizeAttendancePeriod(
            records = records,
            sessionDatesById = mapOf("s1" to 100L, "s2" to 110L, "s3" to 120L),
            startEpochDay = 100L,
            endEpochDay = 110L,
        )

        assertEquals(3, summary.recordCount)
        assertEquals(2, summary.sessionCount)
        assertEquals(1, summary.statusCounts[AttendanceStatus.PRESENT])
        assertEquals(1, summary.statusCounts[AttendanceStatus.LATE])
        assertEquals(1, summary.statusCounts[AttendanceStatus.ABSENT])
        assertEquals(0, summary.statusCounts[AttendanceStatus.EXCUSED_ABSENCE])
        assertEquals(0, summary.statusCounts[AttendanceStatus.UNRECORDED])
    }

    @Test
    fun rejectsInvertedDateRange() {
        assertFailsWith<IllegalArgumentException> {
            summarizeAttendancePeriod(emptyList(), emptyMap(), 20L, 10L)
        }
    }
}
