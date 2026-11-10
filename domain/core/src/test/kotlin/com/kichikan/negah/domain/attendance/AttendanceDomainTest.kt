package com.kichikan.negah.domain.attendance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking

class AttendanceDomainTest {
    private class FakeAttendanceRepository : AttendanceRepository {
        var saved: List<AttendanceRecord> = emptyList()
        override fun observeForSession(sessionId: String): Flow<List<AttendanceRecord>> = emptyFlow()
        override fun observeAll(): Flow<List<AttendanceRecord>> = emptyFlow()
        override fun observeRevisionsForSession(sessionId: String): Flow<List<AttendanceRevision>> = emptyFlow()
        override suspend fun saveForSession(records: List<AttendanceRecord>) { saved = records }
    }

    @Test
    fun reasonlessAbsentExcusedAndUnrecordedEntriesAreRejected() = runBlocking {
        listOf(
            AttendanceStatus.ABSENT,
            AttendanceStatus.EXCUSED_ABSENCE,
            AttendanceStatus.UNRECORDED,
        ).forEach { status ->
            val repository = FakeAttendanceRepository()
            val result = SaveAttendance(repository)(
                sessionId = "session-1",
                drafts = listOf(AttendanceDraft("member-1", status, null)),
                nowEpochMillis = 100L,
            )
            assertEquals(SaveAttendanceResult.ReasonRequired("member-1"), result)
            assertTrue(repository.saved.isEmpty())
        }
    }

    @Test
    fun duplicateMemberEntriesAreRejected() = runBlocking {
        val repository = FakeAttendanceRepository()
        val result = SaveAttendance(repository)(
            sessionId = "session-1",
            drafts = listOf(
                AttendanceDraft("member-1", AttendanceStatus.PRESENT, null),
                AttendanceDraft("member-1", AttendanceStatus.LATE, null),
            ),
            nowEpochMillis = 100L,
        )
        assertIs<SaveAttendanceResult.DuplicateMember>(result)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun validEntriesAreConfirmedAndUseStableIds() = runBlocking {
        val repository = FakeAttendanceRepository()
        val useCase = SaveAttendance(repository)
        val draft = listOf(AttendanceDraft("member-1", AttendanceStatus.ABSENT, "بیماری"))

        assertEquals(SaveAttendanceResult.Saved, useCase("session-1", draft, 100L))
        val firstId = repository.saved.single().id
        assertTrue(repository.saved.single().isConfirmed)
        assertEquals("بیماری", repository.saved.single().reason)

        useCase("session-1", draft, 200L)
        assertEquals(firstId, repository.saved.single().id)
        assertEquals(200L, repository.saved.single().updatedAtEpochMillis)
    }
}
