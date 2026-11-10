package com.kichikan.negah.data.local.member

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kichikan.negah.domain.member.Member

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val nickname: String?,
    val totalXp: Long = 0L,
    val spendablePoints: Long = 0L,
    val diamonds: Long = 0L,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val isArchived: Boolean = false,
) {
    fun toDomain() = Member(id, fullName, nickname, totalXp, spendablePoints, diamonds,
        createdAtEpochMillis, updatedAtEpochMillis, isArchived)
    companion object {
        fun fromDomain(member: Member) = MemberEntity(
            member.id, member.fullName, member.nickname, member.totalXp, member.spendablePoints,
            member.diamonds, member.createdAtEpochMillis, member.updatedAtEpochMillis, member.isArchived,
        )
    }
}
