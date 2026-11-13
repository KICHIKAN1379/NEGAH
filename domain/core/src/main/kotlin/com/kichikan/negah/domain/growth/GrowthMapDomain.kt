package com.kichikan.negah.domain.growth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

enum class GrowthTrack { CAPABILITY, SELF_IMPROVEMENT }

data class GrowthItem(
    val id: String,
    val track: GrowthTrack,
    val title: String,
    val description: String?,
    val sortOrder: Int,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class MemberGrowthProgress(
    val memberId: String,
    val growthItemId: String,
    val progressPercent: Int,
    val note: String?,
    val updatedAtEpochMillis: Long,
)

/** Immutable audit row written whenever a member's progress value or note changes. */
data class GrowthProgressChange(
    val id: String,
    val memberId: String,
    val growthItemId: String,
    val previousPercent: Int?,
    val newPercent: Int,
    val note: String?,
    val changedAtEpochMillis: Long,
)

interface GrowthMapRepository {
    fun observeItems(track: GrowthTrack): Flow<List<GrowthItem>>
    fun observeAllItems(track: GrowthTrack): Flow<List<GrowthItem>>
    fun observeProgress(memberId: String): Flow<List<MemberGrowthProgress>>
    suspend fun saveItem(item: GrowthItem)
    suspend fun updateProgress(progress: MemberGrowthProgress)
    fun observeHistory(memberId: String): Flow<List<GrowthProgressChange>> = emptyFlow()
}

sealed interface UpdateGrowthProgressResult {
    data object Saved : UpdateGrowthProgressResult
    data object Invalid : UpdateGrowthProgressResult
}

class UpdateGrowthProgress @Inject constructor(
    private val repository: GrowthMapRepository,
) {
    suspend operator fun invoke(progress: MemberGrowthProgress): UpdateGrowthProgressResult {
        if (progress.memberId.isBlank() || progress.growthItemId.isBlank() ||
            progress.progressPercent !in 0..100 || progress.updatedAtEpochMillis < 0L
        ) return UpdateGrowthProgressResult.Invalid
        repository.updateProgress(progress)
        return UpdateGrowthProgressResult.Saved
    }
}
