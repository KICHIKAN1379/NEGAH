package com.kichikan.negah.data.local.session

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.attendance.AttendanceEntity
import com.kichikan.negah.data.local.member.MemberEntity
import com.kichikan.negah.domain.attendance.AttendanceStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionAttendanceDaoTest {
    private lateinit var database: NegahDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun attendanceUpdatesPreservePreviousStateAndBlockSessionDeletion() = runBlocking {
        database.memberDao().insert(
            MemberEntity(
                id = "member-1",
                fullName = "عضو آزمایشی",
                nickname = null,
                totalXp = 0L,
                spendablePoints = 0L,
                diamonds = 0L,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L,
                isArchived = false,
            ),
        )
        database.sessionDao().upsert(
            SessionEntity(
                id = "session-1",
                title = "جلسه آزمایشی",
                sessionDateEpochDay = 10L,
                notes = null,
                createdAtEpochMillis = 1L,
                topic = "اخلاق",
                location = "مسجد",
                isCompleted = false,
            ),
        )

        val first = AttendanceEntity(
            id = "original-record-id",
            sessionId = "session-1",
            memberId = "member-1",
            status = AttendanceStatus.PRESENT,
            reason = null,
            isConfirmed = true,
            updatedAtEpochMillis = 10L,
        )
        database.attendanceDao().saveForSession(listOf(first))

        val changed = first.copy(
            id = "new-incoming-id",
            status = AttendanceStatus.ABSENT,
            reason = "بیماری",
            updatedAtEpochMillis = 20L,
        )
        database.attendanceDao().saveForSession(listOf(changed))

        val current = database.attendanceDao().observeForSession("session-1").first()
        assertEquals(1, current.size)
        assertEquals("original-record-id", current.single().id)
        assertEquals(AttendanceStatus.ABSENT, current.single().status)

        val revisions = database.attendanceDao().observeRevisionsForSession("session-1").first()
        assertEquals(1, revisions.size)
        assertEquals("PRESENT", revisions.single().previousStatus)
        assertEquals(10L, revisions.single().previousUpdatedAtEpochMillis)

        var deletionBlocked = false
        try {
            database.sessionDao().deleteById("session-1")
        } catch (_: SQLiteConstraintException) {
            deletionBlocked = true
        }
        assertTrue(deletionBlocked)
        assertNotNull(database.sessionDao().findById("session-1"))
    }

    @Test
    fun sessionCanBeMarkedCompleted() = runBlocking {
        val dao = database.sessionDao()
        dao.upsert(
            SessionEntity(
                id = "session-2",
                title = "جلسه دوم",
                sessionDateEpochDay = 11L,
                notes = null,
                createdAtEpochMillis = 2L,
                topic = null,
                location = null,
                isCompleted = false,
            ),
        )

        dao.setCompleted("session-2", true)

        assertEquals(true, dao.findById("session-2")?.isCompleted)
    }
}
