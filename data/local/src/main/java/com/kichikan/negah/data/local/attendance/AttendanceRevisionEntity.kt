package com.kichikan.negah.data.local.attendance

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_revisions",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["attendanceRecordId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("attendanceRecordId"), Index("sessionId")],
)
data class AttendanceRevisionEntity(
    @PrimaryKey val id: String,
    val attendanceRecordId: String,
    val sessionId: String,
    val memberId: String,
    val previousStatus: String,
    val previousReason: String?,
    val previousIsConfirmed: Boolean,
    val previousUpdatedAtEpochMillis: Long,
    val changedAtEpochMillis: Long,
)
