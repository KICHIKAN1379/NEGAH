package com.kichikan.negah

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthTrack

@Composable
fun ProgressScreen(viewModel: ProgressViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTrack by rememberSaveable { mutableStateOf(GrowthTrack.CAPABILITY.name) }
    val track = GrowthTrack.valueOf(selectedTrack)
    val items = if (track == GrowthTrack.CAPABILITY) state.capabilityItems else state.selfImprovementItems
    val progressById = state.memberProgress.associateBy { it.growthItemId }
    val selectedMember = state.members.firstOrNull { it.id == state.selectedMemberId }
    val average = if (items.isEmpty()) 0 else items.map { progressById[it.id]?.progressPercent ?: 0 }.average().toInt()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("پیشرفت اعضا", style = MaterialTheme.typography.headlineSmall)
        if (state.members.isEmpty()) {
            Text("برای مشاهده مسیر رشد، ابتدا یک عضو ثبت کنید.")
        } else {
            Text("انتخاب عضو", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.members.forEach { member ->
                    FilterChip(
                        selected = member.id == state.selectedMemberId,
                        onClick = { viewModel.selectMember(member.id) },
                        label = { Text(member.nickname?.takeIf(String::isNotBlank) ?: member.fullName) },
                    )
                }
            }
            Text(selectedMember?.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = track == GrowthTrack.CAPABILITY,
                    onClick = { selectedTrack = GrowthTrack.CAPABILITY.name },
                    label = { Text("توانمندی") },
                )
                FilterChip(
                    selected = track == GrowthTrack.SELF_IMPROVEMENT,
                    onClick = { selectedTrack = GrowthTrack.SELF_IMPROVEMENT.name },
                    label = { Text("خودسازی") },
                )
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("میانگین این مسیر", style = MaterialTheme.typography.titleMedium)
                    Text("$average٪", style = MaterialTheme.typography.headlineMedium)
                    LinearProgressIndicator(
                        progress = { average / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${items.size} شاخص فعال", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (items.isEmpty()) {
                Text("هنوز شاخص فعالی در این مسیر تعریف نشده است. شاخص‌ها را از پالایشگاه مدیریت کنید.")
            } else {
                items.forEach { item ->
                    GrowthProgressCard(item, progressById[item.id]?.progressPercent ?: 0, progressById[item.id]?.note)
                }
            }
        }
    }
}

@Composable
private fun GrowthProgressCard(item: GrowthItem, progress: Int, note: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            item.description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("میزان پیشرفت")
                Text("$progress٪")
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            note?.takeIf(String::isNotBlank)?.let {
                Text("یادداشت مربی: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
