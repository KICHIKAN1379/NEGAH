package com.kichikan.negah

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.group.GroupWithMembers
import kotlinx.coroutines.launch

/** «گروه» tab of the members area: one leader, the rest of the picked members are the sub-group. */
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var leaderId by remember { mutableStateOf<String?>(null) }
    val subIds = remember { emptyList<String>().toMutableStateList() }
    var formOpen by remember { mutableStateOf(false) }
    var economyGroupId by remember { mutableStateOf<String?>(null) }
    var historyGroupId by remember { mutableStateOf<String?>(null) }
    var pointsDelta by remember { mutableStateOf("") }
    var diamondsDelta by remember { mutableStateOf("") }
    var economyReason by remember { mutableStateOf("") }

    val displayNames = remember(state.members) {
        state.members.associate { it.id to (it.nickname?.takeIf(String::isNotBlank) ?: it.fullName) }
    }
    val takenElsewhere = remember(state.groups, editingId) {
        state.activeGroups.filter { it.group.id != editingId }.flatMap { it.allMemberIds }.toSet()
    }
    val selectable = state.members.filter { it.id !in takenElsewhere }

    fun resetForm() {
        editingId = null; name = ""; leaderId = null; subIds.clear(); formOpen = false
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("گروه‌ها", style = MaterialTheme.typography.headlineSmall)
        Text(
            "هر گروه یک سرگروه دارد و بقیه اعضای انتخاب‌شده زیرگروه او هستند. هر عضو فقط در یک گروه فعال می‌تواند باشد.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (!formOpen) {
            Button(onClick = { formOpen = true; viewModel.clearMessage() }, modifier = Modifier.fillMaxWidth()) { Text("گروه جدید") }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (editingId == null) "گروه جدید" else "ویرایش گروه", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; viewModel.clearMessage() },
                        label = { Text("نام گروه") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("سرگروه")
                    if (selectable.isEmpty()) {
                        Text("عضو آزادی برای انتخاب وجود ندارد؛ ابتدا عضو ثبت کنید یا گروهی را بایگانی کنید.")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectable.forEach { member ->
                            FilterChip(
                                selected = leaderId == member.id,
                                onClick = { leaderId = member.id; subIds.remove(member.id) },
                                label = { Text(displayNames[member.id].orEmpty()) },
                            )
                        }
                    }
                    Text("زیرگروه (با تیک انتخاب کنید)")
                    selectable.filter { it.id != leaderId }.forEach { member ->
                        val checked = member.id in subIds
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (checked) subIds.remove(member.id) else subIds.add(member.id)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                if (it) { if (member.id !in subIds) subIds.add(member.id) } else subIds.remove(member.id)
                            })
                            Text(displayNames[member.id].orEmpty())
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                if (viewModel.save(editingId, name, leaderId, subIds.toList())) resetForm()
                            }
                        }) { Text("ذخیره گروه") }
                        TextButton(onClick = { resetForm(); viewModel.clearMessage() }) { Text("انصراف") }
                    }
                }
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        if (state.activeGroups.isEmpty()) {
            Text("هنوز گروهی ساخته نشده است.")
        } else {
            state.activeGroups.forEach { entry ->
                GroupCard(
                    entry = entry,
                    names = displayNames,
                    onEdit = {
                        editingId = entry.group.id
                        name = entry.group.name
                        leaderId = entry.group.leaderMemberId
                        subIds.clear(); subIds.addAll(entry.subMemberIds)
                        formOpen = true
                        viewModel.clearMessage()
                    },
                    onArchive = { viewModel.archive(entry.group.id) },
                    onAdjustEconomy = {
                        economyGroupId = entry.group.id
                        pointsDelta = ""
                        diamondsDelta = ""
                        economyReason = ""
                        viewModel.clearMessage()
                    },
                    onShowHistory = {
                        historyGroupId = entry.group.id
                        viewModel.loadEconomyHistory(entry.group.id)
                    },
                )
            }
        }
    }


    economyGroupId?.let { groupId ->
        val group = state.groups.firstOrNull { it.group.id == groupId }?.group
        AlertDialog(
            onDismissRequest = { economyGroupId = null; viewModel.clearMessage() },
            title = { Text("تغییر موجودی ${group?.name.orEmpty()}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("موجودی فعلی: ${group?.spendablePoints ?: 0L} امتیاز · ${group?.diamonds ?: 0L} الماس")
                    OutlinedTextField(
                        value = pointsDelta,
                        onValueChange = { pointsDelta = it.filter { ch -> ch == '-' || ch.isDigit() } },
                        label = { Text("تغییر امتیاز گروه (+ یا -)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = diamondsDelta,
                        onValueChange = { diamondsDelta = it.filter { ch -> ch == '-' || ch.isDigit() } },
                        label = { Text("تغییر الماس گروه (+ یا -)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = economyReason,
                        onValueChange = { economyReason = it },
                        label = { Text("دلیل تغییر (الزامی)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val points = pointsDelta.toLongOrNull() ?: 0L
                    val diamonds = diamondsDelta.toLongOrNull() ?: 0L
                    scope.launch {
                        if (viewModel.adjustEconomy(groupId, points, diamonds, economyReason)) {
                            economyGroupId = null
                            pointsDelta = ""
                            diamondsDelta = ""
                            economyReason = ""
                        }
                    }
                }) { Text("ثبت تغییر") }
            },
            dismissButton = {
                TextButton(onClick = { economyGroupId = null; viewModel.clearMessage() }) { Text("انصراف") }
            },
        )
    }

    historyGroupId?.let { groupId ->
        val group = state.groups.firstOrNull { it.group.id == groupId }?.group
        AlertDialog(
            onDismissRequest = { historyGroupId = null; viewModel.clearEconomyHistory() },
            title = { Text("سوابق اقتصاد ${group?.name.orEmpty()}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.economyHistory.isEmpty()) {
                        Text("هنوز تغییری برای این گروه ثبت نشده است.")
                    } else {
                        state.economyHistory.forEach { transaction ->
                            val currency = if (transaction.currency.name == "POINTS") "امتیاز" else "الماس"
                            Text(
                                "${if (transaction.amountDelta > 0) "+" else ""}${transaction.amountDelta} $currency · ${transaction.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyGroupId = null; viewModel.clearEconomyHistory() }) { Text("بستن") }
            },
        )
    }

}

@Composable
private fun GroupCard(
    entry: GroupWithMembers,
    names: Map<String, String>,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onAdjustEconomy: () -> Unit,
    onShowHistory: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.group.name, style = MaterialTheme.typography.titleMedium)
            Text("موجودی مستقل: ${entry.group.spendablePoints} امتیاز · ${entry.group.diamonds} الماس")
            Text("سرگروه: ${names[entry.group.leaderMemberId] ?: "عضو بایگانی‌شده"}")
            Text(
                "زیرگروه: " + entry.subMemberIds.joinToString("، ") { names[it] ?: "عضو بایگانی‌شده" },
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("ویرایش") }
                TextButton(onClick = onAdjustEconomy) { Text("تغییر موجودی") }
                TextButton(onClick = onShowHistory) { Text("سوابق") }
                TextButton(onClick = onArchive) { Text("بایگانی") }
            }
        }
    }
}
