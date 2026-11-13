package com.kichikan.negah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.economy.LevelProgression

/** Ranking inside the coach's own ring only; ties are broken by name for a stable order. */
@Composable
fun CompetitionScreen(
    viewModel: ProgressViewModel,
    groupsViewModel: GroupsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val groupsState by groupsViewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("MEMBERS") }
    val ranked = remember(state.members) {
        state.members.sortedWith(compareByDescending<com.kichikan.negah.domain.member.Member> { it.totalXp }.thenBy { it.fullName })
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("رقابت", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("بازگشت") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == "MEMBERS", onClick = { tab = "MEMBERS" }, label = { Text("رتبه اعضا") })
            FilterChip(selected = tab == "GROUPS", onClick = { tab = "GROUPS" }, label = { Text("رتبه گروه‌ها") })
        }
        if (tab == "GROUPS") {
            val activeGroups = groupsState.groups.filter { it.group.isActive }
            if (activeGroups.isEmpty()) {
                Text("هنوز گروهی ساخته نشده است. گروه‌ها را از «اعضا ← گروه» بسازید.")
            } else {
                Text(
                    "گروه‌ها ثبت شده‌اند؛ معیار و فرمول رتبه‌بندی گروه‌ها هنوز تأیید نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                )
                activeGroups.forEach { entry ->
                    Text(entry.group.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else if (ranked.isEmpty()) {
            Text("هنوز عضوی ثبت نشده است.")
        } else {
            Text("ترتیب بر اساس XP (پیشرفت دائمی)؛ امتیاز قابل‌خرج در رتبه اثری ندارد.", style = MaterialTheme.typography.bodySmall)
            ranked.take(3).forEachIndexed { index, member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${index + 1}. ${member.nickname?.takeIf(String::isNotBlank) ?: member.fullName}", style = MaterialTheme.typography.titleMedium)
                        Text("سطح ${LevelProgression.levelFor(member.totalXp)} · ${member.totalXp} XP")
                    }
                }
            }
            ranked.drop(3).forEachIndexed { index, member ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 4}. ${member.nickname?.takeIf(String::isNotBlank) ?: member.fullName}")
                    Text("سطح ${LevelProgression.levelFor(member.totalXp)} · ${member.totalXp} XP")
                }
            }
        }
    }
}
