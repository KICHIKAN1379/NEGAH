package com.kichikan.negah

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DailyQuotesScreen(viewModel: DailyQuotesViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("بازگشت به تنظیمات") }
        Text("جمله‌های روز", style = MaterialTheme.typography.headlineSmall)
        Text("جمله‌های فعال به نوبت و بر اساس روز نمایش داده می‌شوند. در نبود جملهٔ اختصاصی، متن‌های پیش‌فرض خانه نمایش داده می‌شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(state.draft, viewModel::setDraft, modifier = Modifier.fillMaxWidth(), label = { Text("متن جمله") }, minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::save) { Text(if (state.editingId == null) "افزودن جمله" else "ذخیره ویرایش") }
            if (state.editingId != null) TextButton(onClick = viewModel::cancelEdit) { Text("لغو ویرایش") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.isLoading) Text("در حال بارگذاری...")
        else if (state.quotes.isEmpty()) Text("هنوز جملهٔ اختصاصی ثبت نشده است.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.quotes, key = { it.id }) { quote ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(quote.text, style = MaterialTheme.typography.bodyLarge)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Switch(quote.isActive, onCheckedChange = { viewModel.toggle(quote, it) }); Text(if (quote.isActive) "فعال" else "غیرفعال") }
                            Row { TextButton(onClick = { viewModel.edit(quote) }) { Text("ویرایش") }; TextButton(onClick = { viewModel.delete(quote) }) { Text("حذف") } }
                        }
                    }
                }
            }
        }
    }
}