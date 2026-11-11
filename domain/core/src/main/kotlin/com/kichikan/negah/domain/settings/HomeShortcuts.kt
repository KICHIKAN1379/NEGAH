package com.kichikan.negah.domain.settings

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

enum class HomeShortcut(val id: String, val label: String) {
    MEMBERS("members", "اعضا"),
    PROGRESS("progress", "پیشرفت"),
    SESSIONS("sessions", "جلسات و حضور و غیاب"),
    SHOP("shop", "فروشگاه"),
    REFINERY("refinery", "پالایشگاه"),
    CLASS_DAYS("class_days", "روزهای کلاس");

    companion object {
        val defaultSelection = listOf(MEMBERS, PROGRESS, SESSIONS, SHOP)
        fun fromId(id: String): HomeShortcut? = entries.firstOrNull { it.id == id }
        const val MAX_SELECTION = 4
    }
}

interface HomeShortcutsRepository {
    fun observeSelectedShortcuts(): Flow<List<HomeShortcut>>
    suspend fun saveSelectedShortcuts(shortcuts: List<HomeShortcut>)
}

class SaveHomeShortcuts @Inject constructor(
    private val repository: HomeShortcutsRepository,
) {
    suspend operator fun invoke(shortcuts: List<HomeShortcut>) {
        require(shortcuts.size <= HomeShortcut.MAX_SELECTION) {
            "At most four home shortcuts can be selected."
        }
        require(shortcuts.distinct().size == shortcuts.size) {
            "A home shortcut cannot be selected more than once."
        }
        repository.saveSelectedShortcuts(shortcuts)
    }
}
