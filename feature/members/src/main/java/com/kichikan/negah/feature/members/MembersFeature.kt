package com.kichikan.negah.feature.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        MemberProfileScreen(selectedMember, state.economyTransactions, viewModel::onProfileClosed, modifier)
        return
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val totalXp = state.members.sumOf { it.totalXp }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("اعضا", style = MaterialTheme.typography.headlineMedium)
                Text("پرونده و مسیر رشد اعضای حلقه", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${state.members.size} عضو", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }

        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.members.size.toString(), style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("عضو", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(totalXp.toString(), style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Text("مجموع XP", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        OutlinedTextField(value = state.searchQuery, onValueChange = viewModel::onSearchChanged,
            label = { Text("جست‌وجوی نام عضو") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemberSortOption.entries.forEach { option ->
                FilterChip(selected = state.sortOption == option,
                    onClick = { viewModel.onSortChanged(option) },
                    label = { Text(when (option) {
                        MemberSortOption.LEVEL -> "رتبه و سطح"
                        MemberSortOption.POINTS -> "امتیاز قابل خرج"
                        MemberSortOption.NAME -> "نام"
                    }) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("فهرست اعضا", style = MaterialTheme.typography.titleMedium)
            Text("${state.visibleMembers.size} نتیجه", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.visibleMembers.isEmpty()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.members.isEmpty()) "هنوز عضوی ثبت نشده است" else "عضوی با این جست‌وجو پیدا نشد",
                    style = MaterialTheme.typography.titleMedium)
                Text("برای شروع، عضو جدیدی به حلقه اضافه کن", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(state.visibleMembers, key = { it.id }) { member ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.onMemberSelected(member.id) }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(member.fullName.trim().firstOrNull()?.toString() ?: "؟",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                                member.nickname?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("سطح ${LevelProgression.levelFor(member.totalXp)}  •  ${member.totalXp} XP",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("${member.spendablePoints} امتیاز", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary)
                                    Text("${member.diamonds} الماس", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Text("‹", style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("افزودن عضو  +")
        }
        state.savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall) }
        state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("افزودن عضو") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = state.fullName, onValueChange = viewModel::onFullNameChanged,
                        label = { Text("نام و نام خانوادگی") }, singleLine = true)
                    OutlinedTextField(value = state.nickname, onValueChange = viewModel::onNicknameChanged,
                        label = { Text("نام نمایشی (اختیاری)") }, singleLine = true)
                    state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addMember(); showAddDialog = false }, enabled = !state.isSaving) {
                    Text(if (state.isSaving) "در حال ثبت..." else "ثبت عضو")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("انصراف") } },
        )
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
