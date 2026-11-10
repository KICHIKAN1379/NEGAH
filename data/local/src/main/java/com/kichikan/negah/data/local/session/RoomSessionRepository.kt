package com.kichikan.negah.data.local.session

import com.kichikan.negah.domain.session.Session
import com.kichikan.negah.domain.session.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSessionRepository @Inject constructor(
    private val dao: SessionDao,
) : SessionRepository {
    override fun observeAll(): Flow<List<Session>> =
        dao.observeAll().map { sessions -> sessions.map(SessionEntity::toDomain) }

    override suspend fun save(session: Session) {
        dao.upsert(SessionEntity.fromDomain(session))
    }

    override suspend fun delete(sessionId: String): Boolean =
        dao.deleteById(sessionId) > 0

    override suspend fun setCompleted(sessionId: String, isCompleted: Boolean) {
        dao.setCompleted(sessionId, isCompleted)
    }
}
