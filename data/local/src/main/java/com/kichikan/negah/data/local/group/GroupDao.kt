package com.kichikan.negah.data.local.group

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM member_groups ORDER BY createdAtEpochMillis, id")
    fun observeGroups(): Flow<List<MemberGroupEntity>>

    @Query("SELECT * FROM group_members")
    fun observeMemberships(): Flow<List<GroupMemberEntity>>

    @Upsert
    suspend fun upsertGroup(group: MemberGroupEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM member_groups WHERE id = :groupId)")
    suspend fun groupExists(groupId: String): Boolean

    @Query(
        """
        UPDATE member_groups
        SET name = :name,
            leaderMemberId = :leaderMemberId,
            isActive = :isActive,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :groupId
        """,
    )
    suspend fun updateGroupDetails(
        groupId: String,
        name: String,
        leaderMemberId: String,
        isActive: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearMemberships(groupId: String)

    @Upsert
    suspend fun upsertMemberships(rows: List<GroupMemberEntity>)

    @Query("UPDATE member_groups SET isActive = 0, updatedAtEpochMillis = :updatedAt WHERE id = :groupId")
    suspend fun deactivate(groupId: String, updatedAt: Long)

    /** Members that already belong (as leader or sub-member) to another ACTIVE group. */
    @Query(
        """
        SELECT memberId FROM group_members gm JOIN member_groups g ON g.id = gm.groupId
        WHERE g.isActive = 1 AND g.id != :excludeGroupId AND gm.memberId IN (:memberIds)
        UNION
        SELECT leaderMemberId FROM member_groups
        WHERE isActive = 1 AND id != :excludeGroupId AND leaderMemberId IN (:memberIds)
        """,
    )
    suspend fun findAssignedElsewhere(memberIds: List<String>, excludeGroupId: String): List<String>

    @Transaction
    suspend fun saveGroup(group: MemberGroupEntity, subMemberIds: List<String>): Boolean {
        val everyone = (subMemberIds + group.leaderMemberId).distinct()
        if (group.isActive && findAssignedElsewhere(everyone, group.id).isNotEmpty()) return false
        if (groupExists(group.id)) {
            updateGroupDetails(
                groupId = group.id,
                name = group.name,
                leaderMemberId = group.leaderMemberId,
                isActive = group.isActive,
                updatedAt = group.updatedAtEpochMillis,
            )
        } else {
            upsertGroup(group)
        }
        clearMemberships(group.id)
        upsertMemberships(subMemberIds.distinct().filter { it != group.leaderMemberId }.map { GroupMemberEntity(group.id, it) })
        return true
    }
}
