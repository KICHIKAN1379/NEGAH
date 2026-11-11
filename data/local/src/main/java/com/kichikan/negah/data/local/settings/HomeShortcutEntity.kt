package com.kichikan.negah.data.local.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_shortcut_selections")
data class HomeShortcutEntity(
    @PrimaryKey val shortcutId: String,
    val sortOrder: Int,
)

@Entity(tableName = "home_shortcut_settings")
data class HomeShortcutSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val configured: Boolean = true,
)
