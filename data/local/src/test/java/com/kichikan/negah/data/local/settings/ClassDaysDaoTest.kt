package com.kichikan.negah.data.local.settings

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
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
class ClassDaysDaoTest {
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
    fun replaceAllStoresOnlyTheSelectedDaysInWeekOrder() = runBlocking {
        val dao = database.classDaysDao()

        dao.replaceAll(listOf(ClassDayEntity(5), ClassDayEntity(1)))
        assertEquals(listOf(1, 5), dao.observeSelectedDayIndexes().first())

        dao.replaceAll(listOf(ClassDayEntity(0)))
        assertEquals(listOf(0), dao.observeSelectedDayIndexes().first())
    }

    @Test
    fun emptySelectionClearsPreviouslySelectedDays() = runBlocking {
        val dao = database.classDaysDao()

        dao.replaceAll(listOf(ClassDayEntity(2), ClassDayEntity(4)))
        dao.replaceAll(emptyList())

        assertEquals(emptyList<Int>(), dao.observeSelectedDayIndexes().first())
    }
}
