package com.kichikan.negah.data.local.attendance

import androidx.room.TypeConverter
import com.kichikan.negah.domain.attendance.AttendanceStatus

class AttendanceStatusConverter {
    @TypeConverter
    fun fromStatus(status: AttendanceStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
}
