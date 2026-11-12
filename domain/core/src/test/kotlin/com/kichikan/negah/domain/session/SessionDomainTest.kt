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
    fun updatingSessionKeepsIdentityCreationTimeAndCompletionState() = runBlocking {
        val repository = FakeSessionRepository()
        val existing = Session(
            id = "session-existing",
            title = "عنوان قبلی",
            sessionDateEpochDay = 12L,
            notes = "یادداشت قبلی",
            createdAtEpochMillis = 123L,
            topic = "موضوع قبلی",
            location = "مکان قبلی",
            isCompleted = true,
        )

        val result = SaveSession(repository)(
            title = "  عنوان جدید  ",
            topic = "  موضوع جدید  ",
            location = " مکان جدید ",
            notes = " یادداشت جدید ",
            sessionDateEpochDay = 20L,
            nowEpochMillis = 999L,
            existingSession = existing,
        )

        val saved = assertIs<SaveSessionResult.Saved>(result).session
        assertEquals(existing.id, saved.id)
        assertEquals(existing.createdAtEpochMillis, saved.createdAtEpochMillis)
        assertEquals(existing.isCompleted, saved.isCompleted)
        assertEquals("عنوان جدید", saved.title)
        assertEquals("موضوع جدید", saved.topic)
        assertEquals("مکان جدید", saved.location)
        assertEquals("یادداشت جدید", saved.notes)
        assertEquals(20L, saved.sessionDateEpochDay)
        assertEquals(saved, repository.saved.single())
    }

    @Test
    fun blankTitleDoesNotOverwriteAnExistingSession() = runBlocking {
        val repository = FakeSessionRepository()
        val existing = Session(
            id = "session-existing", title = "عنوان قبلی", sessionDateEpochDay = 12L,
            notes = null, createdAtEpochMillis = 123L, topic = null, location = null,
            isCompleted = false,
        )

        val result = SaveSession(repository)(
            title = "  ", topic = null, location = null, notes = null,
            sessionDateEpochDay = 20L, nowEpochMillis = 999L, existingSession = existing,
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
