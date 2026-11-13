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
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    entry: GroupWithMembers,
    names: Map<String, String>,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.group.name, style = MaterialTheme.typography.titleMedium)
            Text("سرگروه: ${names[entry.group.leaderMemberId] ?: "عضو بایگانی‌شده"}")
            Text(
                "زیرگروه: " + entry.subMemberIds.joinToString("، ") { names[it] ?: "عضو بایگانی‌شده" },
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("ویرایش") }
                TextButton(onClick = onArchive) { Text("بایگانی") }
            }
        }
    }
}
