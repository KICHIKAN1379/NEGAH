package com.kichikan.negah.domain.attendance

data class AttendancePeriodSummary(
    val recordCount: Int,
    val sessionCount: Int,
    val statusCounts: Map<AttendanceStatus, Int>,
)

/**
 * Summarizes persisted attendance records for sessions whose local calendar date
 * falls inside the inclusive epoch-day range.
 */
fun summarizeAttendancePeriod(
    records: List<AttendanceRecord>,
    sessionDatesById: Map<String, Long>,
    startEpochDay: Long,
    endEpochDay: Long,
): AttendancePeriodSummary {
    require(startEpochDay <= endEpochDay) { "Start date must not be after end date." }
    val inRange = records.filter { record ->
        sessionDatesById[record.sessionId]?.let { it in startEpochDay..endEpochDay } == true
    }
    return AttendancePeriodSummary(
        recordCount = inRange.size,
        sessionCount = inRange.map { it.sessionId }.distinct().size,
        statusCounts = AttendanceStatus.entries.associateWith { status ->
            inRange.count { it.status == status }
        },
    )
}
