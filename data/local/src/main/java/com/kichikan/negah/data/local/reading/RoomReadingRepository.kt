package com.kichikan.negah.data.local.reading

import com.kichikan.negah.domain.reading.BookReading
import com.kichikan.negah.domain.reading.ReadingLevels
import com.kichikan.negah.domain.reading.ReadingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomReadingRepository @Inject constructor(
    private val dao: BookReadingDao,
) : ReadingRepository {
    override fun observeAll(): Flow<List<BookReading>> =
        dao.observeAll().map { rows -> rows.map(BookReadingEntity::toDomain) }

    override suspend fun add(reading: BookReading) {
        require(reading.id.isNotBlank() && reading.memberId.isNotBlank() && reading.title.isNotBlank())
        require(reading.level in ReadingLevels.MIN..ReadingLevels.MAX)
        dao.insert(BookReadingEntity.fromDomain(reading))
    }
}
