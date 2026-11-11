package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.growth.*
import com.kichikan.negah.domain.member.*
import com.kichikan.negah.domain.mission.*
import com.kichikan.negah.domain.economy.EconomyCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProgressUiState(
 val members: List<Member> = emptyList(), val selectedMemberId: String? = null,
 val capabilityItems: List<GrowthItem> = emptyList(), val selfImprovementItems: List<GrowthItem> = emptyList(),
 val memberProgress: List<MemberGrowthProgress> = emptyList(), val missions: List<Mission> = emptyList(),
 val assignments: List<MissionAssignment> = emptyList(),
)
@HiltViewModel
class ProgressViewModel @Inject constructor(
 private val memberRepository: MemberRepository, private val growthMapRepository: GrowthMapRepository,
 private val missionRepository: MissionRepository,
) : ViewModel() {
 private val mutableState = MutableStateFlow(ProgressUiState())
 val state = mutableState.asStateFlow()
 private var progressJob: Job? = null
 private var progressJobMemberId: String? = null
 private var assignmentsJob: Job? = null
 private var assignmentsMissionId: String? = null
 init {
  viewModelScope.launch {
   memberRepository.observeActiveMembers().collect { members ->
    val selected = mutableState.value.selectedMemberId?.takeIf { id -> members.any { it.id == id } } ?: members.firstOrNull()?.id
    mutableState.update { it.copy(members = members, selectedMemberId = selected) }
    if (selected != null && selected != progressJobMemberId) observeMember(selected)
    if (selected == null) { progressJob?.cancel(); progressJob = null; progressJobMemberId = null; mutableState.update { it.copy(memberProgress = emptyList()) } }
   }
  }
  viewModelScope.launch {
   combine(growthMapRepository.observeItems(GrowthTrack.CAPABILITY), growthMapRepository.observeItems(GrowthTrack.SELF_IMPROVEMENT)) { a,b -> a to b }
    .collect { (a,b) -> mutableState.update { it.copy(capabilityItems=a, selfImprovementItems=b) } }
  }
  viewModelScope.launch { missionRepository.observeMissions().collect { rows -> mutableState.update { it.copy(missions=rows) } } }
 }
 fun selectMember(id: String) {
  if (mutableState.value.members.none { it.id == id }) return
  mutableState.update { it.copy(selectedMemberId=id) }
  if (progressJobMemberId != id) observeMember(id)
 }
 fun selectMission(id: String) {
  if (assignmentsMissionId == id) return
  assignmentsJob?.cancel(); assignmentsMissionId=id
  assignmentsJob=viewModelScope.launch { missionRepository.observeAssignments(id).collect { rows -> mutableState.update { it.copy(assignments=rows) } } }
 }
 fun createMission(kind: MissionKind, title: String, description: String, currency: EconomyCurrency, amount: Long) {
  if (title.isBlank() || amount <= 0) return
  viewModelScope.launch { missionRepository.saveMission(Mission(UUID.randomUUID().toString(),kind,title.trim(),description.trim().ifBlank { null },currency,amount,true,System.currentTimeMillis())) }
 }
 fun assignSelectedMember(missionId: String) {
  val memberId=mutableState.value.selectedMemberId ?: return
  viewModelScope.launch { missionRepository.assignMember(MissionAssignment(UUID.randomUUID().toString(),missionId,memberId,MissionAssignmentStatus.ASSIGNED,null,System.currentTimeMillis())) }
 }
 fun updateMissionProgress(id: String,note: String,completed: Boolean) {
  viewModelScope.launch { missionRepository.updateProgress(id,note,completed,System.currentTimeMillis()) }
 }
 fun approveMission(id: String) { viewModelScope.launch { missionRepository.approveAndReward(id,System.currentTimeMillis()) } }
 private fun observeMember(id: String) {
  progressJob?.cancel(); progressJobMemberId=id
  progressJob=viewModelScope.launch { growthMapRepository.observeProgress(id).collect { rows -> mutableState.update { if(it.selectedMemberId==id) it.copy(memberProgress=rows) else it } } }
 }
}
