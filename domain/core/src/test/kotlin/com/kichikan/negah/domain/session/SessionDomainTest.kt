package com.kichikan.negah.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking

class SessionDomainTest {
    private class FakeSessionRepository : SessionRepository {
        val saved = mutableListOf<Session>()
        override fun observeAll(): Flow<List<Session>> = emptyFlow()
        override suspend fun save(session: Session) { saved += session }
        override suspend fun setCompleted(sessionId: String, isCompleted: Boolean) = Unit
    }

    @Test
    fun blankTitleIsRejected() = runBlocking {
        val repository = FakeSessionRepository()
        val result = SaveSession(repository)(
            title = "   ",
            topic = null,
            location = null,
            notes = null,
            sessionDateEpochDay = 1L,
            nowEpochMillis = 10L,
        )
        assertIs<SaveSessionResult.InvalidTitle>(result)
        assertEquals(0, repository.saved.size)
    }

    @Test
    fun titleAndOptionalFieldsAreNormalizedBeforeSaving() = runBlocking {
        val repository = FakeSessionRepository()
        val result = SaveSession(repository)(
            title = "  جلسه هفتگی  ",
            topic = "  اخلاق  ",
            location = " ",
            notes = "  مرور هفته  ",
            sessionDateEpochDay = 12L,
            nowEpochMillis = 99L,
        )
        val saved = assertIs<SaveSessionResult.Saved>(result).session
        assertEquals("جلسه هفتگی", saved.title)
        assertEquals("اخلاق", saved.topic)
        assertEquals(null, saved.location)
        assertEquals("مرور هفته", saved.notes)
        assertEquals(12L, saved.sessionDateEpochDay)
    }
}
