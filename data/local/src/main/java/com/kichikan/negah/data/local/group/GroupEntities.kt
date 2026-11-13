package com.kichikan.negah.data.local.group

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.group.MemberGroup

@Entity(
    tableName = "member_groups",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["leaderMemberId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("leaderMemberId")],
)
data class MemberGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val leaderMemberId: String,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val spendablePoints: Long = 0L,
    val diamonds: Long = 0L,
) {
    fun toDomain() = MemberGroup(id, name, leaderMemberId, isActive, createdAtEpochMillis, updatedAtEpochMillis, spendablePoints, diamonds)

    companion object {
        fun fromDomain(group: MemberGroup) = MemberGroupEntity(
            group.id, group.name, group.leaderMemberId, group.isActive,
            group.createdAtEpochMillis, group.updatedAtEpochMillis, group.spendablePoints, group.diamonds,
        )
    }
}

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = MemberGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("memberId")],
)
data class GroupMemberEntity(
    val groupId: String,
    val memberId: String,
)
