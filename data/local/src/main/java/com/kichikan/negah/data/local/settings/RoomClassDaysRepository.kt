package com.kichikan.negah.data.local.settings

import com.kichikan.negah.domain.settings.ClassDay
import com.kichikan.negah.domain.settings.ClassDaysRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomClassDaysRepository @Inject constructor(
    private val dao: ClassDaysDao,
) : ClassDaysRepository {
    override fun observeSelectedDays(): Flow<Set<ClassDay>> =
        dao.observeSelectedDayIndexes().map { indexes ->
            indexes.mapNotNull { ClassDay.fromOrder(it) }.toSet()
        }

    override suspend fun saveSelectedDays(days: Set<ClassDay>) {
        dao.replaceAll(days.sortedBy { it.order }.map { ClassDayEntity(it.order) })
    }
}
