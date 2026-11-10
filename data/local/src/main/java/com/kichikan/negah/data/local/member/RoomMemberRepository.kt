package com.kichikan.negah.data.local.member

import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomMemberRepository @Inject constructor(private val memberDao: MemberDao) : MemberRepository {
    override fun observeActiveMembers(): Flow<List<Member>> =
        memberDao.observeActiveMembers().map { list -> list.map(MemberEntity::toDomain) }
    override fun observeArchivedMembers(): Flow<List<Member>> =
        memberDao.observeArchivedMembers().map { list -> list.map(MemberEntity::toDomain) }
    override suspend fun create(member: Member) = memberDao.insert(MemberEntity.fromDomain(member))
    override suspend fun archive(memberId: String) = memberDao.archive(memberId, System.currentTimeMillis())
    override suspend fun restore(memberId: String) = memberDao.restore(memberId, System.currentTimeMillis())
}
