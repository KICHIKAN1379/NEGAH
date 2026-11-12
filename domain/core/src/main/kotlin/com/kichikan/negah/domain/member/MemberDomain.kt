package com.kichikan.negah.domain.member

import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

data class Member(
    val id: String, val fullName: String, val nickname: String?,
    val totalXp: Long, val spendablePoints: Long, val diamonds: Long,
    val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long, val isArchived: Boolean,
    val birthDateJalali: String? = null,
    val activeBadgeLevel: Int? = null,
)

interface MemberRepository {
    fun observeActiveMembers(): Flow<List<Member>>
    fun observeArchivedMembers(): Flow<List<Member>>
    /** Creates a new member; existing wallet balances must never be overwritten here. */
    suspend fun create(member: Member)
    suspend fun updateBirthDate(memberId: String, birthDateJalali: String?, updatedAtEpochMillis: Long) {
        throw UnsupportedOperationException("Birth date editing is not supported by this repository")
    }
    suspend fun updateProfile(memberId: String, fullName: String, nickname: String?, birthDateJalali: String?, updatedAtEpochMillis: Long) {
        throw UnsupportedOperationException("Profile editing is not supported by this repository")
    }
    suspend fun updateActiveBadgeLevel(memberId: String, level: Int?, updatedAtEpochMillis: Long) {
        throw UnsupportedOperationException("Badge selection is not supported by this repository")
    }
    suspend fun archive(memberId: String)
    suspend fun restore(memberId: String)
}

sealed interface RegisterMemberResult {
    data class Saved(val member: Member) : RegisterMemberResult
    data object InvalidName : RegisterMemberResult
}

class RegisterMember @Inject constructor(private val repository: MemberRepository) {
    suspend operator fun invoke(fullName: String, nickname: String?, nowEpochMillis: Long, birthDateJalali: String? = null): RegisterMemberResult {
        val name = fullName.trim()
        if (name.isBlank()) return RegisterMemberResult.InvalidName
        val member = Member(
            id = UUID.randomUUID().toString(), fullName = name,
            nickname = nickname?.trim()?.takeIf(String::isNotEmpty),
            totalXp = 0L, spendablePoints = 0L, diamonds = 0L,
            createdAtEpochMillis = nowEpochMillis, updatedAtEpochMillis = nowEpochMillis,
            birthDateJalali = birthDateJalali,
            isArchived = false,
        )
        repository.create(member)
        return RegisterMemberResult.Saved(member)
    }
}
