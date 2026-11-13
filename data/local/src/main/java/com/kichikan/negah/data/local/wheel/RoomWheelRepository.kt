package com.kichikan.negah.data.local.wheel

import com.kichikan.negah.domain.wheel.WheelPrize
import com.kichikan.negah.domain.wheel.WheelRepository
import com.kichikan.negah.domain.wheel.WheelSpin
import com.kichikan.negah.domain.wheel.WheelSpinResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWheelRepository @Inject constructor(private val dao: WheelDao) : WheelRepository {
    override fun observePrizes(): Flow<List<WheelPrize>> = dao.observePrizes().map { rows -> rows.map { it.toDomain() } }
    override fun observeSpinCost(): Flow<Long> = dao.observeSpinCost().map { it ?: 0L }
    override fun observeSpins(): Flow<List<WheelSpin>> = dao.observeSpins().map { rows -> rows.map { it.toDomain() } }

    override suspend fun savePrize(prize: WheelPrize, nowEpochMillis: Long) {
        dao.savePrize(
            WheelPrizeEntity(prize.id, prize.label, prize.currency.name, prize.amount, prize.weight, prize.isActive, nowEpochMillis, nowEpochMillis),
        )
    }

    override suspend fun setSpinCost(costPoints: Long) {
        require(costPoints >= 0L)
        dao.upsertSettings(WheelSettingsEntity(1, costPoints))
    }

    override suspend fun spin(memberId: String, spinId: String, nowEpochMillis: Long, roll: (Long) -> Long): WheelSpinResult =
        dao.spinAtomically(memberId, spinId, nowEpochMillis, roll)
}
