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
        )
        return if (repository.saveGroup(group, subs)) SaveGroupResult.Saved(group)
        else SaveGroupResult.MemberAlreadyInAnotherGroup
    }
}

data class GroupRankEntry(
    val group: MemberGroup,
    val memberCount: Int,
    val totalXp: Long,
    val totalSpendablePoints: Long,
    val totalDiamonds: Long,
)

/**
 * Group ranking uses the summed XP (permanent progress) of the group's registered members;
 * ties fall back to member count and then name for a stable order. Archived or unknown
 * members contribute nothing.
 */
object GroupRankCalculator {
    fun rank(groups: List<GroupWithMembers>, members: List<Member>): List<GroupRankEntry> {
        val byId = members.filter { !it.isArchived }.associateBy { it.id }
        return groups.filter { it.group.isActive }.map { entry ->
            val present = entry.allMemberIds.mapNotNull(byId::get)
            GroupRankEntry(
                group = entry.group,
                memberCount = present.size,
                totalXp = present.sumOf { it.totalXp },
                totalSpendablePoints = present.sumOf { it.spendablePoints },
                totalDiamonds = present.sumOf { it.diamonds },
            )
        }.sortedWith(
            compareByDescending<GroupRankEntry> { it.totalXp }
                .thenByDescending { it.memberCount }
                .thenBy { it.group.name },
        )
    }
}
