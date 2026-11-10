package com.kichikan.negah.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.member.RegisterMember
import com.kichikan.negah.domain.member.RegisterMemberResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MemberSortOption { NAME, XP }

data class MembersUiState(
    val members: List<Member> = emptyList(),
    val fullName: String = "",
    val nickname: String = "",
    val searchQuery: String = "",
    val sortOption: MemberSortOption = MemberSortOption.NAME,
    val isSaving: Boolean = false,
    val validationMessage: String? = null,
    val savedMessage: String? = null,
) {
    val visibleMembers: List<Member>
        get() {
            val filtered = members.filter {
                searchQuery.isBlank() || it.fullName.contains(searchQuery.trim(), true) ||
                    it.nickname.orEmpty().contains(searchQuery.trim(), true)
            }
            return when (sortOption) {
                MemberSortOption.NAME -> filtered.sortedBy { it.fullName.lowercase() }
                MemberSortOption.XP -> filtered.sortedByDescending { it.totalXp }
            }
        }
}

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val repository: MemberRepository,
    private val registerMember: RegisterMember,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MembersUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeActiveMembers().collect { members ->
                mutableState.update { it.copy(members = members) }
            }
        }
    }

    fun onFullNameChanged(value: String) = mutableState.update {
        it.copy(fullName = value, validationMessage = null, savedMessage = null)
    }
    fun onNicknameChanged(value: String) = mutableState.update { it.copy(nickname = value, savedMessage = null) }
    fun onSearchChanged(value: String) = mutableState.update { it.copy(searchQuery = value) }
    fun onSortChanged(value: MemberSortOption) = mutableState.update { it.copy(sortOption = value) }

    fun addMember() {
        val snapshot = mutableState.value
        if (snapshot.isSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, validationMessage = null, savedMessage = null) }
            try {
                when (registerMember(snapshot.fullName, snapshot.nickname, System.currentTimeMillis())) {
                    RegisterMemberResult.InvalidName -> mutableState.update {
                        it.copy(isSaving = false, validationMessage = "نام و نام خانوادگی را وارد کنید")
                    }
                    is RegisterMemberResult.Saved -> mutableState.update {
                        it.copy(fullName = "", nickname = "", isSaving = false, savedMessage = "عضو جدید ثبت شد")
                    }
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(isSaving = false, validationMessage = "ثبت عضو انجام نشد؛ دوباره تلاش کنید")
                }
            }
        }
    }
}

@Composable
fun MembersScreen(viewModel: MembersViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("اعضا", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(value = state.fullName, onValueChange = viewModel::onFullNameChanged,
            label = { Text("نام و نام خانوادگی") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = state.nickname, onValueChange = viewModel::onNicknameChanged,
            label = { Text("نام نمایشی (اختیاری)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = viewModel::addMember, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSaving) "در حال ثبت..." else "ثبت عضو")
        }
        state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        OutlinedTextField(value = state.searchQuery, onValueChange = viewModel::onSearchChanged,
            label = { Text("جست‌وجوی نام عضو") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MemberSortOption.entries.forEachIndexed { index, option ->
                SegmentedButton(selected = state.sortOption == option,
                    onClick = { viewModel.onSortChanged(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, MemberSortOption.entries.size)) {
                    Text(if (option == MemberSortOption.NAME) "نام" else "XP")
                }
            }
        }
        Text("${state.visibleMembers.size} عضو", style = MaterialTheme.typography.labelLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.visibleMembers, key = { it.id }) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                            member.nickname?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Column {
                            Text("سطح ${com.kichikan.negah.domain.economy.LevelProgression.levelFor(member.totalXp)}")
                            Text("${member.totalXp} XP", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
