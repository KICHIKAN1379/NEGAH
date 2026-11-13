package com.kichikan.negah

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The six entries of «پیشرفت», in the approved order. */
enum class ProgressEntry(val title: String, val subtitle: String) {
    IDENTITY("شناسنامه", "سطح، XP و مسیر رشد عضو"),
    MISSIONS("مأموریت", "مأموریت، مأموریت ویژه و پازل گروهی"),
    READING("کتاب‌خوانی", "ثبت کتاب و رتبه‌بندی بر اساس سطح مطالعه"),
    WHEEL("گردونه", "هزینه با امتیاز قابل‌خرج"),
    COMPETITION("رقابت", "رتبه اعضا و رتبه گروه‌ها"),
    SESSIONS("جلسات", "فهرست جلسات و حضور و غیاب"),
}

@Composable
fun ProgressHubScreen(
    onOpen: (ProgressEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("پیشرفت", style = MaterialTheme.typography.headlineSmall)
        ProgressEntry.entries.forEach { entry ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(entry) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                    Text(entry.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun WheelPendingScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("گردونه", style = MaterialTheme.typography.headlineSmall)
        Text("گردونه هنوز پیاده‌سازی نشده است. جوایز، وزن‌ها و هزینه آن از پالایشگاه تنظیم خواهد شد و فقط با داده واقعی کار می‌کند.")
        androidx.compose.material3.TextButton(onClick = onBack) { Text("بازگشت") }
    }
}
