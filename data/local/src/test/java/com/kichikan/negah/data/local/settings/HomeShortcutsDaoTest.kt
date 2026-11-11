package com.kichikan.negah.data.local.settings

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.domain.settings.HomeShortcut
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
class HomeShortcutsDaoTest {
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
    fun firstRunUsesDefaultsAndSavedEmptySelectionStaysEmpty() = runBlocking {
        val repository = RoomHomeShortcutsRepository(database.homeShortcutsDao())

        assertEquals(HomeShortcut.defaultSelection, repository.observeSelectedShortcuts().first())

        repository.saveSelectedShortcuts(emptyList())

        assertEquals(emptyList<HomeShortcut>(), repository.observeSelectedShortcuts().first())
    }

    @Test
    fun savedShortcutsKeepTheSelectedOrder() = runBlocking {
        val repository = RoomHomeShortcutsRepository(database.homeShortcutsDao())
        val selected = listOf(HomeShortcut.SHOP, HomeShortcut.MEMBERS, HomeShortcut.CLASS_DAYS)

        repository.saveSelectedShortcuts(selected)

        assertEquals(selected, repository.observeSelectedShortcuts().first())
    }
}
