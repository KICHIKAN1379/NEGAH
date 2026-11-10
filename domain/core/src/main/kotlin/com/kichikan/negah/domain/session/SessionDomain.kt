package com.kichikan.negah.domain.session

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

data class Session(
    val id: String,
    val title: String,
    val sessionDateEpochDay: Long,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val topic: String?,
    val location: String?,
    val isCompleted: Boolean,
)

interface SessionRepository {
    fun observeAll(): Flow<List<Session>>
    suspend fun save(session: Session)
    suspend fun setCompleted(sessionId: String, isCompleted: Boolean)
}

sealed interface SaveSessionResult {
    data class Saved(val session: Session) : SaveSessionResult
    data object InvalidTitle : SaveSessionResult
}

class SaveSession @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(
        title: String,
        topic: String?,
        location: String?,
        notes: String?,
        sessionDateEpochDay: Long,
        nowEpochMillis: Long,
    ): SaveSessionResult {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return SaveSessionResult.InvalidTitle

        val session = Session(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            sessionDateEpochDay = sessionDateEpochDay,
            notes = notes?.trim()?.takeIf(String::isNotBlank),
            createdAtEpochMillis = nowEpochMillis,
            topic = topic?.trim()?.takeIf(String::isNotBlank),
            location = location?.trim()?.takeIf(String::isNotBlank),
            isCompleted = false,
        )
        repository.save(session)
        return SaveSessionResult.Saved(session)
    }
}
