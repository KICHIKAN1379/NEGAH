package com.kichikan.negah.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    val attendanceDays: List<HomeAttendanceDay> = emptyList(),
    val recentSessions: List<HomeRecentSession> = emptyList(),
)

@Composable
fun HomeScreen(
    dashboard: HomeDashboardState,
    onOpenMembers: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("نگاه مربی", style = MaterialTheme.typography.headlineMedium)
            Text(
                "دیدن مسیر رشد، همراهی با هر عضو",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اعضای فعال", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "اعضای ثبت‌شده در حلقه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    dashboard.activeMemberCount.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("موجودی جمع اعضا", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    WalletValue("XP", dashboard.totalXp)
                    WalletValue("امتیاز", dashboard.totalPoints)
                    WalletValue("الماس", dashboard.totalDiamonds)
                }
                Text(
                    "سه موجودی مستقل از یکدیگر محاسبه می‌شوند.",
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
                Text("روند حضور در ۷ روز اخیر", style = MaterialTheme.typography.titleMedium)
                if (dashboard.attendanceDays.isEmpty()) {
                    Text(
                        "هنوز داده‌ای برای نمایش وجود ندارد.",
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
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .padding(2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(day.presentRatio.coerceIn(0f, 1f))
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(6.dp),
                                        )
                                        .padding(vertical = 5.dp),
                                )
                            }
                            Text(
                                "${day.presentCount} از ${day.recordedCount}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Text(
                        "تعداد حاضرها از کل وضعیت‌های ثبت‌شده در جلسات هر روز",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("جلسات اخیر", style = MaterialTheme.typography.titleMedium)
                if (dashboard.recentSessions.isEmpty()) {
                    Text(
                        "هنوز جلسه‌ای ثبت نشده است.",
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("دسترسی سریع", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onOpenMembers, modifier = Modifier.fillMaxWidth()) {
                Text("مدیریت اعضا")
            }
            Button(onClick = onOpenProgress, modifier = Modifier.fillMaxWidth()) {
                Text("بخش پیشرفت")
            }
            Button(onClick = onOpenSessions, modifier = Modifier.fillMaxWidth()) {
                Text("جلسات و حضور و غیاب")
            }
        }
    }
}

@Composable
private fun WalletValue(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
