package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.group.AdjustGroupEconomy
import com.kichikan.negah.domain.group.AdjustGroupEconomyResult
import com.kichikan.negah.domain.group.GroupEconomyTransaction
import com.kichikan.negah.domain.group.GroupRepository
import com.kichikan.negah.domain.group.GroupWithMembers
import com.kichikan.negah.domain.group.SaveGroup
import com.kichikan.negah.domain.group.SaveGroupResult
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GroupsUiState(
    val members: List<Member> = emptyList(),
    val groups: List<GroupWithMembers> = emptyList(),
    val message: String? = null,
    val economyHistory: List<GroupEconomyTransaction> = emptyList(),
) {
    val activeGroups: List<GroupWithMembers> get() = groups.filter { it.group.isActive }
}

@HiltViewModel
class GroupsViewModel @Inject constructor(
    memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val saveGroup: SaveGroup,
    private val adjustGroupEconomy: AdjustGroupEconomy,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GroupsUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            memberRepository.observeActiveMembers().collect { members ->
                mutableState.update { it.copy(members = members) }
            }
        }
        viewModelScope.launch {
            groupRepository.observeGroups().collect { groups ->
                mutableState.update { it.copy(groups = groups) }
            }
        }
    }

    /** Returns true when saved so the form can reset itself. */
    suspend fun save(groupId: String?, name: String, leaderMemberId: String?, subMemberIds: Collection<String>): Boolean {
        val existing = groupId?.let { id -> mutableState.value.groups.firstOrNull { it.group.id == id } }
        val result = saveGroup(
            groupId = groupId,
            name = name,
            leaderMemberId = leaderMemberId,
            subMemberIds = subMemberIds,
            nowEpochMillis = System.currentTimeMillis(),
            createdAtEpochMillis = existing?.group?.createdAtEpochMillis ?: System.currentTimeMillis(),
        )
        val message = when (result) {
            is SaveGroupResult.Saved -> "گروه ذخیره شد"
            SaveGroupResult.InvalidName -> "نام گروه را وارد کنید"
            SaveGroupResult.MissingLeader -> "سرگروه را انتخاب کنید"
            SaveGroupResult.NoMembers -> "حداقل یک زیرگروه انتخاب کنید"
            SaveGroupResult.MemberAlreadyInAnotherGroup -> "یکی از اعضای انتخاب‌شده در گروه دیگری است"
        }
        mutableState.update { it.copy(message = message) }
        return result is SaveGroupResult.Saved
    }

    suspend fun adjustEconomy(
        groupId: String,
        pointsDelta: Long,
        diamondsDelta: Long,
        reason: String,
    ): Boolean {
        val result = adjustGroupEconomy(
            groupId = groupId,
            pointsDelta = pointsDelta,
            diamondsDelta = diamondsDelta,
            reason = reason,
            nowEpochMillis = System.currentTimeMillis(),
        )
        val message = when (result) {
            AdjustGroupEconomyResult.InvalidAdjustment -> "مقدار تغییر یا دلیل معتبر نیست"
            AdjustGroupEconomyResult.Appended -> "تغییر موجودی گروه ثبت شد"
            AdjustGroupEconomyResult.GroupNotFound -> "گروه پیدا نشد"
            AdjustGroupEconomyResult.InsufficientBalance -> "موجودی گروه برای این کاهش کافی نیست"
            AdjustGroupEconomyResult.BalanceOverflow -> "مقدار موجودی از محدوده مجاز خارج می‌شود"
        }
        mutableState.update { it.copy(message = message) }
        return result is AdjustGroupEconomyResult.Appended
    }

    fun loadEconomyHistory(groupId: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(economyHistory = emptyList()) }
            val history = groupRepository.observeEconomyTransactions(groupId).first()
            mutableState.update { it.copy(economyHistory = history) }
        }
    }

    fun clearEconomyHistory() = mutableState.update { it.copy(economyHistory = emptyList()) }

    fun archive(groupId: String) {
        viewModelScope.launch {
            groupRepository.archiveGroup(groupId, System.currentTimeMillis())
            mutableState.update { it.copy(message = "گروه بایگانی شد و اعضایش آزاد شدند") }
        }
    }

    fun clearMessage() = mutableState.update { it.copy(message = null) }
}
