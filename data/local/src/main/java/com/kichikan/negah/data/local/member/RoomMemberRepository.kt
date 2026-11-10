package com.kichikan.negah.data.local.member

import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomMemberRepository @Inject constructor(private val memberDao: MemberDao) : MemberRepository {
    override fun observeActiveMembers(): Flow<List<Member>> =
        memberDao.observeActiveMembers().map { list -> list.map(MemberEntity::toDomain) }
    override suspend fun save(member: Member) = memberDao.upsert(MemberEntity.fromDomain(member))
    override suspend fun archive(memberId: String) = memberDao.archive(memberId, System.currentTimeMillis())
}
