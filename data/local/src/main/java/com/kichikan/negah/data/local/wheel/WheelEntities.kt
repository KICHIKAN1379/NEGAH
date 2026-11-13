package com.kichikan.negah.data.local.wheel

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.wheel.WheelPrize
import com.kichikan.negah.domain.wheel.WheelSpin

@Entity(tableName = "wheel_prizes")
data class WheelPrizeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val currency: String,
    val amount: Long,
    val weight: Int,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain() = WheelPrize(id, label, EconomyCurrency.valueOf(currency), amount, weight, isActive)
}

/** Single-row settings table (id is always 1). */
@Entity(tableName = "wheel_settings")
data class WheelSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val spinCostPoints: Long,
)

@Entity(
    tableName = "wheel_spins",
    foreignKeys = [
        ForeignKey(entity = MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = WheelPrizeEntity::class, parentColumns = ["id"], childColumns = ["prizeId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("memberId"), Index("prizeId"), Index("spunAtEpochMillis")],
)
data class WheelSpinEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val prizeId: String,
    val prizeLabel: String,
    val currency: String,
    val amount: Long,
    val costPoints: Long,
    val spunAtEpochMillis: Long,
) {
    fun toDomain() = WheelSpin(id, memberId, prizeId, prizeLabel, EconomyCurrency.valueOf(currency), amount, costPoints, spunAtEpochMillis)
}
