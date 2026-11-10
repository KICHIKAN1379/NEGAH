package com.kichikan.negah.data.local.attendance

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.data.local.session.SessionEntity
import com.kichikan.negah.domain.attendance.AttendanceStatus

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["sessionId", "memberId"], unique = true), Index("memberId")],
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val memberId: String,
    val status: AttendanceStatus,
    val reason: String?,
    val isConfirmed: Boolean,
    val updatedAtEpochMillis: Long,
)
