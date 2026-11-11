package com.kichikan.negah.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kichikan.negah.domain.settings.HomeShortcut

data class HomeAttendanceDay(
    val label: String,
    val presentCount: Int,
    val recordedCount: Int,
) {
    val presentRatio: Float
        get() = if (recordedCount == 0) 0f else presentCount.toFloat() / recordedCount
}

data class HomeRecentSession(
    val title: String,
    val dateLabel: String,
    val completed: Boolean,
)

data class HomeDashboardState(
    val activeMemberCount: Int = 0,
    val totalXp: Long = 0,
    val totalPoints: Long = 0,
    val totalDiamonds: Long = 0,
    val weekSessionCount: Int = 0,
    val weekRecordedAttendanceCount: Int = 0,
    val attendanceDays: List<HomeAttendanceDay> = emptyList(),
    val recentSessions: List<HomeRecentSession> = emptyList(),
)

@Composable
fun HomeScreen(
    dashboard: HomeDashboardState,
    shortcuts: List<HomeShortcut>,
    onOpenShortcut: (HomeShortcut) -> Unit,
    onOpenWeeklyPulse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("نگاه مربی", style = MaterialTheme.typography.headlineMedium)
            Text(
                "دیدن مسیر رشد، همراهی با هر عضو",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("نبض هفتگی حلقه", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${dashboard.weekSessionCount} جلسه در هفته جاری  •  ${dashboard.weekRecordedAttendanceCount} وضعیت حضور ثبت‌شده",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenWeeklyPulse) {
                    Text("مشاهده جزئیات نبض هفتگی")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("دسترسی سریع", style = MaterialTheme.typography.titleMedium)
            if (shortcuts.isEmpty()) {
                Text(
                    "میان‌بری انتخاب نشده است. از تنظیمات، میان‌برهای خانه را مشخص کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                shortcuts.forEach { shortcut ->
                    Button(
                        onClick = { onOpenShortcut(shortcut) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(shortcut.label)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeWeeklyPulseScreen(
    dashboard: HomeDashboardState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("بازگشت به خانه") }
        Text("نبض هفتگی حلقه", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("نمای کلی حلقه", style = MaterialTheme.typography.titleMedium)
                SummaryValue("اعضای فعال", dashboard.activeMemberCount.toString())
                SummaryValue("XP", dashboard.totalXp.toString())
                SummaryValue("امتیاز قابل‌خرج", dashboard.totalPoints.toString())
                SummaryValue("الماس", dashboard.totalDiamonds.toString())
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("روند حضور در ۷ روز اخیر", style = MaterialTheme.typography.titleMedium)
                if (dashboard.attendanceDays.none { it.recordedCount > 0 }) {
                    Text(
                        "در این بازه هنوز وضعیت حضوری ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    dashboard.attendanceDays.forEach { day ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(day.label, style = MaterialTheme.typography.labelMedium)
                            Box(
                                modifier = Modifier.weight(1f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(day.presentRatio.coerceIn(0f, 1f))
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                        .padding(vertical = 5.dp),
                                )
                            }
                            Text("${day.presentCount} از ${day.recordedCount}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(
                    "حاضرها از مجموع وضعیت‌های ثبت‌شده؛ وضعیت «بدون ثبت» نیز در مخرج شمارش می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("رویدادهای اخیر", style = MaterialTheme.typography.titleMedium)
                if (dashboard.recentSessions.isEmpty()) {
                    Text(
                        "هنوز رویدادی از جلسه‌ها برای نمایش وجود ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    dashboard.recentSessions.forEach { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    session.dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (session.completed) "تکمیل‌شده" else "در انتظار",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (session.completed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
