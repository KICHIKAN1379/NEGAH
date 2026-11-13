package com.kichikan.negah.data.local.group

import androidx.room.Room
import com.kichikan.negah.data.local.NegahDatabase
import com.kichikan.negah.data.local.member.MemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GroupDaoTest {
    private lateinit var database: NegahDatabase
    private lateinit var dao: GroupDao

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NegahDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.groupDao()
        listOf("a", "b", "c", "d").forEach { id ->
            database.memberDao().insert(
                MemberEntity(id = id, fullName = "Member $id", nickname = null, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun group(id: String, leader: String) =
        MemberGroupEntity(id, "Group $id", leader, true, 1L, 1L)

    @Test
    fun savesGroupWithLeaderAndSubMembers() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b", "c")))
        assertEquals(listOf("g1"), dao.observeGroups().first().map { it.id })
        assertEquals(setOf("b", "c"), dao.observeMemberships().first().map { it.memberId }.toSet())
    }

    @Test
    fun memberCannotJoinASecondActiveGroupAsLeaderOrSubMember() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b")))
        assertFalse(dao.saveGroup(group("g2", "c"), listOf("b")))
        assertFalse(dao.saveGroup(group("g3", "a"), listOf("d")))
        assertEquals(listOf("g1"), dao.observeGroups().first().map { it.id })
    }

    @Test
    fun editingReplacesMembershipAndArchivingFreesMembers() = runBlocking {
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("b", "c")))
        assertTrue(dao.saveGroup(group("g1", "a"), listOf("d")))
        assertEquals(setOf("d"), dao.observeMemberships().first().map { it.memberId }.toSet())

        dao.deactivate("g1", 5L)
        assertTrue(dao.saveGroup(group("g2", "b"), listOf("a", "d")))
        assertEquals(false, dao.observeGroups().first().first { it.id == "g1" }.isActive)
    }
}
