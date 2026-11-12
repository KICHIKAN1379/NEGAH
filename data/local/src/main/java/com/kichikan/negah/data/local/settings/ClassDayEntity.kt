package com.kichikan.negah.data.local.settings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_days")
data class ClassDayEntity(
    @PrimaryKey val weekdayIndex: Int,
)
