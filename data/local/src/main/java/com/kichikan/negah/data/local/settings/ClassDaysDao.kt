package com.kichikan.negah.data.local.settings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ClassDaysDao {
    @Query("SELECT weekdayIndex FROM class_days ORDER BY weekdayIndex ASC")
    abstract fun observeSelectedDayIndexes(): Flow<List<Int>>

    @Query("DELETE FROM class_days")
    protected abstract suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertAll(days: List<ClassDayEntity>)

    @Transaction
    open suspend fun replaceAll(days: List<ClassDayEntity>) {
        clearAll()
        if (days.isNotEmpty()) insertAll(days)
    }
}
