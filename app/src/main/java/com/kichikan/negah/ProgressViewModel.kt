package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthMapRepository
import com.kichikan.negah.domain.growth.GrowthTrack
import com.kichikan.negah.domain.growth.MemberGrowthProgress
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgressUiState(
    val members: List<Member> = emptyList(),
    val selectedMemberId: String? = null,
    val capabilityItems: List<GrowthItem> = emptyList(),
    val selfImprovementItems: List<GrowthItem> = emptyList(),
    val memberProgress: List<MemberGrowthProgress> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val growthMapRepository: GrowthMapRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProgressUiState())
    val state = mutableState.asStateFlow()
    private var progressJob: Job? = null
    private var progressJobMemberId: String? = null

    init {
        viewModelScope.launch {
            memberRepository.observeActiveMembers().collect { members ->
                val selected = mutableState.value.selectedMemberId
                    ?.takeIf { id -> members.any { it.id == id } }
                    ?: members.firstOrNull()?.id
                mutableState.update { it.copy(members = members, selectedMemberId = selected) }
                if (selected != null && selected != progressJobMemberId) observeMember(selected)
                if (selected == null) {
                    progressJob?.cancel()
                    progressJob = null
                    progressJobMemberId = null
                    mutableState.update { it.copy(memberProgress = emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            combine(
                growthMapRepository.observeItems(GrowthTrack.CAPABILITY),
                growthMapRepository.observeItems(GrowthTrack.SELF_IMPROVEMENT),
            ) { capability, selfImprovement -> capability to selfImprovement }
                .collect { (capability, selfImprovement) ->
                    mutableState.update {
                        it.copy(capabilityItems = capability, selfImprovementItems = selfImprovement)
                    }
                }
        }
    }

    fun selectMember(memberId: String) {
        if (mutableState.value.members.none { it.id == memberId }) return
        mutableState.update { it.copy(selectedMemberId = memberId) }
        if (progressJobMemberId != memberId) observeMember(memberId)
    }

    private fun observeMember(memberId: String) {
        progressJob?.cancel()
        progressJobMemberId = memberId
        progressJob = viewModelScope.launch {
            growthMapRepository.observeProgress(memberId).collect { progress ->
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(memberProgress = progress)
                    else current
                }
            }
        }
    }
}
