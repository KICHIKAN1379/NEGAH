package com.kichikan.negah.data.local.group

import com.kichikan.negah.domain.group.GroupEconomyAppendResult
import com.kichikan.negah.domain.group.GroupEconomyTransaction
import com.kichikan.negah.domain.group.GroupRepository
import com.kichikan.negah.domain.group.GroupWithMembers
import com.kichikan.negah.domain.group.MemberGroup
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomGroupRepository @Inject constructor(
    private val dao: GroupDao,
    private val economyDao: GroupEconomyDao,
) : GroupRepository {
    override fun observeGroups(): Flow<List<GroupWithMembers>> =
        combine(dao.observeGroups(), dao.observeMemberships()) { groups, memberships ->
            val byGroup = memberships.groupBy { it.groupId }
            groups.map { entity ->
                GroupWithMembers(
                    group = entity.toDomain(),
                    subMemberIds = byGroup[entity.id].orEmpty().map { it.memberId }.sorted(),
                )
            }
        }

    override suspend fun saveGroup(group: MemberGroup, subMemberIds: List<String>): Boolean {
        require(group.id.isNotBlank() && group.name.isNotBlank() && group.leaderMemberId.isNotBlank())
        return dao.saveGroup(MemberGroupEntity.fromDomain(group), subMemberIds)
    }

    override suspend fun archiveGroup(groupId: String, updatedAtEpochMillis: Long) =
        dao.deactivate(groupId, updatedAtEpochMillis)

    override fun observeEconomyTransactions(groupId: String): Flow<List<GroupEconomyTransaction>> =
        economyDao.observeForGroup(groupId).map { rows -> rows.map(GroupEconomyTransactionEntity::toDomain) }

    override suspend fun appendEconomyTransactions(
        transactions: List<GroupEconomyTransaction>,
    ): GroupEconomyAppendResult =
        economyDao.appendAtomically(transactions.map(GroupEconomyTransactionEntity::fromDomain))
}
