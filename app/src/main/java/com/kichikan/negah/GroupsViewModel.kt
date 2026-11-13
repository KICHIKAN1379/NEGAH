package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch

data class GroupsUiState(
    val members: List<Member> = emptyList(),
    val groups: List<GroupWithMembers> = emptyList(),
    val message: String? = null,
) {
    val activeGroups: List<GroupWithMembers> get() = groups.filter { it.group.isActive }
}

@HiltViewModel
class GroupsViewModel @Inject constructor(
    memberRepository: MemberRepository,
    private val groupRepository: GroupRepository,
    private val saveGroup: SaveGroup,
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

    fun archive(groupId: String) {
        viewModelScope.launch {
            groupRepository.archiveGroup(groupId, System.currentTimeMillis())
            mutableState.update { it.copy(message = "گروه بایگانی شد و اعضایش آزاد شدند") }
        }
    }

    fun clearMessage() = mutableState.update { it.copy(message = null) }
}
