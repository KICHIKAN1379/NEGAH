package com.kichikan.negah.domain.attendance

import kotlin.test.Test
import kotlin.test.assertEquals

class AttendanceRulesTest {
    @Test
    fun reasonIsRequiredForThreeStatuses() {
        listOf(AttendanceStatus.ABSENT, AttendanceStatus.EXCUSED_ABSENCE, AttendanceStatus.UNRECORDED)
            .forEach { status ->
                assertEquals(AttendanceValidation.ReasonRequired, AttendanceRules.validate(status, null))
            }
    }

    @Test
    fun reasonIsOptionalForPresentAndLate() {
        listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE).forEach { status ->
            assertEquals(AttendanceValidation.Valid, AttendanceRules.validate(status, null))
        }
    }
}
