package com.kichikan.negah.domain.group

import com.kichikan.negah.domain.member.Member
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** A coach-defined group: exactly one leader (سرگروه) plus any number of sub-group members. */
data class MemberGroup(
    val id: String,
    val name: String,
    val leaderMemberId: String,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val spendablePoints: Long = 0L,
    val diamonds: Long = 0L,
)

data class GroupWithMembers(
    val group: MemberGroup,
    /** Sub-group members; the leader is not repeated here. */
    val subMemberIds: List<String>,
) {
    val allMemberIds: List<String> get() = listOf(group.leaderMemberId) + subMemberIds
}

interface GroupRepository {
    fun observeGroups(): Flow<List<GroupWithMembers>>

    /**
     * Atomically creates or updates a group and its sub-members. Returns false (and changes
     * nothing) when any selected member already belongs to another active group.
     */
    suspend fun saveGroup(group: MemberGroup, subMemberIds: List<String>): Boolean

    /** Soft-deactivates a group; it frees its members and is never hard-deleted. */
    suspend fun archiveGroup(groupId: String, updatedAtEpochMillis: Long)

    fun observeEconomyTransactions(groupId: String): Flow<List<GroupEconomyTransaction>>
    suspend fun appendEconomyTransactions(transactions: List<GroupEconomyTransaction>): GroupEconomyAppendResult
}


enum class GroupEconomyCurrency { POINTS, DIAMONDS }

data class GroupEconomyTransaction(
    val id: String,
    val groupId: String,
    val currency: GroupEconomyCurrency,
    /** Signed balance change; positive adds and negative subtracts. */
    val amountDelta: Long,
    val reason: String,
    val createdAtEpochMillis: Long,
)

enum class GroupEconomyAppendResult {
    APPENDED, GROUP_NOT_FOUND, INSUFFICIENT_BALANCE, BALANCE_OVERFLOW, INVALID_BATCH
}

sealed interface AdjustGroupEconomyResult {
    data object InvalidAdjustment : AdjustGroupEconomyResult
    data object Appended : AdjustGroupEconomyResult
    data object GroupNotFound : AdjustGroupEconomyResult
    data object InsufficientBalance : AdjustGroupEconomyResult
    data object BalanceOverflow : AdjustGroupEconomyResult
}

class AdjustGroupEconomy @Inject constructor(
    private val repository: GroupRepository,
) {
    suspend operator fun invoke(
        groupId: String,
        pointsDelta: Long,
        diamondsDelta: Long,
        reason: String,
        nowEpochMillis: Long,
    ): AdjustGroupEconomyResult {
        val cleanReason = reason.trim()
        if (groupId.isBlank() || cleanReason.isBlank() ||
            (pointsDelta == 0L && diamondsDelta == 0L)
        ) return AdjustGroupEconomyResult.InvalidAdjustment

        val transactions = buildList {
            if (pointsDelta != 0L) add(
                GroupEconomyTransaction(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    currency = GroupEconomyCurrency.POINTS,
                    amountDelta = pointsDelta,
                    reason = cleanReason,
                    createdAtEpochMillis = nowEpochMillis,
                ),
            )
            if (diamondsDelta != 0L) add(
                GroupEconomyTransaction(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    currency = GroupEconomyCurrency.DIAMONDS,
                    amountDelta = diamondsDelta,
                    reason = cleanReason,
                    createdAtEpochMillis = nowEpochMillis,
                ),
            )
        }
        return when (repository.appendEconomyTransactions(transactions)) {
            GroupEconomyAppendResult.APPENDED -> AdjustGroupEconomyResult.Appended
            GroupEconomyAppendResult.GROUP_NOT_FOUND -> AdjustGroupEconomyResult.GroupNotFound
            GroupEconomyAppendResult.INSUFFICIENT_BALANCE -> AdjustGroupEconomyResult.InsufficientBalance
            GroupEconomyAppendResult.BALANCE_OVERFLOW -> AdjustGroupEconomyResult.BalanceOverflow
            GroupEconomyAppendResult.INVALID_BATCH -> AdjustGroupEconomyResult.InvalidAdjustment
        }
    }
}

sealed interface SaveGroupResult {
    data class Saved(val group: MemberGroup) : SaveGroupResult
    data object InvalidName : SaveGroupResult
    data object MissingLeader : SaveGroupResult
    data object NoMembers : SaveGroupResult
    data object MemberAlreadyInAnotherGroup : SaveGroupResult
}

class SaveGroup @Inject constructor(
    private val repository: GroupRepository,
) {
    /** [groupId] null creates a new group; otherwise the existing group is edited. */
    suspend operator fun invoke(
        groupId: String?,
        name: String,
        leaderMemberId: String?,
        subMemberIds: Collection<String>,
        nowEpochMillis: Long,
        createdAtEpochMillis: Long = nowEpochMillis,
        spendablePoints: Long = 0L,
        diamonds: Long = 0L,
    ): SaveGroupResult {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return SaveGroupResult.InvalidName
        if (leaderMemberId.isNullOrBlank()) return SaveGroupResult.MissingLeader
        val subs = subMemberIds.filter { it != leaderMemberId }.distinct()
        if (subs.isEmpty()) return SaveGroupResult.NoMembers
        val group = MemberGroup(
            id = groupId ?: UUID.randomUUID().toString(),
            name = cleanName,
            leaderMemberId = leaderMemberId,
            isActive = true,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
            spendablePoints = spendablePoints,
            diamonds = diamonds,
        )
        return if (repository.saveGroup(group, subs)) SaveGroupResult.Saved(group)
        else SaveGroupResult.MemberAlreadyInAnotherGroup
    }
}

data class GroupRankEntry(
    val group: MemberGroup,
    val memberCount: Int,
    val totalSpendablePoints: Long,
    val totalDiamonds: Long,
)

/**
 * AK1 ranks by each active group's own spendable-point balance. Member balances,
 * member XP, and group diamonds do not affect the ranking. Archived members only
 * affect the displayed active-member count.
 */
object GroupRankCalculator {
    fun rank(groups: List<GroupWithMembers>, members: List<Member>): List<GroupRankEntry> {
        val byId = members.filter { !it.isArchived }.associateBy { it.id }
        return groups.filter { it.group.isActive }.map { entry ->
            val present = entry.allMemberIds.mapNotNull(byId::get)
            GroupRankEntry(
                group = entry.group,
                memberCount = present.size,
                totalSpendablePoints = entry.group.spendablePoints,
                totalDiamonds = entry.group.diamonds,
            )
        }.sortedWith(
            compareByDescending<GroupRankEntry> { it.totalSpendablePoints },
        )
    }
}
