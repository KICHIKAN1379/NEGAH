package com.kichikan.negah.data.local.growth

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.growth.MemberGrowthProgress

@Entity(
    tableName = "member_growth_progress",
    primaryKeys = ["memberId", "growthItemId"],
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
    indices = [Index("growthItemId")],
)
data class MemberGrowthProgressEntity(
    val memberId: String,
    val growthItemId: String,
    val progressPercent: Int,
    val note: String?,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain() = MemberGrowthProgress(
        memberId, growthItemId, progressPercent, note, updatedAtEpochMillis,
    )

    companion object {
        fun fromDomain(progress: MemberGrowthProgress) = MemberGrowthProgressEntity(
            progress.memberId, progress.growthItemId, progress.progressPercent,
            progress.note, progress.updatedAtEpochMillis,
        )
    }
}
