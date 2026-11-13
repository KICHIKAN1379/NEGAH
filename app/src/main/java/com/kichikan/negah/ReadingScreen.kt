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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.member.JalaliDateText
import com.kichikan.negah.domain.reading.ReadingLevels
import com.kichikan.negah.domain.reading.ReadingScoreCalculator
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var memberId by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf(3) }
    var date by rememberSaveable { mutableStateOf(JalaliDateText.format(LocalDate.now())) }
    val memberNames = remember(state.members) {
        state.members.associate { it.id to (it.nickname?.takeIf(String::isNotBlank) ?: it.fullName) }
    }
    val ranking = remember(state.readings) { ReadingScoreCalculator.ranking(state.readings) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("کتاب‌خوانی", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("بازگشت") }
        }

        Text("رتبه‌بندی بر اساس سطح مطالعه", style = MaterialTheme.typography.titleMedium)
        if (ranking.isEmpty()) {
            Text("هنوز کتابی ثبت نشده است.")
        } else {
            ranking.forEachIndexed { index, entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${index + 1}. ${memberNames[entry.memberId] ?: "عضو بایگانی‌شده"}")
                        Text("${entry.score} امتیاز · ${entry.bookCount} کتاب")
                    }
                }
            }
        }

        Text("ثبت کتاب", style = MaterialTheme.typography.titleMedium)
        if (state.members.isEmpty()) {
            Text("برای ثبت کتاب، ابتدا یک عضو ثبت کنید.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.members.forEach { member ->
                    FilterChip(
                        selected = member.id == memberId,
                        onClick = { memberId = member.id },
                        label = { Text(memberNames[member.id].orEmpty()) },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; viewModel.clearMessage() },
                label = { Text("عنوان کتاب") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = date,
                onValueChange = { date = it; viewModel.clearMessage() },
                label = { Text("تاریخ (شمسی، مثل ۱۴۰۵/۰۸/۲۱)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("سطح کتاب (۱ تا ۵)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (ReadingLevels.MIN..ReadingLevels.MAX).forEach { value ->
                    FilterChip(
                        selected = level == value,
                        onClick = { level = value },
                        label = { Text(value.toString()) },
                    )
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        if (viewModel.record(memberId, title, level, date)) title = ""
                    }
                },
            ) { Text("ثبت کتاب") }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        Text("آخرین کتاب‌های ثبت‌شده", style = MaterialTheme.typography.titleMedium)
        if (state.readings.isEmpty()) {
            Text("موردی برای نمایش نیست.")
        } else {
            state.readings.take(15).forEach { reading ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(reading.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${memberNames[reading.memberId] ?: "عضو بایگانی‌شده"} · سطح ${reading.level} · " +
                                JalaliDateText.format(
                                    java.time.Instant.ofEpochMilli(reading.readAtEpochMillis)
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                                ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
