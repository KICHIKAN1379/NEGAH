package com.kichikan.negah.domain.attendance

enum class AttendanceStatus(val requiresReason: Boolean) {
    PRESENT(false),
    ABSENT(true),
    EXCUSED_ABSENCE(true),
    LATE(false),
    UNRECORDED(true),
}
