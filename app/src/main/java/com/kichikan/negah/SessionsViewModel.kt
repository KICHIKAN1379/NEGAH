package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.attendance.AttendanceDraft
import com.kichikan.negah.domain.attendance.AttendanceRecord
import com.kichikan.negah.domain.attendance.AttendanceRepository
import com.kichikan.negah.domain.attendance.AttendanceStatus
import com.kichikan.negah.domain.attendance.SaveAttendance
import com.kichikan.negah.domain.attendance.SaveAttendanceResult
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.session.SaveSession
import com.kichikan.negah.domain.session.SaveSessionResult
import com.kichikan.negah.domain.session.Session
import com.kichikan.negah.domain.session.SessionRepository
import com.kichikan.negah.domain.settings.ClassDay
import com.kichikan.negah.domain.settings.ClassDaysRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SessionFilter { ALL, UPCOMING, COMPLETED }

data class SessionsUiState(
    val sessions: List<Session> = emptyList(),
    val members: List<Member> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val classDays: Set<ClassDay> = emptySet(),
    val searchQuery: String = "",
    val filter: SessionFilter = SessionFilter.ALL,
    val selectedDateEpochDay: Long? = null,
    val selectedSessionId: String? = null,
    val attendanceDrafts: Map<String, AttendanceDraft> = emptyMap(),
    val isLoading: Boolean = true,
    val isAttendanceLoading: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val memberRepository: MemberRepository,
    private val attendanceRepository: AttendanceRepository,
    private val classDaysRepository: ClassDaysRepository,
    private val saveSession: SaveSession,
    private val saveAttendance: SaveAttendance,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState = _uiState.asStateFlow()
    private var attendanceJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                sessionRepository.observeAll(),
                memberRepository.observeActiveMembers(),
                attendanceRepository.observeAll(),
                classDaysRepository.observeSelectedDays(),
            ) { sessions, members, attendance, classDays ->
                SessionsSnapshot(sessions, members, attendance, classDays)
            }.collect { snapshot ->
                _uiState.update { current ->
                    val drafts = if (current.selectedSessionId == null) {
                        current.attendanceDrafts
                    } else {
                        snapshot.members.associate { member ->
                            member.id to (current.attendanceDrafts[member.id]
                                ?: AttendanceDraft(member.id, AttendanceStatus.UNRECORDED, null))
                        }
                    }
                    current.copy(
                        sessions = snapshot.sessions,
                        members = snapshot.members,
                        attendanceRecords = snapshot.attendance,
                        classDays = snapshot.classDays,
                        attendanceDrafts = drafts,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: SessionFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun selectCalendarDate(epochDay: Long?) {
        _uiState.update { it.copy(selectedDateEpochDay = epochDay) }
    }

    fun createSession(
        title: String,
        topic: String?,
        location: String?,
        notes: String?,
        sessionDateEpochDay: Long,
    ) {
        viewModelScope.launch {
            try {
                when (
                    saveSession(
                        title = title,
                        topic = topic,
                        location = location,
                        notes = notes,
                        sessionDateEpochDay = sessionDateEpochDay,
                        nowEpochMillis = System.currentTimeMillis(),
                    )
                ) {
                    is SaveSessionResult.Saved -> setMessage("جلسه ثبت شد.", isError = false)
                    SaveSessionResult.InvalidTitle -> setMessage("عنوان جلسه را وارد کنید.", isError = true)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setMessage("ثبت جلسه انجام نشد. دوباره تلاش کنید.", isError = true)
            }
        }
    }

    fun openSession(sessionId: String) {
        val current = _uiState.value
        if (current.sessions.none { it.id == sessionId }) return
        attendanceJob?.cancel()
        val defaults = current.members.associate { member ->
            member.id to AttendanceDraft(member.id, AttendanceStatus.UNRECORDED, null)
        }
        _uiState.update {
            it.copy(
                selectedSessionId = sessionId,
                attendanceDrafts = defaults,
                isAttendanceLoading = true,
                isSaving = false,
                message = null,
                messageIsError = false,
            )
        }
        attendanceJob = viewModelScope.launch {
            attendanceRepository.observeForSession(sessionId).collect { records ->
                val existing = records.associateBy { it.memberId }
                _uiState.update { state ->
                    if (state.selectedSessionId != sessionId) state
                    else {
                        val drafts = state.members.associate { member ->
                            val record = existing[member.id]
                            member.id to if (record == null) {
                                AttendanceDraft(member.id, AttendanceStatus.UNRECORDED, null)
                            } else {
                                AttendanceDraft(member.id, record.status, record.reason)
                            }
                        }
                        state.copy(
                            attendanceDrafts = drafts,
                            isAttendanceLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun closeSession() {
        attendanceJob?.cancel()
        attendanceJob = null
        _uiState.update {
            it.copy(
                selectedSessionId = null,
                attendanceDrafts = emptyMap(),
                isAttendanceLoading = false,
                isSaving = false,
                message = null,
                messageIsError = false,
            )
        }
    }

    fun setAttendanceStatus(memberId: String, status: AttendanceStatus) {
        _uiState.update { state ->
            if (state.isAttendanceLoading || state.isSaving) return@update state
            val existing = state.attendanceDrafts[memberId]
                ?: AttendanceDraft(memberId, AttendanceStatus.UNRECORDED, null)
            state.copy(
                attendanceDrafts = state.attendanceDrafts + (
                    memberId to existing.copy(
                        status = status,
                        reason = if (status.requiresReason) existing.reason else null,
                    )
                ),
                message = null,
                messageIsError = false,
            )
        }
    }

    fun setAttendanceReason(memberId: String, reason: String) {
        _uiState.update { state ->
            if (state.isAttendanceLoading || state.isSaving) return@update state
            val existing = state.attendanceDrafts[memberId]
                ?: AttendanceDraft(memberId, AttendanceStatus.UNRECORDED, null)
            state.copy(
                attendanceDrafts = state.attendanceDrafts + (memberId to existing.copy(reason = reason)),
                message = null,
                messageIsError = false,
            )
        }
    }

    fun saveAttendance() {
        val current = _uiState.value
        val sessionId = current.selectedSessionId ?: return
        if (current.isAttendanceLoading || current.isSaving) return
        if (current.members.isEmpty()) {
            setMessage("برای این جلسه عضوی ثبت نشده است.", isError = true)
            return
        }
        val drafts = current.members.map { member ->
            current.attendanceDrafts[member.id]
                ?: AttendanceDraft(member.id, AttendanceStatus.UNRECORDED, null)
        }
        _uiState.update { it.copy(isSaving = true, message = null, messageIsError = false) }

        viewModelScope.launch {
            try {
                when (val result = saveAttendance(sessionId, drafts, System.currentTimeMillis())) {
                    SaveAttendanceResult.Saved -> setMessage("حضور و غیاب جلسه ذخیره شد.", isError = false)
                    is SaveAttendanceResult.ReasonRequired -> {
                        val name = current.members.firstOrNull { it.id == result.memberId }?.fullName ?: "عضو"
                        setMessage("برای " + name + " وارد کردن دلیل الزامی است.", isError = true)
                    }
                    SaveAttendanceResult.DuplicateMember -> {
                        setMessage("برای یک عضو بیش از یک وضعیت انتخاب شده است.", isError = true)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                setMessage("ذخیره حضور و غیاب انجام نشد. سوابق قبلی حفظ شده‌اند.", isError = true)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun setCompleted(sessionId: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                sessionRepository.setCompleted(sessionId, completed)
                setMessage(if (completed) "جلسه تکمیل شد." else "جلسه به وضعیت در انتظار برگشت.", isError = false)
            } catch (_: Exception) {
                setMessage("تغییر وضعیت جلسه انجام نشد.", isError = true)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val deleted = sessionRepository.delete(sessionId)
                setMessage(if (deleted) "جلسه حذف شد." else "جلسه پیدا نشد.", isError = !deleted)
            } catch (_: Exception) {
                setMessage("به‌دلیل وجود سوابق حضور و غیاب، حذف این جلسه ممکن نیست.", isError = true)
            }
        }
    }

    private fun setMessage(message: String, isError: Boolean) {
        _uiState.update { it.copy(message = message, messageIsError = isError) }
    }
}

private data class SessionsSnapshot(
    val sessions: List<Session>,
    val members: List<Member>,
    val attendance: List<AttendanceRecord>,
    val classDays: Set<ClassDay>,
)
