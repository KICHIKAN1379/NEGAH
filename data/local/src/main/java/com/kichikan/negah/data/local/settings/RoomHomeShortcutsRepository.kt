package com.kichikan.negah.data.local.settings

import com.kichikan.negah.domain.settings.HomeShortcut
import com.kichikan.negah.domain.settings.HomeShortcutsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomHomeShortcutsRepository @Inject constructor(
    private val dao: HomeShortcutsDao,
) : HomeShortcutsRepository {
    override fun observeSelectedShortcuts(): Flow<List<HomeShortcut>> =
        combine(dao.observeConfigured(), dao.observeSelectedShortcutIds()) { configured, ids ->
            if (configured == true) {
                ids.mapNotNull(HomeShortcut::fromId)
            } else {
                HomeShortcut.defaultSelection
            }
        }

    override suspend fun saveSelectedShortcuts(shortcuts: List<HomeShortcut>) {
        dao.replace(shortcuts.mapIndexed { index, shortcut ->
            HomeShortcutEntity(shortcutId = shortcut.id, sortOrder = index)
        })
    }
}
