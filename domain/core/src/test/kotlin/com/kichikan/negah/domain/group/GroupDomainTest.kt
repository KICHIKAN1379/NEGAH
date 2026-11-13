package com.kichikan.negah.domain.group

import com.kichikan.negah.domain.member.Member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

private class FakeGroupRepository(private val accept: Boolean = true) : GroupRepository {
    var saved: Pair<MemberGroup, List<String>>? = null
    var economyTransactions = emptyList<GroupEconomyTransaction>()

    override fun observeGroups(): Flow<List<GroupWithMembers>> = flowOf(emptyList())
    override suspend fun saveGroup(group: MemberGroup, subMemberIds: List<String>): Boolean {
        if (accept) saved = group to subMemberIds
        return accept
    }
    override suspend fun archiveGroup(groupId: String, updatedAtEpochMillis: Long) = Unit
    override fun observeEconomyTransactions(groupId: String) =
        flowOf(economyTransactions.filter { it.groupId == groupId })

    override suspend fun appendEconomyTransactions(
        transactions: List<GroupEconomyTransaction>,
    ): GroupEconomyAppendResult {
        if (!accept) return GroupEconomyAppendResult.GROUP_NOT_FOUND
        economyTransactions = economyTransactions + transactions
        return GroupEconomyAppendResult.APPENDED
    }
}

class GroupDomainTest {
    private fun member(
        id: String,
        xp: Long,
        points: Long = 0,
        diamonds: Long = 0,
        archived: Boolean = false,
    ) = Member(
        id = id,
        fullName = "عضو $id",
        nickname = null,
        totalXp = xp,
        spendablePoints = points,
        diamonds = diamonds,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
        isArchived = archived,
    )

    private fun group(
        id: String,
        name: String,
        leader: String,
        vararg subs: String,
        active: Boolean = true,
        points: Long = 0L,
        diamonds: Long = 0L,
    ) = GroupWithMembers(
        MemberGroup(id, name, leader, active, 0, 0, points, diamonds),
        subs.toList(),
    )

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
        val saved = assertIs<SaveGroupResult.Saved>(result).group
        assertEquals("شاهین", saved.name)
        assertEquals("a", saved.leaderMemberId)
        assertEquals(listOf("b", "c"), repo.saved?.second)
    }

    @Test
    fun conflictFromRepositoryIsReported() = runBlocking {
        val result = SaveGroup(FakeGroupRepository(accept = false))(
            null, "گروه", "a", listOf("b"), 1L,
        )
        assertEquals(SaveGroupResult.MemberAlreadyInAnotherGroup, result)
    }

    @Test
    fun editingKeepsTheGroupIdAndCreationTime() = runBlocking {
        val repo = FakeGroupRepository()
        val result = SaveGroup(repo)(
            "g1", "جدید", "a", listOf("b"), nowEpochMillis = 9L, createdAtEpochMillis = 2L,
        )
        val saved = assertIs<SaveGroupResult.Saved>(result).group
        assertEquals("g1", saved.id)
        assertEquals(2L, saved.createdAtEpochMillis)
        assertEquals(9L, saved.updatedAtEpochMillis)
    }

    @Test
    fun groupRankingUsesIndependentGroupPointsNotMemberBalancesXpOrDiamonds() {
        val members = listOf(
            member("a", xp = 1, points = 900),
            member("b", xp = 2, points = 800),
            member("c", xp = 9_999, points = 0, diamonds = 10_000),
        )
        val groups = listOf(
            group("low", "الف", "a", "b", points = 10, diamonds = 500),
            group("high", "ب", "c", points = 50, diamonds = 0),
            group("inactive", "غیرفعال", "a", active = false, points = 999),
        )

        val ranking = GroupRankCalculator.rank(groups, members)

        assertEquals(listOf("high", "low"), ranking.map { it.group.id })
        assertEquals(50L, ranking.first().totalSpendablePoints)
        assertEquals(10L, ranking.last().totalSpendablePoints)
        assertEquals(500L, ranking.last().totalDiamonds)
    }

    @Test
    fun groupRankingCountsOnlyNonArchivedMembers() {
        val members = listOf(member("a", 10), member("b", 20, archived = true))
        val ranking = GroupRankCalculator.rank(
            listOf(group("g", "گروه", "a", "b", points = 4)),
            members,
        )
        assertEquals(1, ranking.single().memberCount)
        assertEquals(4L, ranking.single().totalSpendablePoints)
    }

    @Test
    fun groupEconomyAdjustmentRequiresReasonAndNonZeroDelta() = runBlocking {
        val repository = FakeGroupRepository()
        val adjust = AdjustGroupEconomy(repository)
        assertEquals(
            AdjustGroupEconomyResult.InvalidAdjustment,
            adjust("g1", 10, 0, "  ", 1L),
        )
        assertEquals(
            AdjustGroupEconomyResult.InvalidAdjustment,
            adjust("g1", 0, 0, "اصلاح", 1L),
        )
        assertTrue(repository.economyTransactions.isEmpty())
    }

    @Test
    fun groupEconomyAdjustmentWritesIndependentLedgerRows() = runBlocking {
        val repository = FakeGroupRepository()
        val result = AdjustGroupEconomy(repository)("g1", 25, 3, "پاداش گروهی", 9L)

        assertEquals(AdjustGroupEconomyResult.Appended, result)
        assertEquals(
            listOf(GroupEconomyCurrency.POINTS, GroupEconomyCurrency.DIAMONDS),
            repository.economyTransactions.map { it.currency },
        )
        assertTrue(repository.economyTransactions.all { it.reason == "پاداش گروهی" })
        assertTrue(repository.economyTransactions.all { it.amountDelta > 0 })
    }

    @Test
    fun groupEconomyAdjustmentAllowsReasonedNegativeCorrection() = runBlocking {
        val repository = FakeGroupRepository()
        val result = AdjustGroupEconomy(repository)("g1", -4, 0, "اصلاح اشتباه", 10L)

        assertEquals(AdjustGroupEconomyResult.Appended, result)
        assertEquals(listOf(GroupEconomyCurrency.POINTS), repository.economyTransactions.map { it.currency })
        assertEquals(-4L, repository.economyTransactions.single().amountDelta)
    }
}
