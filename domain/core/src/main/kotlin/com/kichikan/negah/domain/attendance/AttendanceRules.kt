package com.kichikan.negah.domain.attendance

sealed interface AttendanceValidation {
    data object Valid : AttendanceValidation
    data object ReasonRequired : AttendanceValidation
}

object AttendanceRules {
    fun validate(status: AttendanceStatus, reason: String?): AttendanceValidation =
        if (status.requiresReason && reason.isNullOrBlank()) {
            AttendanceValidation.ReasonRequired
        } else {
            AttendanceValidation.Valid
        }
}
