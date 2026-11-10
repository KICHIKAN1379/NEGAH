package com.kichikan.negah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.settings.ClassDay

@Composable
fun ClassDaysScreen(
    viewModel: ClassDaysViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("بازگشت") }
            Text("روزهای کلاس", style = MaterialTheme.typography.headlineSmall)
        }
        Text("روزهای برگزاری حلقه را انتخاب کنید. تقویم جلسات بر اساس این انتخاب ساخته می‌شود.")

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            ClassDay.entries.sortedBy { it.order }.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = day in state.selectedDays,
                        onCheckedChange = { viewModel.toggle(day) },
                    )
                    Text(day.label)
                }
            }
            if (state.selectedDays.isEmpty()) {
                Text(
                    "هنوز روزی انتخاب نشده است؛ تا زمان انتخاب روزها، تقویم جلسات خالی خواهد بود.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.messageIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = viewModel::save,
            enabled = !state.isLoading && !state.isSaving && state.hasUnsavedChanges,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) "در حال ذخیره..." else "ذخیره")
        }
    }
}
