package com.kichikan.negah.domain.settings

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

enum class ClassDay(val order: Int, val label: String) {
    SATURDAY(0, "شنبه"),
    SUNDAY(1, "یکشنبه"),
    MONDAY(2, "دوشنبه"),
    TUESDAY(3, "سه‌شنبه"),
    WEDNESDAY(4, "چهارشنبه"),
    THURSDAY(5, "پنجشنبه"),
    FRIDAY(6, "جمعه");

    companion object {
        fun fromOrder(order: Int): ClassDay? = entries.firstOrNull { it.order == order }
    }
}

interface ClassDaysRepository {
    fun observeSelectedDays(): Flow<Set<ClassDay>>
    suspend fun saveSelectedDays(days: Set<ClassDay>)
}

class SaveClassDays @Inject constructor(
    private val repository: ClassDaysRepository,
) {
    suspend operator fun invoke(days: Set<ClassDay>) {
        repository.saveSelectedDays(days)
    }
}
