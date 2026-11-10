package com.kichikan.negah.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
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
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyRepository
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.economy.EconomyTransactionKind
import com.kichikan.negah.domain.economy.LevelProgression
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.member.RegisterMember
import com.kichikan.negah.domain.member.RegisterMemberResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

enum class MemberSortOption { NAME, LEVEL, POINTS }

data class MembersUiState(
    val members: List<Member> = emptyList(),
    val fullName: String = "",
    val nickname: String = "",
    val searchQuery: String = "",
    val sortOption: MemberSortOption = MemberSortOption.NAME,
    val selectedMemberId: String? = null,
    val economyTransactions: List<EconomyTransaction> = emptyList(),
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
                MemberSortOption.NAME -> filtered.sortedWith(
                    compareBy<Member> { it.fullName.lowercase() }.thenBy { it.id },
                )
                MemberSortOption.LEVEL -> filtered.sortedWith(
                    compareByDescending<Member> { LevelProgression.levelFor(it.totalXp) }
                        .thenByDescending { it.totalXp }
                        .thenBy { it.fullName.lowercase() },
                )
                MemberSortOption.POINTS -> filtered.sortedWith(
                    compareByDescending<Member> { it.spendablePoints }
                        .thenBy { it.fullName.lowercase() },
                )
            }
        }
}

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val repository: MemberRepository,
    private val registerMember: RegisterMember,
    private val economyRepository: EconomyRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MembersUiState())
    val state = mutableState.asStateFlow()
    private var economyHistoryJob: Job? = null

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
    fun onMemberSelected(memberId: String) {
        economyHistoryJob?.cancel()
        mutableState.update { it.copy(selectedMemberId = memberId, economyTransactions = emptyList()) }
        economyHistoryJob = viewModelScope.launch {
            economyRepository.observeForMember(memberId).collect { transactions ->
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(economyTransactions = transactions)
                    else current
                }
            }
        }
    }

    fun onProfileClosed() {
        economyHistoryJob?.cancel()
        economyHistoryJob = null
        mutableState.update { it.copy(selectedMemberId = null, economyTransactions = emptyList()) }
    }

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
    val selectedMember = state.members.firstOrNull { it.id == state.selectedMemberId }
    if (selectedMember != null) {
        MemberProfileScreen(
            member = selectedMember,
            transactions = state.economyTransactions,
            onBack = viewModel::onProfileClosed,
            modifier = modifier,
        )
        return
    }

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
                    Text(when (option) {
                        MemberSortOption.NAME -> "نام"
                        MemberSortOption.LEVEL -> "سطح"
                        MemberSortOption.POINTS -> "امتیاز"
                    })
                }
            }
        }
        Text("${state.visibleMembers.size} عضو", style = MaterialTheme.typography.labelLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.visibleMembers, key = { it.id }) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onMemberSelected(member.id) },
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                            member.nickname?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("${member.spendablePoints} امتیاز قابل خرج",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("سطح ${LevelProgression.levelFor(member.totalXp)}")
                            Text("${member.totalXp} XP", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberProfileScreen(
    member: Member,
    transactions: List<EconomyTransaction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = onBack) { Text("بازگشت به اعضا") }
        Text(member.fullName, style = MaterialTheme.typography.headlineMedium)
        member.nickname?.let {
            Text(it, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("وضعیت پیشرفت", style = MaterialTheme.typography.titleMedium)
                ProfileValue("سطح", LevelProgression.levelFor(member.totalXp).toString())
                ProfileValue("تجربه", "${member.totalXp} XP")
                ProfileValue("امتیاز قابل خرج", member.spendablePoints.toString())
                ProfileValue("الماس", member.diamonds.toString())
            }
        }
        Text("تاریخچه اقتصادی", style = MaterialTheme.typography.titleLarge)
        if (transactions.isEmpty()) {
            Text(
                "هنوز تراکنش اقتصادی برای این عضو ثبت نشده است",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    EconomyTransactionRow(transaction)
                }
            }
        }
    }
}

@Composable
private fun EconomyTransactionRow(transaction: EconomyTransaction) {
    val currencyLabel = when (transaction.currency) {
        EconomyCurrency.XP -> "XP"
        EconomyCurrency.POINTS -> "امتیاز"
        EconomyCurrency.DIAMONDS -> "الماس"
    }
    val kindLabel = when (transaction.kind) {
        EconomyTransactionKind.EARN -> "دریافت"
        EconomyTransactionKind.SPEND -> "خرج"
        EconomyTransactionKind.CORRECTION -> "اصلاح"
        EconomyTransactionKind.REFUND -> "بازگشت"
    }
    val signedAmount = if (transaction.amountDelta > 0) {
        "+${transaction.amountDelta}"
    } else transaction.amountDelta.toString()
    val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(transaction.createdAtEpochMillis))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$kindLabel $currencyLabel", style = MaterialTheme.typography.titleSmall)
                Text(timestamp, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                signedAmount,
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.amountDelta < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
