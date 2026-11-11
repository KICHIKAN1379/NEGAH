package com.kichikan.negah.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking

class HomeShortcutsTest {
    private class FakeRepository : HomeShortcutsRepository {
        var saved: List<HomeShortcut>? = null
        override fun observeSelectedShortcuts(): Flow<List<HomeShortcut>> = emptyFlow()
        override suspend fun saveSelectedShortcuts(shortcuts: List<HomeShortcut>) {
            saved = shortcuts
        }
    }

    @Test
    fun savesFourOrFewerUniqueShortcuts() = runBlocking {
        val repository = FakeRepository()
        val save = SaveHomeShortcuts(repository)
        val selected = HomeShortcut.defaultSelection

        save(selected)

        assertEquals(selected, repository.saved)
    }

    @Test
    fun rejectsMoreThanFourShortcuts() = runBlocking {
        val repository = FakeRepository()
        val save = SaveHomeShortcuts(repository)

        assertFailsWith<IllegalArgumentException> {
            save(HomeShortcut.entries)
        }
        assertEquals(null, repository.saved)
    }

    @Test
    fun rejectsDuplicateShortcuts() = runBlocking {
        val repository = FakeRepository()
        val save = SaveHomeShortcuts(repository)

        assertFailsWith<IllegalArgumentException> {
            save(listOf(HomeShortcut.MEMBERS, HomeShortcut.MEMBERS))
        }
        assertEquals(null, repository.saved)
    }
}
