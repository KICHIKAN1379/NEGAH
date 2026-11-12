package com.kichikan.negah.domain.growth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

class UpdateGrowthProgressTest {
    private class FakeRepository : GrowthMapRepository {
        var saved: MemberGrowthProgress? = null
        override fun observeItems(track: GrowthTrack): Flow<List<GrowthItem>> = error("unused")
        override fun observeAllItems(track: GrowthTrack): Flow<List<GrowthItem>> = error("unused")
        override fun observeProgress(memberId: String): Flow<List<MemberGrowthProgress>> = error("unused")
        override suspend fun saveItem(item: GrowthItem) = Unit
        override suspend fun updateProgress(progress: MemberGrowthProgress) { saved = progress }
    }

    @Test fun rejectsProgressOutsideZeroToOneHundred() = runBlocking {
        val repository = FakeRepository()
        val useCase = UpdateGrowthProgress(repository)
        val result = useCase(MemberGrowthProgress("m1", "g1", 101, null, 1L))
        assertEquals(UpdateGrowthProgressResult.Invalid, result)
        assertEquals(null, repository.saved)
    }

    @Test fun savesValidProgress() = runBlocking {
        val repository = FakeRepository()
        val progress = MemberGrowthProgress("m1", "g1", 65, "تمرین مستمر", 1L)
        val result = UpdateGrowthProgress(repository)(progress)
        assertEquals(UpdateGrowthProgressResult.Saved, result)
        assertEquals(progress, repository.saved)
    }
}
