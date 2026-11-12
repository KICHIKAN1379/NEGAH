package com.kichikan.negah.feature.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kichikan.negah.domain.settings.HomeShortcut
import java.time.LocalDate

data class HomeAttendanceDay(val label: String, val presentCount: Int, val recordedCount: Int) {
    val presentRatio: Float get() = if (recordedCount == 0) 0f else presentCount.toFloat() / recordedCount
}
data class HomeRecentSession(val title: String, val dateLabel: String, val completed: Boolean)
data class HomeRecentEvent(val title: String, val detail: String, val timestampLabel: String)
data class HomeDashboardState(
    val activeMemberCount: Int = 0, val totalXp: Long = 0, val totalPoints: Long = 0,
    val totalDiamonds: Long = 0, val weekSessionCount: Int = 0,
    val weekRecordedAttendanceCount: Int = 0, val attendanceDays: List<HomeAttendanceDay> = emptyList(),
    val recentSessions: List<HomeRecentSession> = emptyList(),
    val dailyQuote: String = "",
    val recentEvents: List<HomeRecentEvent> = emptyList(),
)

private fun decodePreview(bytes: ByteArray?, maxDimension: Int) = bytes?.let {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(it, 0, it.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@let null
    var sample = 1
    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
    BitmapFactory.decodeByteArray(it, 0, it.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

@Composable
fun HomeScreen(
    dashboard: HomeDashboardState, shortcuts: List<HomeShortcut>,
    ringName: String, ringUsername: String?, ringImageBytes: ByteArray?, bannerBytes: ByteArray?,
    onOpenShortcut: (HomeShortcut) -> Unit, onOpenWeeklyPulse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringImage = remember(ringImageBytes) { decodePreview(ringImageBytes, 256) }
    val banner = remember(bannerBytes) { decodePreview(bannerBytes, 1280) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(206.dp).clip(RoundedCornerShape(26.dp))) {
            if (banner != null) {
                Image(banner, "بنر حلقه", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.scrim.copy(alpha = .12f),
                        MaterialTheme.colorScheme.scrim.copy(alpha = .78f)))))
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.secondary))))
            }
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (ringImage != null) {
                    Image(ringImage, "تصویر حلقه", Modifier.size(68.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Surface(Modifier.size(68.dp), shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = .22f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("ن", style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val foreground = if (banner == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
                    Text(ringName.ifBlank { "حلقهٔ من" }, style = MaterialTheme.typography.headlineSmall, color = foreground)
                    ringUsername?.takeIf { it.isNotBlank() }?.let {
                        Text("@" + it, style = MaterialTheme.typography.labelMedium, color = foreground.copy(alpha = .86f))
                    }
                    Text("نگاه مربی", style = MaterialTheme.typography.labelMedium, color = foreground.copy(alpha = .86f))
                }
            }
        }

        if (dashboard.dailyQuote.isNotBlank()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("جملهٔ امروز", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(dashboard.dailyQuote, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("نبض هفتگی حلقه", style = MaterialTheme.typography.titleLarge)
                    Text(dashboard.activeMemberCount.toString() + " عضو",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(dashboard.weekSessionCount.toString() + " جلسه در هفته جاری",
                    style = MaterialTheme.typography.bodyLarge)
                Text(dashboard.weekRecordedAttendanceCount.toString() + " وضعیت حضور ثبت‌شده",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onOpenWeeklyPulse, modifier = Modifier.align(Alignment.End)) {
                    Text("مشاهده جزئیات نبض هفتگی")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("دسترسی سریع", style = MaterialTheme.typography.titleLarge)
            if (shortcuts.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text("میان‌بری انتخاب نشده است. از تنظیمات، میان‌برهای خانه را مشخص کنید.",
                        Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else shortcuts.take(HomeShortcut.MAX_SELECTION).forEach { shortcut ->
                Button(onClick = { onOpenShortcut(shortcut) },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(shortcut.label, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
fun HomeWeeklyPulseScreen(dashboard: HomeDashboardState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("بازگشت به خانه") }
        Text("نبض هفتگی حلقه", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("نمای کلی حلقه", style = MaterialTheme.typography.titleMedium)
                SummaryValue("اعضای فعال", dashboard.activeMemberCount.toString())
                SummaryValue("XP", dashboard.totalXp.toString())
                SummaryValue("امتیاز قابل‌خرج", dashboard.totalPoints.toString())
                SummaryValue("الماس", dashboard.totalDiamonds.toString())
                SummaryValue("جلسات هفته", dashboard.weekSessionCount.toString())
                SummaryValue("وضعیت‌های حضور ثبت‌شده", dashboard.weekRecordedAttendanceCount.toString())
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("روند حضور در ۷ روز اخیر", style = MaterialTheme.typography.titleMedium)
                if (dashboard.attendanceDays.none { it.recordedCount > 0 }) {
                    Text("در این بازه هنوز وضعیت حضوری ثبت نشده است.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else dashboard.attendanceDays.forEach { day ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(day.label, style = MaterialTheme.typography.labelMedium)
                        Box(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)).padding(2.dp)) {
                            Box(Modifier.fillMaxWidth(day.presentRatio.coerceIn(0f, 1f))
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).padding(vertical = 5.dp))
                        }
                        Text(day.presentCount.toString() + " از " + day.recordedCount,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("نسبت حاضرها از مجموع وضعیت‌های ثبت‌شده محاسبه می‌شود؛ «بدون ثبت» نیز در مخرج شمارش است.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("آخرین رویدادها", style = MaterialTheme.typography.titleMedium)
                if (dashboard.recentEvents.isEmpty()) {
                    Text("هنوز رویدادی برای نمایش ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else dashboard.recentEvents.take(20).forEachIndexed { index, event ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(event.title, style = MaterialTheme.typography.bodyMedium)
                        Text(event.detail + " · " + event.timestampLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index < dashboard.recentEvents.take(20).lastIndex) HorizontalDivider()
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("جلسات اخیر", style = MaterialTheme.typography.titleMedium)
                if (dashboard.recentSessions.isEmpty()) {
                    Text("هنوز جلسه‌ای برای نمایش وجود ندارد.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else dashboard.recentSessions.forEach { session ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(session.title, style = MaterialTheme.typography.bodyMedium)
                            Text(session.dateLabel, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (session.completed) "تکمیل‌شده" else "در انتظار",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (session.completed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun CoachAssistantScreen(
    dashboard: HomeDashboardState, onBack: () -> Unit,
    onOpenMembers: () -> Unit, onOpenSessions: () -> Unit, modifier: Modifier = Modifier,
) {
    val suggestions = buildList {
        if (dashboard.activeMemberCount == 0) add("هنوز عضوی ثبت نشده است؛ می‌توانید از بخش اعضا، نخستین عضو حلقه را اضافه کنید.")
        if (dashboard.weekSessionCount == 0) add("برای هفتهٔ جاری جلسه‌ای ثبت نشده است؛ برنامهٔ جلسات را بررسی کنید.")
        if (dashboard.attendanceDays.none { it.recordedCount > 0 }) add("در هفت روز اخیر سابقهٔ حضور ثبت‌شده‌ای دیده نمی‌شود.")
        if (isEmpty()) add("اطلاعات ثبت‌شدهٔ فعلی نشانه‌ای برای پیگیری فوری نشان نمی‌دهد. روند اعضا و جلسات را منظم مرور کنید.")
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("بازگشت به خانه") }
        Text("دستیار مربی", style = MaterialTheme.typography.headlineSmall)
        Text("این راهنمای آفلاین فقط از اطلاعات ثبت‌شده روی همین دستگاه استفاده می‌کند.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        suggestions.forEachIndexed { index, suggestion ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text((index + 1).toString() + ".", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(suggestion, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Button(onClick = onOpenMembers, modifier = Modifier.fillMaxWidth()) { Text("رفتن به اعضا") }
        Button(onClick = onOpenSessions, modifier = Modifier.fillMaxWidth()) { Text("رفتن به جلسات") }
    }
}

@Composable
private fun SummaryValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
