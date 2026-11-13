package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.member.JalaliDateText
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.reading.BookReading
import com.kichikan.negah.domain.reading.ReadingRepository
import com.kichikan.negah.domain.reading.RecordBookReading
import com.kichikan.negah.domain.reading.RecordBookReadingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReadingUiState(
    val members: List<Member> = emptyList(),
    val readings: List<BookReading> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    memberRepository: MemberRepository,
    readingRepository: ReadingRepository,
    private val recordBookReading: RecordBookReading,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReadingUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            memberRepository.observeActiveMembers().collect { members ->
                mutableState.update { it.copy(members = members) }
            }
        }
        viewModelScope.launch {
            readingRepository.observeAll().collect { readings ->
                mutableState.update { it.copy(readings = readings) }
            }
        }
    }

    /** Returns true when the book was saved so the form can reset itself. */
    suspend fun record(memberId: String?, title: String, level: Int, jalaliDate: String): Boolean {
        if (memberId == null) {
            mutableState.update { it.copy(message = "ابتدا عضو را انتخاب کنید") }
            return false
        }
        val date = JalaliDateText.parse(jalaliDate)
        if (date == null) {
            mutableState.update { it.copy(message = "تاریخ را به شکل ۱۴۰۵/۰۸/۲۱ وارد کنید") }
            return false
        }
        if (date.isAfter(LocalDate.now())) {
            mutableState.update { it.copy(message = "تاریخ مطالعه نمی‌تواند در آینده باشد") }
            return false
        }
        val readAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = recordBookReading(memberId, title, readAt, level, System.currentTimeMillis())
        val message = when (result) {
            is RecordBookReadingResult.Saved -> "کتاب ثبت شد"
            RecordBookReadingResult.InvalidMember -> "عضو نامعتبر است"
            RecordBookReadingResult.InvalidTitle -> "عنوان کتاب را وارد کنید"
            RecordBookReadingResult.InvalidLevel -> "سطح باید بین ۱ تا ۵ باشد"
        }
        mutableState.update { it.copy(message = message) }
        return result is RecordBookReadingResult.Saved
    }

    fun clearMessage() = mutableState.update { it.copy(message = null) }
}
