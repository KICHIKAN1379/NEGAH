package com.kichikan.negah.domain.group

import com.kichikan.negah.domain.member.Member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

private class FakeGroupRepository(private val accept: Boolean = true) : GroupRepository {
    var saved: Pair<MemberGroup, List<String>>? = null
    override fun observeGroups(): Flow<List<GroupWithMembers>> = flowOf(emptyList())
    override suspend fun saveGroup(group: MemberGroup, subMemberIds: List<String>): Boolean {
        if (accept) saved = group to subMemberIds
        return accept
    }
    override suspend fun archiveGroup(groupId: String, updatedAtEpochMillis: Long) = Unit
}

class GroupDomainTest {
    private fun member(id: String, xp: Long, points: Long = 0, diamonds: Long = 0, archived: Boolean = false) = Member(
        id = id, fullName = "عضو $id", nickname = null,
        totalXp = xp, spendablePoints = points, diamonds = diamonds,
        createdAtEpochMillis = 0, updatedAtEpochMillis = 0, isArchived = archived,
    )

    private fun group(id: String, name: String, leader: String, vararg subs: String, active: Boolean = true) =
        GroupWithMembers(MemberGroup(id, name, leader, active, 0, 0), subs.toList())

    @Test
    fun saveRequiresNameLeaderAndAtLeastOneSubMember() = runBlocking {
        val repo = FakeGroupRepository()
        val useCase = SaveGroup(repo)
        assertEquals(SaveGroupResult.InvalidName, useCase(null, "  ", "a", listOf("b"), 1L))
        assertEquals(SaveGroupResult.MissingLeader, useCase(null, "گروه", null, listOf("b"), 1L))
        assertEquals(SaveGroupResult.NoMembers, useCase(null, "گروه", "a", listOf("a"), 1L))
        assertEquals(null, repo.saved)
    }

    @Test
    fun savesTrimmedNameAndDropsLeaderAndDuplicatesFromSubMembers() = runBlocking {
        val repo = FakeGroupRepository()
        val result = SaveGroup(repo)(null, "  شاهین  ", "a", listOf("a", "b", "b", "c"), 5L)
        val group = assertIs<SaveGroupResult.Saved>(result).group
        assertEquals("شاهین", group.name)
        assertEquals("a", group.leaderMemberId)
        assertEquals(listOf("b", "c"), repo.saved?.second)
    }

    @Test
    fun conflictFromRepositoryIsReported() = runBlocking {
        val result = SaveGroup(FakeGroupRepository(accept = false))(null, "گروه", "a", listOf("b"), 1L)
        assertEquals(SaveGroupResult.MemberAlreadyInAnotherGroup, result)
    }

    @Test
    fun editingKeepsTheGroupIdAndCreationTime() = runBlocking {
        val repo = FakeGroupRepository()
        val result = SaveGroup(repo)("g1", "جدید", "a", listOf("b"), nowEpochMillis = 9L, createdAtEpochMillis = 2L)
        val saved = assertIs<SaveGroupResult.Saved>(result).group
        assertEquals("g1", saved.id)
        assertEquals(2L, saved.createdAtEpochMillis)
        assertEquals(9L, saved.updatedAtEpochMillis)
    }

    @Test
    fun rankingSumsXpOfRegisteredMembersAndIgnoresArchivedAndInactive() {
        val members = listOf(
            member("a", 100, points = 5, diamonds = 1), member("b", 50), member("c", 400),
            member("d", 90, archived = true), member("e", 10),
        )
        val groups = listOf(
            group("g1", "الف", "a", "b", "d"),
            group("g2", "ب", "c", "e"),
            group("g3", "غیرفعال", "a", active = false),
        )
        val ranking = GroupRankCalculator.rank(groups, members)
        assertEquals(listOf("g2", "g1"), ranking.map { it.group.id })
        assertEquals(410L, ranking[0].totalXp)
        assertEquals(150L, ranking[1].totalXp)
        assertEquals(2, ranking[1].memberCount)
        assertEquals(5L, ranking[1].totalSpendablePoints)
        assertEquals(1L, ranking[1].totalDiamonds)
    }
}
