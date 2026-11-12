package com.kichikan.negah.data.local.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HomeShortcutsDao {
    @Query("SELECT shortcutId FROM home_shortcut_selections ORDER BY sortOrder ASC")
    abstract fun observeSelectedShortcutIds(): Flow<List<String>>

    @Query("SELECT configured FROM home_shortcut_settings WHERE id = 1")
    abstract fun observeConfigured(): Flow<Boolean?>

    @Query("DELETE FROM home_shortcut_selections")
    protected abstract suspend fun clearSelections()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSelections(shortcuts: List<HomeShortcutEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun setSettings(settings: HomeShortcutSettingsEntity)

    @Transaction
    open suspend fun replace(shortcuts: List<HomeShortcutEntity>) {
        clearSelections()
        if (shortcuts.isNotEmpty()) insertSelections(shortcuts)
        setSettings(HomeShortcutSettingsEntity(configured = true))
    }
}
