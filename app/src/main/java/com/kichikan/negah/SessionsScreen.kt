package com.kichikan.negah

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.attendance.AttendanceDraft
import com.kichikan.negah.domain.attendance.AttendanceStatus
import com.kichikan.negah.domain.session.Session
import com.kichikan.negah.domain.settings.ClassDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onBack: () -> Unit,
    onOpenClassDays: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSessionId = state.selectedSessionId

    if (selectedSessionId != null) {
        val session = state.sessions.firstOrNull { it.id == selectedSessionId }
        AttendanceSessionScreen(
            state = state,
            session = session,
            onBack = viewModel::closeSession,
            onStatusChange = viewModel::setAttendanceStatus,
            onReasonChange = viewModel::setAttendanceReason,
            onSave = viewModel::saveAttendance,
            onSetCompleted = { viewModel.setCompleted(selectedSessionId, it) },
            modifier = modifier,
        )
    } else {
        SessionListScreen(
            state = state,
            onBack = onBack,
            onOpenClassDays = onOpenClassDays,
            onSearchChange = viewModel::setSearchQuery,
            onFilterChange = viewModel::setFilter,
            onDateSelected = viewModel::selectCalendarDate,
            onOpenSession = viewModel::openSession,
            onSetCompleted = viewModel::setCompleted,
            onSaveSession = viewModel::saveSession,
            modifier = modifier,
        )
    }
}

@Composable
private fun SessionListScreen(
    state: SessionsUiState,
    onBack: () -> Unit,
    onOpenClassDays: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (SessionFilter) -> Unit,
    onDateSelected: (Long?) -> Unit,
    onOpenSession: (String) -> Unit,
    onSetCompleted: (String, Boolean) -> Unit,
    onSaveSession: (Session?, String, String?, String?, String?, Long) -> Unit,
    modifier: Modifier,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<Session?>(null) }

    val memberNamesBySession = remember(state.attendanceRecords, state.members) {
        val namesById = state.members.associate { it.id to it.fullName }
        state.attendanceRecords.groupBy { it.sessionId }.mapValues { (_, records) ->
            records.mapNotNull { namesById[it.memberId] }.toSet()
        }
    }
    val filteredSessions = remember(
        state.sessions,
        state.searchQuery,
        state.filter,
        state.selectedDateEpochDay,
        memberNamesBySession,
    ) {
        state.sessions
            .filter { session ->
                when (state.filter) {
                    SessionFilter.ALL -> true
                    SessionFilter.UPCOMING -> !session.isCompleted
                    SessionFilter.COMPLETED -> session.isCompleted
                }
            }
            .filter { session ->
                state.selectedDateEpochDay?.let { session.sessionDateEpochDay == it } ?: true
            }
            .filter { session ->
                val query = state.searchQuery.trim()
                query.isBlank() || listOfNotNull(
                    session.title,
                    session.topic,
                    session.location,
                ).any { it.contains(query, ignoreCase = true) } ||
                    memberNamesBySession[session.id].orEmpty().any { it.contains(query, ignoreCase = true) }
            }
            .sortedWith(
                compareBy<Session> { it.isCompleted }
                    .thenBy { if (it.isCompleted) -it.sessionDateEpochDay else it.sessionDateEpochDay },
            )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("بازگشت") }
            Text("جلسات", style = MaterialTheme.typography.headlineSmall)
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("کل: " + state.sessions.size)
                Text("در پیش: " + state.sessions.count { !it.isCompleted })
                Text("تکمیل‌شده: " + state.sessions.count { it.isCompleted })
            }

            WeeklySessionCalendar(
                classDays = state.classDays,
                selectedDateEpochDay = state.selectedDateEpochDay,
                onDateSelected = onDateSelected,
                onOpenClassDays = onOpenClassDays,
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("جست‌وجوی عنوان، موضوع، مکان یا عضو") },
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SessionFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    SessionFilter.ALL -> "همه"
                                    SessionFilter.UPCOMING -> "در پیش"
                                    SessionFilter.COMPLETED -> "تکمیل‌شده"
                                },
                            )
                        },
                    )
                }
                FilterChip(
                    selected = state.selectedDateEpochDay == null,
                    onClick = { onDateSelected(null) },
                    label = { Text("همه روزها") },
                )
            }

            Button(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("جلسه جدید")
            }

            if (state.message != null) {
                Text(
                    state.message,
                    color = if (state.messageIsError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }

            if (filteredSessions.isEmpty()) {
                Text("جلسه‌ای مطابق این فیلتر پیدا نشد.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        val counts = state.attendanceRecords
                            .filter { it.sessionId == session.id }
                            .groupingBy { it.status }
                            .eachCount()
                        SessionCard(
                            session = session,
                            presentCount = counts[AttendanceStatus.PRESENT] ?: 0,
                            absentCount = (counts[AttendanceStatus.ABSENT] ?: 0) +
                                (counts[AttendanceStatus.EXCUSED_ABSENCE] ?: 0),
                            onOpen = { onOpenSession(session.id) },
                            onEdit = { editingSession = session },
                            onSetCompleted = { onSetCompleted(session.id, !session.isCompleted) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingSession != null) {
        SessionFormDialog(
            initialSession = editingSession,
            onDismiss = {
                showCreateDialog = false
                editingSession = null
            },
            onSave = { existingSession, title, topic, location, notes, dateEpochDay ->
                onSaveSession(existingSession, title, topic, location, notes, dateEpochDay)
                showCreateDialog = false
                editingSession = null
            },
        )
    }


}

@Composable
private fun SessionCard(
    session: Session,
    presentCount: Int,
    absentCount: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onSetCompleted: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                session.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable(onClick = onOpen),
            )
            Text(formatSessionDate(session.sessionDateEpochDay), style = MaterialTheme.typography.bodySmall)
            session.topic?.let { Text("موضوع: " + it) }
            session.location?.let { Text("مکان: " + it) }
            Text("حاضر: $presentCount  •  غیبت/موجه: $absentCount")
            Text(if (session.isCompleted) "وضعیت: تکمیل‌شده" else "وضعیت: در پیش")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpen) { Text("حضور و غیاب") }
                TextButton(onClick = onEdit) { Text("ویرایش") }
                TextButton(onClick = onSetCompleted) {
                    Text(if (session.isCompleted) "بازگشایی" else "تکمیل جلسه")
                }
            }
        }
    }
}

