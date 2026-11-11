package com.kichikan.negah.data.local.settings

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.domain.settings.RingProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RingProfileDaoTest {
    private lateinit var database: NegahDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun firstRunReturnsAnEmptyProfileWithoutInventingAnIdentity() = runBlocking {
        val repository = RoomRingProfileRepository(database.ringProfileDao())

        assertEquals(RingProfile(), repository.observeProfile().first())
    }

    @Test
    fun savedRingProfileSurvivesRepositoryRecreation() = runBlocking {
        val profile = RingProfile(
            name = "حلقه نمونه",
            username = "halaqe-01",
            updatedAtEpochMillis = 1234L,
        )
        RoomRingProfileRepository(database.ringProfileDao()).saveProfile(profile)

        val reloaded = RoomRingProfileRepository(database.ringProfileDao())
            .observeProfile().first()

        assertEquals(profile, reloaded)
    }

    @Test
    fun blankOptionalUsernameIsStoredAsNull() = runBlocking {
        val profile = RingProfile(name = "حلقه", username = null, updatedAtEpochMillis = 2L)
        RoomRingProfileRepository(database.ringProfileDao()).saveProfile(profile)

        assertEquals(profile, RoomRingProfileRepository(database.ringProfileDao()).observeProfile().first())
    }
}
