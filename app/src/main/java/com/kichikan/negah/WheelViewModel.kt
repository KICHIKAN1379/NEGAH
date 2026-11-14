package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.wheel.SavePrizeResult
import com.kichikan.negah.domain.wheel.SaveWheelPrize
import com.kichikan.negah.domain.wheel.WheelPrize
import com.kichikan.negah.domain.wheel.WheelRepository
import com.kichikan.negah.domain.wheel.WheelSpin
import com.kichikan.negah.domain.wheel.WheelSpinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WheelUiState(
    val members: List<Member> = emptyList(),
    val prizes: List<WheelPrize> = emptyList(),
    val spinCost: Long = 0L,
    val spins: List<WheelSpin> = emptyList(),
    val lastWin: WheelSpin? = null,
    /** Bumped on every successful spin so the screen can start one animation per spin. */
    val spinToken: Long = 0L,
    /** Phone-clock second (0..59) at the moment of the tap; drives turns and duration. */
    val spinSecond: Int = 0,
    val message: String? = null,
)

@HiltViewModel
class WheelViewModel @Inject constructor(
    memberRepository: MemberRepository,
    private val repository: WheelRepository,
    private val saveWheelPrize: SaveWheelPrize,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WheelUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch { memberRepository.observeActiveMembers().collect { v -> mutableState.update { it.copy(members = v) } } }
        viewModelScope.launch { repository.observePrizes().collect { v -> mutableState.update { it.copy(prizes = v) } } }
        viewModelScope.launch { repository.observeSpinCost().collect { v -> mutableState.update { it.copy(spinCost = v) } } }
        viewModelScope.launch { repository.observeSpins().collect { v -> mutableState.update { it.copy(spins = v) } } }
    }

    fun spin(memberId: String?) {
        if (memberId == null) {
            mutableState.update { it.copy(message = "ابتدا عضو را انتخاب کنید") }
            return
        }
        viewModelScope.launch {
            // The outcome and the motion both come from the phone clock at the moment of the tap.
            val now = System.currentTimeMillis()
            val second = ((now / 1000L) % 60L).toInt()
            val result = repository.spin(memberId, UUID.randomUUID().toString(), now) { total ->
                Math.floorMod(now, total)
            }
            mutableState.update {
                when (result) {
                    is WheelSpinResult.Won -> it.copy(lastWin = result.spin, message = null, spinToken = it.spinToken + 1, spinSecond = second)
                    WheelSpinResult.NoActivePrizes -> it.copy(lastWin = null, message = "هیچ جایزه فعالی تعریف نشده است")
                    WheelSpinResult.InsufficientPoints -> it.copy(lastWin = null, message = "امتیاز قابل‌خرج عضو برای چرخاندن کافی نیست")
                    WheelSpinResult.InvalidMember -> it.copy(lastWin = null, message = "عضو نامعتبر است")
                    WheelSpinResult.BalanceOverflow -> it.copy(lastWin = null, message = "سقف موجودی مجاز است؛ چرخش انجام نشد")
                }
            }
        }
    }

    fun savePrize(id: String?, label: String, currency: EconomyCurrency, amount: Long?, weight: Int?, active: Boolean) {
        viewModelScope.launch {
            val result = saveWheelPrize(
                WheelPrize(id ?: UUID.randomUUID().toString(), label, currency, amount ?: 0L, weight ?: 0, active),
                System.currentTimeMillis(),
            )
            val text = when (result) {
                SavePrizeResult.Saved -> "جایزه ذخیره شد"
                SavePrizeResult.InvalidLabel -> "عنوان جایزه را وارد کنید"
                SavePrizeResult.InvalidAmount -> "مقدار جایزه باید بیشتر از صفر باشد"
                SavePrizeResult.InvalidWeight -> "وزن باید عدد صحیح مثبت باشد"
            }
            mutableState.update { it.copy(message = text) }
        }
    }

    fun setPrizeActive(prize: WheelPrize, active: Boolean) {
        savePrize(prize.id, prize.label, prize.currency, prize.amount, prize.weight, active)
    }

    fun setSpinCost(cost: Long?) {
        if (cost == null || cost < 0L) {
            mutableState.update { it.copy(message = "هزینه باید عدد صفر یا بیشتر باشد") }
            return
        }
        viewModelScope.launch {
            repository.setSpinCost(cost)
            mutableState.update { it.copy(message = "هزینه چرخش ذخیره شد") }
        }
    }

    fun clearMessage() = mutableState.update { it.copy(message = null) }
}