@Composable
private fun WeeklySessionCalendar(
    classDays: Set<ClassDay>,
    selectedDateEpochDay: Long?,
    onDateSelected: (Long?) -> Unit,
    onOpenClassDays: () -> Unit,
) {
    if (classDays.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("روزهای کلاس هنوز تنظیم نشده‌اند؛ تقویم جلسات از تنظیمات ساخته می‌شود.")
            TextButton(onClick = onOpenClassDays) { Text("تنظیم روزهای کلاس") }
        }
        return
    }

    val today = LocalDate.now()
    val start = today.minusDays(today.daysSinceSaturday().toLong())
    val dates = (0..6).map { start.plusDays(it.toLong()) }
        .filter { it.toClassDay() in classDays }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedDateEpochDay == null,
            onClick = { onDateSelected(null) },
            label = { Text("همه") },
        )
        dates.forEach { date ->
            FilterChip(
                selected = selectedDateEpochDay == date.toEpochDay(),
                onClick = { onDateSelected(date.toEpochDay()) },
                label = { Text(date.toClassDay().label + " " + date.dayOfMonth) },
            )
        }
    }
}

private fun LocalDate.daysSinceSaturday(): Int = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> 0
    DayOfWeek.SUNDAY -> 1
    DayOfWeek.MONDAY -> 2
    DayOfWeek.TUESDAY -> 3
    DayOfWeek.WEDNESDAY -> 4
    DayOfWeek.THURSDAY -> 5
    DayOfWeek.FRIDAY -> 6
}

