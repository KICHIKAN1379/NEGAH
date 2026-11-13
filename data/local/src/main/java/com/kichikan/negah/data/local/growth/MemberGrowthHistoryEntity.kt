package com.kichikan.negah.data.local.growth

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.growth.GrowthProgressChange

@Entity(
    tableName = "member_growth_history",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = GrowthItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["growthItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("memberId"), Index("growthItemId")],
)
data class MemberGrowthHistoryEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val growthItemId: String,
    val previousPercent: Int?,
    val newPercent: Int,
    val note: String?,
    val changedAtEpochMillis: Long,
) {
    fun toDomain() = GrowthProgressChange(
        id, memberId, growthItemId, previousPercent, newPercent, note, changedAtEpochMillis,
    )
}
