package com.kichikan.negah.domain.member

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegisterMemberTest {
    private class FakeRepository : MemberRepository {
        val saved = mutableListOf<Member>()
        override fun observeActiveMembers() = flowOf(saved.toList())
        override suspend fun save(member: Member) { saved += member }
        override suspend fun archive(memberId: String) = Unit
    }

    @Test fun blankNameIsRejected() = runBlocking {
        val repository = FakeRepository()
        assertEquals(RegisterMemberResult.InvalidName, RegisterMember(repository)("  ", null, 100L))
        assertEquals(0, repository.saved.size)
    }

    @Test fun normalizedMemberStartsWithZeroBalances() = runBlocking {
        val repository = FakeRepository()
        val result = RegisterMember(repository)("  سارا رضایی  ", " سارا ", 100L)
        val member = assertIs<RegisterMemberResult.Saved>(result).member
        assertEquals("سارا رضایی", member.fullName)
        assertEquals("سارا", member.nickname)
        assertEquals(0L, member.totalXp)
        assertEquals(0L, member.spendablePoints)
        assertEquals(0L, member.diamonds)
        assertEquals(100L, member.createdAtEpochMillis)
    }
}