private fun LocalDate.toClassDay(): ClassDay = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> ClassDay.SATURDAY
    DayOfWeek.SUNDAY -> ClassDay.SUNDAY
    DayOfWeek.MONDAY -> ClassDay.MONDAY
    DayOfWeek.TUESDAY -> ClassDay.TUESDAY
    DayOfWeek.WEDNESDAY -> ClassDay.WEDNESDAY
    DayOfWeek.THURSDAY -> ClassDay.THURSDAY
    DayOfWeek.FRIDAY -> ClassDay.FRIDAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionFormDialog(
    initialSession: Session?,
    onDismiss: () -> Unit,
    onSave: (Session?, String, String?, String?, String?, Long) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initialSession?.title.orEmpty()) }
    var topic by rememberSaveable { mutableStateOf(initialSession?.topic.orEmpty()) }
    var location by rememberSaveable { mutableStateOf(initialSession?.location.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(initialSession?.notes.orEmpty()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val initialDateMillis = remember(initialSession?.sessionDateEpochDay) {
        val initialDate = initialSession?.sessionDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            ?: LocalDate.now()
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val selectedDate = dateState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    } ?: LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSession == null) "جلسه جدید" else "ویرایش جلسه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it; validationError = null }, label = { Text("عنوان جلسه") }, singleLine = true)
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("موضوع") }, singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("مکان") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("یادداشت") })
                TextButton(onClick = { showDatePicker = true }) {
                    Text("تاریخ: " + selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")))
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) validationError = "عنوان جلسه را وارد کنید."
                else onSave(
                    initialSession,
                    title,
                    topic.takeIf(String::isNotBlank),
                    location.takeIf(String::isNotBlank),
                    notes.takeIf(String::isNotBlank),
                    selectedDate.toEpochDay(),
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("انتخاب") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("انصراف") } },
        ) {
            DatePicker(state = dateState)
        }
    }
}

@Composable
private fun AttendanceSessionScreen(
    state: SessionsUiState,
    session: Session?,
    onBack: () -> Unit,
    onStatusChange: (String, AttendanceStatus) -> Unit,
    onReasonChange: (String, String) -> Unit,
    onSave: () -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("بازگشت به جلسات") }
            Text("حضور و غیاب", style = MaterialTheme.typography.headlineSmall)
        }
        Text(session?.title ?: "جلسه", style = MaterialTheme.typography.titleLarge)
        session?.let {
            Text(formatSessionDate(it.sessionDateEpochDay), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onSetCompleted(!it.isCompleted) }) {
                Text(if (it.isCompleted) "بازگشایی جلسه" else "علامت‌گذاری به‌عنوان تکمیل‌شده")
            }
        }

        if (state.isAttendanceLoading) {
            CircularProgressIndicator()
        } else if (state.members.isEmpty()) {
            Text("برای ثبت حضور و غیاب ابتدا عضو ثبت کنید.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.members, key = { it.id }) { member ->
                    val draft = state.attendanceDrafts[member.id]
                        ?: AttendanceDraft(member.id, AttendanceStatus.UNRECORDED, null)
                    AttendanceMemberCard(
                        memberName = member.nickname?.takeIf(String::isNotBlank) ?: member.fullName,
                        draft = draft,
                        enabled = !state.isSaving,
                        onStatusChange = { onStatusChange(member.id, it) },
                        onReasonChange = { onReasonChange(member.id, it) },
                    )
                }
            }
        }

        state.message?.let {
            Text(
                it,
                color = if (state.messageIsError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = onSave,
            enabled = !state.isAttendanceLoading && !state.isSaving && state.members.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) "در حال ذخیره..." else "ثبت حضور و غیاب")
        }
    }
}

@Composable
private fun AttendanceMemberCard(
    memberName: String,
    draft: AttendanceDraft,
    enabled: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit,
    onReasonChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(memberName, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AttendanceStatus.entries.forEach { status ->
                    FilterChip(
                        selected = draft.status == status,
                        onClick = { if (enabled) onStatusChange(status) },
                        label = { Text(statusLabel(status)) },
                        enabled = enabled,
                    )
                }
            }
            if (draft.status.requiresReason) {
                OutlinedTextField(
                    value = draft.reason.orEmpty(),
                    onValueChange = onReasonChange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("دلیل (الزامی)") },
                    singleLine = true,
                )
            }
        }
    }
}

private fun statusLabel(status: AttendanceStatus): String = when (status) {
    AttendanceStatus.PRESENT -> "حاضر"
    AttendanceStatus.ABSENT -> "غیبت"
    AttendanceStatus.EXCUSED_ABSENCE -> "غیبت موجه"
    AttendanceStatus.LATE -> "تأخیر"
    AttendanceStatus.UNRECORDED -> "بدون ثبت"
}

private fun formatSessionDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
