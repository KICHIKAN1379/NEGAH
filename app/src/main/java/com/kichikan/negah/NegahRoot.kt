package com.kichikan.negah

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.attendance.AttendanceStatus
import com.kichikan.negah.domain.session.Session
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.settings.DailyQuote
import com.kichikan.negah.domain.settings.HomeShortcut
import com.kichikan.negah.feature.home.HomeAttendanceDay
import com.kichikan.negah.feature.home.CoachAssistantScreen
import com.kichikan.negah.feature.home.HomeDashboardState
import com.kichikan.negah.feature.home.HomeRecentSession
import com.kichikan.negah.feature.home.HomeScreen
import com.kichikan.negah.feature.home.HomeWeeklyPulseScreen
import com.kichikan.negah.feature.members.MembersScreen
import com.kichikan.negah.feature.members.MembersViewModel
import com.kichikan.negah.feature.refinery.RefineryScreen
import com.kichikan.negah.feature.refinery.RefineryViewModel
import java.time.LocalDate
import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import java.util.Date
import java.util.Locale

private enum class MainDestination(val title: String) {
    HOME("خانه"), MEMBERS("اعضا"), PROGRESS("پیشرفت"), SHOP("فروشگاه"), SETTINGS("تنظیمات"),
}

@Composable
fun NegahRoot(
    membersViewModel: MembersViewModel,
    refineryViewModel: RefineryViewModel,
    progressViewModel: ProgressViewModel,
    shopViewModel: ShopViewModel,
    classDaysViewModel: ClassDaysViewModel,
    homeShortcutsViewModel: HomeShortcutsViewModel,
    ringProfileViewModel: RingProfileViewModel,
    sessionsViewModel: SessionsViewModel,
    homeDashboardViewModel: HomeDashboardViewModel,
    dailyQuotesViewModel: DailyQuotesViewModel,
    readingViewModel: ReadingViewModel,
) {
    var selected by rememberSaveable { mutableStateOf(MainDestination.HOME.name) }
    var openRefinery by rememberSaveable { mutableStateOf(false) }
    var openClassDays by rememberSaveable { mutableStateOf(false) }
    var openHomeShortcuts by rememberSaveable { mutableStateOf(false) }
    var openRingProfile by rememberSaveable { mutableStateOf(false) }
    var openSessions by rememberSaveable { mutableStateOf(false) }
    var openMissions by rememberSaveable { mutableStateOf(false) }
    var openWeeklyPulse by rememberSaveable { mutableStateOf(false) }
    var openCoachAssistant by rememberSaveable { mutableStateOf(false) }
    var openDailyQuotes by rememberSaveable { mutableStateOf(false) }
    var openIdentity by rememberSaveable { mutableStateOf(false) }
    var openReading by rememberSaveable { mutableStateOf(false) }
    var openCompetition by rememberSaveable { mutableStateOf(false) }
    var openWheel by rememberSaveable { mutableStateOf(false) }
    val membersState by membersViewModel.state.collectAsStateWithLifecycle()
    val sessionsState by sessionsViewModel.uiState.collectAsStateWithLifecycle()
    val homeAssets by homeDashboardViewModel.state.collectAsStateWithLifecycle()
    val dailyQuotesState by dailyQuotesViewModel.state.collectAsStateWithLifecycle()
    val homeShortcuts by homeShortcutsViewModel.uiState.collectAsStateWithLifecycle()
    val ringProfile by ringProfileViewModel.uiState.collectAsStateWithLifecycle()
    val dashboard = remember(membersState.members, sessionsState.sessions, sessionsState.attendanceRecords, homeAssets.transactions, dailyQuotesState.quotes) {
        buildHomeDashboard(
            members = membersState.members,
            sessions = sessionsState.sessions,
            attendance = sessionsState.attendanceRecords,
            transactions = homeAssets.transactions,
            quotes = dailyQuotesState.quotes,
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected == destination.name,
                        onClick = {
                            if (openHomeShortcuts) homeShortcutsViewModel.discardChanges()
                            selected = destination.name
                            openRefinery = false
                            openClassDays = false
                            openHomeShortcuts = false
                            openRingProfile = false
                            openSessions = false
                            openMissions = false
                            openWeeklyPulse = false
                            openCoachAssistant = false
                            openDailyQuotes = false
                            openIdentity = false
                            openReading = false
                            openCompetition = false
                            openWheel = false
                        },
                        icon = { Text(destination.title.take(1)) },
                        label = { Text(destination.title) },
                    )
                }
            }
        }) { padding ->
            when (MainDestination.valueOf(selected)) {
                MainDestination.HOME -> {
                    if (openCoachAssistant) {
                        CoachAssistantScreen(
                            dashboard = dashboard,
                            onBack = { openCoachAssistant = false },
                            onOpenMembers = { selected = MainDestination.MEMBERS.name; openCoachAssistant = false },
                            onOpenSessions = { selected = MainDestination.PROGRESS.name; openSessions = true; openCoachAssistant = false },
                            modifier = Modifier.padding(padding),
                        )
                    } else if (openWeeklyPulse) {
                        HomeWeeklyPulseScreen(
                            dashboard = dashboard,
                            onBack = { openWeeklyPulse = false },
                            modifier = Modifier.padding(padding),
                        )
                    } else {
                        HomeScreen(
                            dashboard = dashboard,
                            shortcuts = homeShortcuts.selected,
                            ringName = ringProfile.profile.name,
                            ringUsername = ringProfile.profile.username,
                            ringImageBytes = ringProfile.ringImageBytes,
                            bannerBytes = ringProfile.bannerBytes,
                            onOpenShortcut = { shortcut ->
                                openRefinery = false
                                openClassDays = false
                                openHomeShortcuts = false
                                openRingProfile = false
                                openSessions = false
                                openWeeklyPulse = false
                                openCoachAssistant = false
                                when (shortcut) {
                                    HomeShortcut.MEMBERS -> selected = MainDestination.MEMBERS.name
                                    HomeShortcut.PROGRESS -> selected = MainDestination.PROGRESS.name
                                    HomeShortcut.SESSIONS -> {
                                        selected = MainDestination.PROGRESS.name
                                        openSessions = true
                                    }
                                    HomeShortcut.SHOP -> selected = MainDestination.SHOP.name
                                    HomeShortcut.REFINERY -> {
                                        selected = MainDestination.SETTINGS.name
                                        openRefinery = true
                                    }
                                    HomeShortcut.CLASS_DAYS -> {
                                        selected = MainDestination.SETTINGS.name
                                        openClassDays = true
                                    }
                                    HomeShortcut.COACH_ASSISTANT -> {
                                        selected = MainDestination.HOME.name
                                        openCoachAssistant = true
                                    }
                                }
                            },
                            onOpenWeeklyPulse = { openWeeklyPulse = true },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
                MainDestination.MEMBERS -> MembersScreen(membersViewModel, Modifier.padding(padding))
                MainDestination.PROGRESS -> {
                    val closeProgressEntry = {
                        openMissions = false
                        openSessions = false
                        openIdentity = false
                        openReading = false
                        openCompetition = false
                        openWheel = false
                    }
                    val inProgressEntry = openMissions || openSessions || openIdentity || openReading || openCompetition || openWheel
                    BackHandler(enabled = inProgressEntry) { closeProgressEntry() }
                    when {
                        openMissions -> MissionsScreen(progressViewModel, Modifier.padding(padding))
                        openSessions -> SessionsScreen(
                            viewModel = sessionsViewModel,
                            onBack = { openSessions = false },
                            onOpenClassDays = {
                                selected = MainDestination.SETTINGS.name
                                openSessions = false
                                openRefinery = false
                                openClassDays = true
                            },
                            modifier = Modifier.padding(padding),
                        )
                        openIdentity -> ProgressScreen(
                            viewModel = progressViewModel,
                            onOpenSessions = { openIdentity = false; openSessions = true },
                            modifier = Modifier.padding(padding),
                        )
                        openReading -> ReadingScreen(readingViewModel, onBack = { openReading = false }, modifier = Modifier.padding(padding))
                        openCompetition -> CompetitionScreen(progressViewModel, onBack = { openCompetition = false }, modifier = Modifier.padding(padding))
                        openWheel -> WheelPendingScreen(onBack = { openWheel = false }, modifier = Modifier.padding(padding))
                        else -> ProgressHubScreen(
                            onOpen = { entry ->
                                when (entry) {
                                    ProgressEntry.IDENTITY -> openIdentity = true
                                    ProgressEntry.MISSIONS -> openMissions = true
                                    ProgressEntry.READING -> openReading = true
                                    ProgressEntry.WHEEL -> openWheel = true
                                    ProgressEntry.COMPETITION -> openCompetition = true
                                    ProgressEntry.SESSIONS -> openSessions = true
                                }
                            },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
                MainDestination.SETTINGS -> {
                    when {
                        openRefinery -> RefineryScreen(refineryViewModel, Modifier.padding(padding))
                        openRingProfile -> RingProfileScreen(ringProfileViewModel, onBack = { openRingProfile = false }, modifier = Modifier.padding(padding))
                        openDailyQuotes -> DailyQuotesScreen(dailyQuotesViewModel, onBack = { openDailyQuotes = false }, modifier = Modifier.padding(padding))
                        openClassDays -> ClassDaysScreen(
                            viewModel = classDaysViewModel,
                            onBack = { openClassDays = false },
                            modifier = Modifier.padding(padding),
                        )
                        openHomeShortcuts -> HomeShortcutsScreen(
                            viewModel = homeShortcutsViewModel,
                            onBack = { openHomeShortcuts = false },
                            modifier = Modifier.padding(padding),
                        )
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)
                                Button(onClick = { openHomeShortcuts = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("میان‌برهای خانه")
                                }
                                Button(onClick = { openRingProfile = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("ویرایش پروفایل حلقه")
                                }
                                Button(onClick = { openRefinery = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("پالایشگاه")
                                }
                                Button(onClick = { openDailyQuotes = true }, modifier = Modifier.fillMaxWidth()) { Text("جمله‌های روز") }
                                Button(onClick = { openClassDays = true }, modifier = Modifier.fillMaxWidth()) { Text("روزهای کلاس") }
                            }
                        }
                    }
                }
                MainDestination.SHOP -> ShopScreen(shopViewModel, Modifier.padding(padding))
            }
        }
    }
}

private fun formatSolarHijriDate(epochMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd", ULocale("fa_IR@calendar=persian")).format(Date(epochMillis))

private data class TimedHomeEvent(val title: String, val detail: String, val timestampEpochMillis: Long)

private fun buildHomeDashboard(
    members: List<com.kichikan.negah.domain.member.Member>,
    sessions: List<Session>,
    attendance: List<com.kichikan.negah.domain.attendance.AttendanceRecord>,
    transactions: List<EconomyTransaction>,
    quotes: List<DailyQuote>,
): HomeDashboardState {
    val today = LocalDate.now()
    val attendanceBySession = attendance.groupBy { it.sessionId }
    val days = (6L downTo 0L).map { offset ->
        val date = today.minusDays(offset)
        val sessionIds = sessions.asSequence()
            .filter { it.sessionDateEpochDay == date.toEpochDay() }
            .map(Session::id)
            .toSet()
        val records = sessionIds.flatMap { attendanceBySession[it].orEmpty() }
        HomeAttendanceDay(
            label = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("fa")),
            presentCount = records.count { it.status == AttendanceStatus.PRESENT },
            recordedCount = records.size,
        )
    }
    val weekOffset = (today.dayOfWeek.value + 1) % 7
    val weekStart = today.minusDays(weekOffset.toLong()).toEpochDay()
    val weekEnd = weekStart + 6
    val currentWeekSessions = sessions.filter { it.sessionDateEpochDay in weekStart..weekEnd }
    val currentWeekSessionIds = currentWeekSessions.map(Session::id).toSet()
    val currentWeekAttendance = attendance.filter { it.sessionId in currentWeekSessionIds }
    val recent = sessions.sortedByDescending(Session::sessionDateEpochDay).take(3).map { session ->
        HomeRecentSession(
            title = session.title,
            dateLabel = formatSolarHijriDate(LocalDate.ofEpochDay(session.sessionDateEpochDay).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()),
            completed = session.isCompleted,
        )
    }
    val timedEvents = buildList {
        sessions.forEach { session -> add(TimedHomeEvent("جلسه ثبت شد", session.title + " · " + if (session.isCompleted) "وضعیت فعلی: تکمیل‌شده" else "وضعیت فعلی: در انتظار", session.createdAtEpochMillis)) }
        attendance.forEach { record ->
            val statusLabel = when (record.status) {
                AttendanceStatus.PRESENT -> "حاضر"
                AttendanceStatus.ABSENT -> "غیبت"
                AttendanceStatus.EXCUSED_ABSENCE -> "غیبت موجه"
                AttendanceStatus.LATE -> "تأخیر"
                AttendanceStatus.UNRECORDED -> "بدون ثبت"
            }
            add(TimedHomeEvent("ثبت حضور و غیاب", statusLabel, record.updatedAtEpochMillis))
        }
        transactions.forEach { transaction ->
            val currency = when (transaction.currency) {
                EconomyCurrency.XP -> "XP"
                EconomyCurrency.POINTS -> "امتیاز"
                EconomyCurrency.DIAMONDS -> "الماس"
            }
            val amount = if (transaction.amountDelta > 0) "+" + transaction.amountDelta else transaction.amountDelta.toString()
            add(TimedHomeEvent("تغییر موجودی " + currency, amount, transaction.createdAtEpochMillis))
        }
    }.sortedByDescending { it.timestampEpochMillis }.take(20)
    val activeQuotes = quotes.filter { it.isActive }.sortedBy { it.createdAtEpochMillis }
    val dailyQuote = if (activeQuotes.isEmpty()) "" else activeQuotes[kotlin.random.Random(today.toEpochDay()).nextInt(activeQuotes.size)].text
    return HomeDashboardState(
        dailyQuote = dailyQuote,
        recentEvents = timedEvents.map { event ->
            com.kichikan.negah.feature.home.HomeRecentEvent(event.title, event.detail,
                formatSolarHijriDate(event.timestampEpochMillis))
        },
        activeMemberCount = members.size,
        totalXp = members.sumOf { it.totalXp },
        totalPoints = members.sumOf { it.spendablePoints },
        totalDiamonds = members.sumOf { it.diamonds },
        weekSessionCount = currentWeekSessions.size,
        weekRecordedAttendanceCount = currentWeekAttendance.size,
        attendanceDays = days,
        recentSessions = recent,
    )
}
