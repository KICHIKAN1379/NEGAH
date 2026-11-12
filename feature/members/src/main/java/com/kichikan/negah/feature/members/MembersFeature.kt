package com.kichikan.negah.feature.members

import android.icu.util.Calendar
import android.icu.util.ULocale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kichikan.negah.domain.economy.EconomyCurrency
import com.kichikan.negah.domain.economy.EconomyRepository
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.economy.EconomyTransactionKind
import com.kichikan.negah.domain.economy.LevelProgression
import com.kichikan.negah.domain.economy.RankBadgeProgression
import com.kichikan.negah.domain.report.MemberReportSummary
import com.kichikan.negah.domain.member.Member
import com.kichikan.negah.domain.member.MemberRepository
import com.kichikan.negah.domain.member.MemberAvatarRepository
import com.kichikan.negah.domain.member.RegisterMember
import com.kichikan.negah.domain.member.RegisterMemberResult
import com.kichikan.negah.domain.growth.GrowthItem
import com.kichikan.negah.domain.growth.GrowthMapRepository
import com.kichikan.negah.domain.growth.GrowthTrack
import com.kichikan.negah.domain.growth.MemberGrowthProgress
import com.kichikan.negah.domain.growth.UpdateGrowthProgress
import com.kichikan.negah.domain.growth.UpdateGrowthProgressResult
import com.kichikan.negah.domain.inventory.ActiveInventoryItem
import com.kichikan.negah.domain.inventory.CatalogItem
import com.kichikan.negah.domain.inventory.CatalogItemMediaRepository
import com.kichikan.negah.domain.inventory.InventoryCategory
import com.kichikan.negah.domain.inventory.InventoryRepository
import com.kichikan.negah.domain.inventory.MemberInventoryEntry
import com.kichikan.negah.domain.mission.Mission
import com.kichikan.negah.domain.mission.MissionAssignment
import com.kichikan.negah.domain.mission.MissionAssignmentStatus
import com.kichikan.negah.domain.mission.MissionKind
import com.kichikan.negah.domain.mission.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import java.io.InputStream

enum class MemberSortOption { NAME, LEVEL, POINTS }
enum class MemberHistoryFilter { ALL, XP, POINTS, DIAMONDS, ITEMS }

data class MembersUiState(
    val members: List<Member> = emptyList(),
    val archivedMembers: List<Member> = emptyList(),
    val showingArchived: Boolean = false,
    val fullName: String = "",
    val nickname: String = "",
    val birthDateJalali: String = "",
    val searchQuery: String = "",
    val sortOption: MemberSortOption = MemberSortOption.NAME,
    val selectedMemberId: String? = null,
    val economyTransactions: List<EconomyTransaction> = emptyList(),
    val historyFilter: MemberHistoryFilter = MemberHistoryFilter.ALL,
    val growthItems: List<GrowthItem> = emptyList(),
    val growthProgress: List<MemberGrowthProgress> = emptyList(),
    val growthSaveMessage: String? = null,
    val inventoryEntries: List<MemberInventoryEntry> = emptyList(),
    val availableCatalogItems: List<CatalogItem> = emptyList(),
    val activeInventoryItems: List<ActiveInventoryItem> = emptyList(),
    val missions: List<Mission> = emptyList(),
    val missionAssignments: List<MissionAssignment> = emptyList(),
    val catalogImageBytes: Map<String, ByteArray> = emptyMap(),
    val inventoryMessage: String? = null,
    val isSaving: Boolean = false,
    val validationMessage: String? = null,
    val savedMessage: String? = null,
    val profileMessage: String? = null,
    val avatarBytes: ByteArray? = null,
    val avatarMessage: String? = null,
) {
    val visibleEconomyTransactions: List<EconomyTransaction>
        get() = economyTransactions.filter { transaction ->
            when (historyFilter) {
                MemberHistoryFilter.ALL -> true
                MemberHistoryFilter.XP -> transaction.currency == EconomyCurrency.XP
                MemberHistoryFilter.POINTS -> transaction.currency == EconomyCurrency.POINTS
                MemberHistoryFilter.DIAMONDS -> transaction.currency == EconomyCurrency.DIAMONDS
                MemberHistoryFilter.ITEMS -> false
            }
        }

    val visibleMembers: List<Member>
        get() {
            val source = if (showingArchived) archivedMembers else members
            val filtered = source.filter {
                searchQuery.isBlank() || it.fullName.contains(searchQuery.trim(), true) ||
                    it.nickname.orEmpty().contains(searchQuery.trim(), true)
            }
            return when (sortOption) {
                MemberSortOption.NAME -> filtered.sortedWith(
                    compareBy<Member> { it.fullName.lowercase() }.thenBy { it.id },
                )
                MemberSortOption.LEVEL -> filtered.sortedWith(
                    compareByDescending<Member> { LevelProgression.levelFor(it.totalXp) }
                        .thenByDescending { it.totalXp }
                        .thenBy { it.fullName.lowercase() },
                )
                MemberSortOption.POINTS -> filtered.sortedWith(
                    compareByDescending<Member> { it.spendablePoints }
                        .thenBy { it.fullName.lowercase() },
                )
            }
        }
}

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val repository: MemberRepository,
    private val registerMember: RegisterMember,
    private val economyRepository: EconomyRepository,
    private val growthMapRepository: GrowthMapRepository,
    private val updateGrowthProgress: UpdateGrowthProgress,
    private val inventoryRepository: InventoryRepository,
    private val missionRepository: MissionRepository,
    private val catalogItemMediaRepository: CatalogItemMediaRepository,
    private val avatarRepository: MemberAvatarRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MembersUiState())
    val state = mutableState.asStateFlow()
    private var economyHistoryJob: Job? = null
    private var growthItemsJob: Job? = null
    private var growthProgressJob: Job? = null
    private var inventoryJob: Job? = null
    private var catalogJob: Job? = null
    private var avatarJob: Job? = null
    private var missionAssignmentsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeActiveMembers().collect { members ->
                mutableState.update { it.copy(members = members) }
                grantBirthdayDiamonds(members)
            }
        }
        viewModelScope.launch {
            repository.observeArchivedMembers().collect { members ->
                mutableState.update { it.copy(archivedMembers = members) }
            }
        }
        viewModelScope.launch {
            missionRepository.observeMissions().collect { missions ->
                mutableState.update { it.copy(missions = missions) }
                missionAssignmentsJob?.cancel()
                missionAssignmentsJob = viewModelScope.launch {
                    if (missions.isEmpty()) {
                        mutableState.update { it.copy(missionAssignments = emptyList()) }
                    } else {
                        kotlinx.coroutines.flow.combine(missions.map { missionRepository.observeAssignments(it.id) }) { rows -> rows.flatMap { it } }
                            .collect { assignments -> mutableState.update { it.copy(missionAssignments = assignments) } }
                    }
                }
            }
        }
    }

    private fun grantBirthdayDiamonds(members: List<Member>) {
        // Android ICU is unavailable in plain JVM unit tests; skip the
        // device-calendar check there while retaining it on Android.
        val today = runCatching {
            Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
        }.getOrNull() ?: return
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH) + 1
        val currentDay = today.get(Calendar.DATE)
        val now = System.currentTimeMillis()
        members.forEach { member ->
            val parts = member.birthDateJalali?.split("/") ?: return@forEach
            if (parts.size != 3) return@forEach
            val month = parts[1].toIntOrNull() ?: return@forEach
            val day = parts[2].toIntOrNull() ?: return@forEach
            if (month != currentMonth || day != currentDay) return@forEach
            val transaction = EconomyTransaction(
                id = "birthday-${member.id}-$currentYear",
                memberId = member.id,
                currency = EconomyCurrency.DIAMONDS,
                amountDelta = 30L,
                kind = EconomyTransactionKind.EARN,
                sourceType = "BIRTHDAY_REWARD",
                sourceId = currentYear.toString(),
                createdAtEpochMillis = now,
            )
            viewModelScope.launch {
                runCatching { economyRepository.appendAtomically(transaction) }
            }
        }
    }

    fun onFullNameChanged(value: String) = mutableState.update {
        it.copy(fullName = value, validationMessage = null, savedMessage = null)
    }
    fun onNicknameChanged(value: String) = mutableState.update { it.copy(nickname = value, savedMessage = null) }
    fun onBirthDateChanged(value: String) = mutableState.update { it.copy(birthDateJalali = value, validationMessage = null, savedMessage = null) }
    fun updateSelectedMemberProfile(fullName: String, nickname: String, birthDate: String) {
        val memberId = mutableState.value.selectedMemberId ?: return
        val name = fullName.trim()
        if (name.isBlank()) { mutableState.update { it.copy(profileMessage = "نام و نام خانوادگی الزامی است") }; return }
        mutableState.update { it.copy(profileMessage = null) }
        val normalizedDate = try { birthDate.trim().takeIf(String::isNotEmpty)?.let(::normalizeJalaliDate) }
        catch (_: Exception) { mutableState.update { it.copy(profileMessage = "تاریخ تولد شمسی معتبر نیست") }; return }
        viewModelScope.launch {
            try {
                repository.updateProfile(memberId, name, nickname.trim().takeIf(String::isNotEmpty), normalizedDate, System.currentTimeMillis())
                mutableState.update { it.copy(profileMessage = "مشخصات عضو ذخیره شد") }
            } catch (_: Exception) { mutableState.update { it.copy(profileMessage = "ذخیره مشخصات انجام نشد") } }
        }
    }

    fun activateBadge(level: Int) {
        val member = mutableState.value.members.firstOrNull { it.id == mutableState.value.selectedMemberId } ?: return
        val maxEarned = LevelProgression.levelFor(member.totalXp).coerceAtMost(RankBadgeProgression.all.size)
        if (level !in 1..maxEarned) { mutableState.update { it.copy(profileMessage = "این نشان هنوز با سطح عضو باز نشده است") }; return }
        viewModelScope.launch {
            runCatching { repository.updateActiveBadgeLevel(member.id, level, System.currentTimeMillis()) }
                .onSuccess { mutableState.update { it.copy(profileMessage = "نشان فعال عضو تغییر کرد") } }
                .onFailure { mutableState.update { it.copy(profileMessage = "تغییر نشان انجام نشد") } }
        }
    }

    fun updateSelectedMemberBirthDate(value: String) {
        val memberId = mutableState.value.selectedMemberId ?: return
        viewModelScope.launch {
            try {
                val normalized = value.trim().takeIf(String::isNotEmpty)?.let(::normalizeJalaliDate)
                repository.updateBirthDate(memberId, normalized, System.currentTimeMillis())
                mutableState.update { it.copy(profileMessage = "تاریخ تولد ذخیره شد") }
            } catch (_: Exception) {
                mutableState.update { it.copy(profileMessage = "تاریخ واردشده معتبر نیست یا ذخیره انجام نشد") }
            }
        }
    }
    fun adjustEconomyBalance(currency: EconomyCurrency, amountDelta: Long, reason: String) {
        val memberId = mutableState.value.selectedMemberId ?: return
        val normalizedReason = reason.trim()
        if (amountDelta == 0L || normalizedReason.isBlank()) {
            mutableState.update { it.copy(profileMessage = "مقدار اصلاح و دلیل آن را وارد کنید") }
            return
        }
        val transaction = EconomyTransaction(
            id = "coach-adjustment-${java.util.UUID.randomUUID()}",
            memberId = memberId,
            currency = currency,
            amountDelta = amountDelta,
            kind = EconomyTransactionKind.CORRECTION,
            sourceType = "COACH_ADJUSTMENT",
            sourceId = normalizedReason,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            runCatching { economyRepository.appendAtomically(transaction) }
                .onSuccess { result ->
                    val message = when (result) {
                        com.kichikan.negah.domain.economy.LedgerAppendResult.APPENDED -> "اصلاح موجودی ثبت شد"
                        com.kichikan.negah.domain.economy.LedgerAppendResult.INSUFFICIENT_BALANCE -> "موجودی پس از کاهش نمی‌تواند منفی شود"
                        com.kichikan.negah.domain.economy.LedgerAppendResult.BALANCE_OVERFLOW -> "مقدار واردشده بیش از حد مجاز است"
                    }
                    mutableState.update { it.copy(profileMessage = message) }
                }
                .onFailure { mutableState.update { it.copy(profileMessage = "ثبت اصلاح موجودی انجام نشد") } }
        }
    }
    fun onSearchChanged(value: String) = mutableState.update { it.copy(searchQuery = value) }
    fun onSortChanged(value: MemberSortOption) = mutableState.update { it.copy(sortOption = value) }
    fun onHistoryFilterChanged(value: MemberHistoryFilter) = mutableState.update { it.copy(historyFilter = value) }
    fun showArchivedMembers(show: Boolean) = mutableState.update { it.copy(showingArchived = show, searchQuery = "") }
    fun archiveMember(memberId: String) {
        viewModelScope.launch {
            runCatching { repository.archive(memberId) }
                .onSuccess { mutableState.update { it.copy(savedMessage = "عضو به بایگانی منتقل شد") } }
                .onFailure { mutableState.update { it.copy(validationMessage = "بایگانی عضو انجام نشد") } }
        }
    }
    fun restoreMember(memberId: String) {
        viewModelScope.launch {
            runCatching { repository.restore(memberId) }
                .onSuccess { mutableState.update { it.copy(savedMessage = "عضو بازیابی شد") } }
                .onFailure { mutableState.update { it.copy(validationMessage = "بازیابی عضو انجام نشد") } }
        }
    }
    fun onMemberSelected(memberId: String) {
        economyHistoryJob?.cancel()
        growthItemsJob?.cancel()
        growthProgressJob?.cancel()
        inventoryJob?.cancel()
        catalogJob?.cancel()
        avatarJob?.cancel()
        mutableState.update { it.copy(selectedMemberId = memberId, economyTransactions = emptyList(), growthItems = emptyList(), growthProgress = emptyList(), growthSaveMessage = null, inventoryEntries = emptyList(), availableCatalogItems = emptyList(), activeInventoryItems = emptyList(), inventoryMessage = null, avatarBytes = null, avatarMessage = null, catalogImageBytes = emptyMap()) }
        avatarJob = viewModelScope.launch {
            val bytes = runCatching { avatarRepository.loadAvatar(memberId) }.getOrNull()
            mutableState.update { current ->
                if (current.selectedMemberId == memberId) current.copy(avatarBytes = bytes) else current
            }
        }
        economyHistoryJob = viewModelScope.launch {
            economyRepository.observeForMember(memberId).collect { transactions ->
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(economyTransactions = transactions)
                    else current
                }
            }
        }
        growthItemsJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                growthMapRepository.observeItems(GrowthTrack.CAPABILITY),
                growthMapRepository.observeItems(GrowthTrack.SELF_IMPROVEMENT),
            ) { capabilities, selfImprovement -> capabilities + selfImprovement }
                .collect { items -> mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(growthItems = items) else current
                } }
        }
        growthProgressJob = viewModelScope.launch {
            growthMapRepository.observeProgress(memberId).collect { progress ->
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(growthProgress = progress) else current
                }
            }
        }
        inventoryJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                inventoryRepository.observeMemberInventory(memberId, InventoryCategory.AVATAR),
                inventoryRepository.observeMemberInventory(memberId, InventoryCategory.FRAME),
                inventoryRepository.observeMemberInventory(memberId, InventoryCategory.REWARD),
                inventoryRepository.observeActiveItems(memberId),
            ) { avatars, frames, rewards, active ->
                (avatars + frames + rewards) to active
            }.collect { (entries, active) ->
                val catalogImages = buildMap {
                    active.filter { it.category == InventoryCategory.AVATAR || it.category == InventoryCategory.FRAME }
                        .distinctBy { it.catalogItemId }.forEach { item ->
                            runCatching { catalogItemMediaRepository.loadCatalogImage(item.catalogItemId) }.getOrNull()?.let { put(item.catalogItemId, it) }
                        }
                }
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(
                        inventoryEntries = entries,
                        activeInventoryItems = active,
                        catalogImageBytes = catalogImages,
                    ) else current
                }
            }
        }

        catalogJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                inventoryRepository.observeCatalog(InventoryCategory.AVATAR),
                inventoryRepository.observeCatalog(InventoryCategory.FRAME),
                inventoryRepository.observeCatalog(InventoryCategory.REWARD),
            ) { avatars, frames, rewards -> avatars + frames + rewards }
                .collect { catalog ->
                    mutableState.update { current ->
                        if (current.selectedMemberId == memberId) current.copy(availableCatalogItems = catalog)
                        else current
                    }
                }
        }
    }


    fun removeMemberAvatar() {
        val memberId = mutableState.value.selectedMemberId ?: return
        avatarJob?.cancel()
        avatarJob = viewModelScope.launch {
            try {
                avatarRepository.removeAvatar(memberId)
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(
                        avatarBytes = null,
                        avatarMessage = "تصویر پروفایل حذف شد",
                    ) else current
                }
            } catch (_: Exception) {
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(
                        avatarMessage = "حذف تصویر انجام نشد",
                    ) else current
                }
            }
        }
    }

    fun updateMemberAvatar(input: InputStream, mimeType: String) {
        val memberId = mutableState.value.selectedMemberId
        if (memberId == null) {
            input.close()
            return
        }
        avatarJob?.cancel()
        avatarJob = viewModelScope.launch {
            try {
                avatarRepository.saveAvatar(memberId, input, mimeType)
                val bytes = avatarRepository.loadAvatar(memberId)
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(
                        avatarBytes = bytes,
                        avatarMessage = "تصویر پروفایل ذخیره شد",
                    ) else current
                }
            } catch (cancelled: CancellationException) {
                runCatching { input.close() }
                throw cancelled
            } catch (_: Exception) {
                runCatching { input.close() }
                mutableState.update { current ->
                    if (current.selectedMemberId == memberId) current.copy(
                        avatarMessage = "ذخیره تصویر انجام نشد؛ فایل دیگری انتخاب کنید",
                    ) else current
                }
            }
        }
    }

    fun onProfileClosed() {
        avatarJob?.cancel()
        economyHistoryJob?.cancel()
        growthItemsJob?.cancel()
        growthProgressJob?.cancel()
        inventoryJob?.cancel()
        catalogJob?.cancel()
        economyHistoryJob = null
        growthItemsJob = null
        growthProgressJob = null
        inventoryJob = null
        catalogJob = null
        avatarJob = null
        mutableState.update { it.copy(selectedMemberId = null, economyTransactions = emptyList(), growthItems = emptyList(), growthProgress = emptyList(), growthSaveMessage = null, inventoryEntries = emptyList(), availableCatalogItems = emptyList(), activeInventoryItems = emptyList(), inventoryMessage = null, avatarBytes = null, avatarMessage = null, catalogImageBytes = emptyMap()) }
    }

    fun saveGrowthProgress(item: GrowthItem, progressPercent: Int, note: String?) {
        val memberId = mutableState.value.selectedMemberId ?: return
        viewModelScope.launch {
            try {
                when (updateGrowthProgress(MemberGrowthProgress(
                    memberId = memberId,
                    growthItemId = item.id,
                    progressPercent = progressPercent,
                    note = note?.trim()?.takeIf(String::isNotEmpty),
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ))) {
                    UpdateGrowthProgressResult.Saved -> mutableState.update { it.copy(growthSaveMessage = "پیشرفت ذخیره شد") }
                    UpdateGrowthProgressResult.Invalid -> mutableState.update { it.copy(growthSaveMessage = "مقدار پیشرفت معتبر نیست") }
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(growthSaveMessage = "ذخیره پیشرفت انجام نشد؛ دوباره تلاش کنید") }
            }
        }
    }

    fun activateInventoryItem(entry: MemberInventoryEntry) {
        viewModelScope.launch {
            try {
                val activated = inventoryRepository.activateItem(
                    memberId = entry.memberId,
                    category = entry.category,
                    catalogItemId = entry.catalogItemId,
                    nowEpochMillis = System.currentTimeMillis(),
                )
                mutableState.update { it.copy(inventoryMessage = if (activated) "آیتم فعال شد" else "این آیتم در گنجینه عضو نیست") }
            } catch (_: Exception) {
                mutableState.update { it.copy(inventoryMessage = "فعال‌سازی انجام نشد؛ دوباره تلاش کنید") }
            }
        }
    }

    fun grantCatalogItem(item: CatalogItem) {
        val memberId = mutableState.value.selectedMemberId ?: return
        viewModelScope.launch {
            try {
                inventoryRepository.grantItem(
                    MemberInventoryEntry(
                        memberId = memberId,
                        catalogItemId = item.id,
                        category = item.category,
                        title = item.title,
                        description = item.description,
                        acquiredAtEpochMillis = System.currentTimeMillis(),
                        sourceType = "COACH_GRANT",
                        sourceId = java.util.UUID.randomUUID().toString(),
                    ),
                )
                mutableState.update { it.copy(inventoryMessage = "آیتم به گنجینه عضو افزوده شد") }
            } catch (_: Exception) {
                mutableState.update { it.copy(inventoryMessage = "این آیتم قبلاً به عضو داده شده یا ثبت آن انجام نشد") }
            }
        }
    }

    fun addMember() {
        val snapshot = mutableState.value
        if (snapshot.isSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, validationMessage = null, savedMessage = null) }
            try {
                when (registerMember(snapshot.fullName, snapshot.nickname, System.currentTimeMillis(), snapshot.birthDateJalali.takeIf(String::isNotBlank)?.let(::normalizeJalaliDate))) {
                    RegisterMemberResult.InvalidName -> mutableState.update {
                        it.copy(isSaving = false, validationMessage = "نام و نام خانوادگی را وارد کنید")
                    }
                    is RegisterMemberResult.Saved -> mutableState.update {
                        it.copy(fullName = "", nickname = "", birthDateJalali = "", isSaving = false, savedMessage = "عضو جدید ثبت شد")
                    }
                }
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(isSaving = false, validationMessage = "ثبت عضو انجام نشد؛ دوباره تلاش کنید")
                }
            }
        }
    }
}

private fun normalizeJalaliDate(value: String): String {
    val digits = value.map { char ->
        when (char) {
            in '۰'..'۹' -> '0' + (char - '۰')
            in '٠'..'٩' -> '0' + (char - '٠')
            else -> char
        }
    }.joinToString("")
    val parts = digits.split("/", "-")
    require(parts.size == 3) { "تاریخ تولد باید به شکل سال/ماه/روز باشد" }
    val year = parts[0].toIntOrNull() ?: error("سال تولد نامعتبر است")
    val month = parts[1].toIntOrNull() ?: error("ماه تولد نامعتبر است")
    val day = parts[2].toIntOrNull() ?: error("روز تولد نامعتبر است")
    require(year in 1200..1600 && month in 1..12 && day in 1..31) { "تاریخ تولد شمسی معتبر نیست" }
    val calendar = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
    calendar.isLenient = false
    calendar.clear()
    calendar.set(year, month - 1, day)
    calendar.timeInMillis // Force strict calendar validation (including month length and leap years).
    return "%04d/%02d/%02d".format(java.util.Locale.ROOT, year, month, day)
}

@Composable
fun MembersScreen(viewModel: MembersViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedMember = state.members.firstOrNull { it.id == state.selectedMemberId }
    if (selectedMember != null) {
        MemberProfileScreen(selectedMember, state.economyTransactions, state.historyFilter, viewModel::onHistoryFilterChanged, state.growthItems, state.growthProgress, state.growthSaveMessage, state.inventoryEntries, state.availableCatalogItems, state.activeInventoryItems, state.inventoryMessage, state.profileMessage, state.avatarBytes, state.avatarMessage, state.catalogImageBytes, state.missions, state.missionAssignments, viewModel::updateSelectedMemberProfile, viewModel::activateBadge, viewModel::adjustEconomyBalance, viewModel::updateMemberAvatar, viewModel::removeMemberAvatar, viewModel::saveGrowthProgress, viewModel::activateInventoryItem, viewModel::grantCatalogItem, viewModel::onProfileClosed, modifier)
        return
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var memberPendingArchive by rememberSaveable { mutableStateOf<Member?>(null) }
    LaunchedEffect(state.savedMessage) { if (state.savedMessage == "عضو جدید ثبت شد") showAddDialog = false }
    val totalXp = state.members.sumOf { it.totalXp }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("اعضا", style = MaterialTheme.typography.headlineMedium)
                Text("پرونده و مسیر رشد اعضای حلقه", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${if (state.showingArchived) state.archivedMembers.size else state.members.size} عضو", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }

        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.members.size.toString(), style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("عضو", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(totalXp.toString(), style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Text("مجموع XP", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !state.showingArchived, onClick = { viewModel.showArchivedMembers(false) }, label = { Text("اعضای فعال") })
            FilterChip(selected = state.showingArchived, onClick = { viewModel.showArchivedMembers(true) }, label = { Text("بایگانی ${state.archivedMembers.size}") })
        }

        OutlinedTextField(value = state.searchQuery, onValueChange = viewModel::onSearchChanged,
            label = { Text("جست‌وجوی نام عضو") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemberSortOption.entries.forEach { option ->
                FilterChip(selected = state.sortOption == option,
                    onClick = { viewModel.onSortChanged(option) },
                    label = { Text(when (option) {
                        MemberSortOption.LEVEL -> "رتبه و سطح"
                        MemberSortOption.POINTS -> "امتیاز قابل خرج"
                        MemberSortOption.NAME -> "نام"
                    }) })
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("فهرست اعضا", style = MaterialTheme.typography.titleMedium)
            Text("${state.visibleMembers.size} نتیجه", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.visibleMembers.isEmpty()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.showingArchived) "عضو بایگانی‌شده‌ای وجود ندارد" else if (state.members.isEmpty()) "هنوز عضوی ثبت نشده است" else "عضوی با این جست‌وجو پیدا نشد",
                    style = MaterialTheme.typography.titleMedium)
                Text(if (state.showingArchived) "اعضای بایگانی‌شده از این بخش قابل بازیابی‌اند" else "برای شروع، عضو جدیدی به حلقه اضافه کن", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(state.visibleMembers, key = { it.id }) { member ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(member.fullName.trim().firstOrNull()?.toString() ?: "؟",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(modifier = Modifier.weight(1f).clickable { if (!state.showingArchived) viewModel.onMemberSelected(member.id) }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    buildString { append(member.fullName); member.nickname?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) } },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                )
                                Text(
                                    "سطح ${LevelProgression.levelFor(member.totalXp)} · ${member.totalXp} XP · ${member.spendablePoints} امتیاز · ${member.diamonds} الماس",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.showingArchived) {
                                TextButton(onClick = { viewModel.restoreMember(member.id) }) { Text("بازیابی") }
                            } else {
                                TextButton(onClick = { memberPendingArchive = member }) { Text("بایگانی") }
                            }
                        }
                    }
                }
            }
        }

        if (!state.showingArchived) Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("افزودن عضو  +")
        }
        state.savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall) }
        state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
    }

    memberPendingArchive?.let { member ->
        AlertDialog(
            onDismissRequest = { memberPendingArchive = null },
            title = { Text("بایگانی عضو") },
            text = { Text("«${member.fullName}» از فهرست فعال خارج می‌شود؛ سوابق و اطلاعات او حذف نمی‌شوند و بعداً قابل بازیابی‌اند.") },
            confirmButton = { Button(onClick = { viewModel.archiveMember(member.id); memberPendingArchive = null }) { Text("تأیید بایگانی") } },
            dismissButton = { TextButton(onClick = { memberPendingArchive = null }) { Text("انصراف") } },
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("افزودن عضو") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = state.fullName, onValueChange = viewModel::onFullNameChanged,
                        label = { Text("نام و نام خانوادگی") }, singleLine = true)
                    OutlinedTextField(value = state.nickname, onValueChange = viewModel::onNicknameChanged,
                        label = { Text("نام نمایشی (اختیاری)") }, singleLine = true)
                    OutlinedTextField(value = state.birthDateJalali, onValueChange = viewModel::onBirthDateChanged,
                        label = { Text("تاریخ تولد شمسی (اختیاری)") },
                        placeholder = { Text("۱۴۰۰/۰۵/۲۱") }, singleLine = true,
                        supportingText = { Text("سال/ماه/روز؛ پاداش تولد سالانه ۳۰ الماس است") })
                    state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addMember() }, enabled = !state.isSaving) {
                    Text(if (state.isSaving) "در حال ثبت..." else "ثبت عضو")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun MemberProfileScreen(member: Member, transactions: List<EconomyTransaction>, historyFilter: MemberHistoryFilter, onHistoryFilterChanged: (MemberHistoryFilter) -> Unit, growthItems: List<GrowthItem>, growthProgress: List<MemberGrowthProgress>, growthSaveMessage: String?, inventoryEntries: List<MemberInventoryEntry>, availableCatalogItems: List<CatalogItem>, activeInventoryItems: List<ActiveInventoryItem>, inventoryMessage: String?, profileMessage: String?, avatarBytes: ByteArray?, avatarMessage: String?, catalogImageBytes: Map<String, ByteArray>, missions: List<Mission>, missionAssignments: List<MissionAssignment>, onUpdateProfile: (String, String, String) -> Unit, onActivateBadge: (Int) -> Unit, onAdjustEconomy: (EconomyCurrency, Long, String) -> Unit, onUpdateAvatar: (InputStream, String) -> Unit, onRemoveAvatar: () -> Unit, onSaveGrowthProgress: (GrowthItem, Int, String?) -> Unit, onActivateInventoryItem: (MemberInventoryEntry) -> Unit, onGrantCatalogItem: (CatalogItem) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var inventoryTab by rememberSaveable { mutableStateOf("آواتار") }
    var showAllReceivedHistory by rememberSaveable { mutableStateOf(false) }
    var showProfileEditDialog by rememberSaveable { mutableStateOf(false) }
    var profileNameDraft by rememberSaveable(member.id) { mutableStateOf(member.fullName) }
    var profileNicknameDraft by rememberSaveable(member.id) { mutableStateOf(member.nickname.orEmpty()) }
    var birthDateDraft by rememberSaveable(member.id) { mutableStateOf(member.birthDateJalali.orEmpty()) }
    var showEconomyAdjustmentDialog by rememberSaveable { mutableStateOf(false) }
    var adjustmentCurrency by rememberSaveable { mutableStateOf(EconomyCurrency.XP) }
    var adjustmentAmountDraft by rememberSaveable { mutableStateOf("") }
    var adjustmentReasonDraft by rememberSaveable { mutableStateOf("") }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var exportIdentity by rememberSaveable { mutableStateOf(true) }
    var exportTreasure by rememberSaveable { mutableStateOf(true) }
    var exportGrowth by rememberSaveable { mutableStateOf(true) }
    var exportReport by rememberSaveable { mutableStateOf(true) }
    var exportHistory by rememberSaveable { mutableStateOf(true) }
    var pendingPdfSections by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    val context = LocalContext.current
    val activeAvatarId = activeInventoryItems.firstOrNull { it.category == InventoryCategory.AVATAR }?.catalogItemId
    val activeFrameId = activeInventoryItems.firstOrNull { it.category == InventoryCategory.FRAME }?.catalogItemId
    val activeAvatarBytes = activeAvatarId?.let(catalogImageBytes::get)
    val activeFrameBytes = activeFrameId?.let(catalogImageBytes::get)
    LaunchedEffect(profileMessage) { if (profileMessage == "مشخصات عضو ذخیره شد") showProfileEditDialog = false }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val pdf = createMemberPdf(member.fullName, pendingPdfSections, avatarBytes, activeAvatarBytes, activeFrameBytes)
            runCatching { context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) } }
            pdf.close()
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            val input = context.contentResolver.openInputStream(uri)
            if (mimeType != null && input != null) onUpdateAvatar(input, mimeType)
            else input?.close()
        }
    }
    val avatarBitmap = remember(avatarBytes) {
        avatarBytes?.let { bytes ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > 512 || bounds.outHeight / sampleSize > 512) sampleSize *= 2
            BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            )?.asImageBitmap()
        }
    }
    val activeAvatarBitmap = remember(activeAvatarBytes) {
        activeAvatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val activeFrameBitmap = remember(activeFrameBytes) {
        activeFrameBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val level = LevelProgression.levelFor(member.totalXp)
    val currentLevelXp = LevelProgression.minimumXpForLevel(level)
    val nextLevelXp = LevelProgression.minimumXpForLevel(level + 1)
    val progress = ((member.totalXp - currentLevelXp).coerceAtLeast(0L).toFloat() /
        (nextLevelXp - currentLevelXp).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val parsedAdjustmentAmount = adjustmentAmountDraft.trim()
        .map { digit -> when (digit) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
            '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
            '−' -> '-'
            else -> digit
        } }.joinToString("").toLongOrNull()
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(member.fullName, style = MaterialTheme.typography.titleLarge)
                Text("پرونده عضو حلقه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("بازگشت") }
        }
        ProfileSection("اطلاعات عضو", "مشخصات پایه") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "تصویر پروفایل ${member.fullName}",
                        modifier = Modifier.size(76.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.size(76.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("تصویر ندارد", style = MaterialTheme.typography.labelSmall) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { avatarPicker.launch("image/*") }) { Text("انتخاب تصویر") }
                    if (avatarBitmap != null) {
                        TextButton(onClick = onRemoveAvatar) { Text("حذف تصویر") }
                    }
                    avatarMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("نام نمایشی: ${member.nickname ?: "ثبت نشده"}", style = MaterialTheme.typography.bodyMedium)
            Text("تاریخ تولد شمسی: ${member.birthDateJalali ?: "ثبت نشده"}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { profileNameDraft = member.fullName; profileNicknameDraft = member.nickname.orEmpty(); birthDateDraft = member.birthDateJalali.orEmpty(); showProfileEditDialog = true }) { Text("ویرایش مشخصات عضو") }
            profileMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(member.fullName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        member.nickname?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Text("سطح " + level, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        RankBadgeProgression.currentForLevel(level)?.let { badge ->
                            Text("درجه ${badge.title}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                        if (activeAvatarBitmap != null) {
                            Image(bitmap = activeAvatarBitmap, contentDescription = "آواتار فعال عضو",
                                modifier = Modifier.size(76.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.size(76.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                Text(level.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        activeFrameBitmap?.let {
                            Image(bitmap = it, contentDescription = "قاب فعال عضو",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                    }
                }
                Text(member.totalXp.toString() + " XP", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text((nextLevelXp - member.totalXp).coerceAtLeast(0L).toString() + " XP تا سطح بعد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileWalletCard("XP", member.totalXp.toString(), "غیرقابل خرج", Modifier.weight(1f))
            ProfileWalletCard("امتیاز", member.spendablePoints.toString(), "قابل خرج", Modifier.weight(1f))
            ProfileWalletCard("الماس", member.diamonds.toString(), "قابل خرج", Modifier.weight(1f))
        }
        TextButton(onClick = {
            adjustmentCurrency = EconomyCurrency.XP
            adjustmentAmountDraft = ""
            adjustmentReasonDraft = ""
            showEconomyAdjustmentDialog = true
        }) { Text("اصلاح XP، امتیاز یا الماس") }
        ProfileSection("نقشه کمال", "دو مسیر مستقل رشد عضو") {
            GrowthTrackSection("توانمندی", growthItems.filter { it.track == GrowthTrack.CAPABILITY }, growthProgress, onSaveGrowthProgress)
            GrowthTrackSection("خودسازی", growthItems.filter { it.track == GrowthTrack.SELF_IMPROVEMENT }, growthProgress, onSaveGrowthProgress)
            growthSaveMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
        ProfileSection("گنجینه", "موجودی و آیتم‌های عضو") {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("آواتار", "قاب", "نشان", "جایزه").forEach { tab ->
                    FilterChip(selected = inventoryTab == tab, onClick = { inventoryTab = tab }, label = { Text(tab) })
                }
            }
            if (inventoryTab == "نشان") {
                Text("این نشان‌ها نماد پیشرفت در نگاه‌اند، نه درجه یا ترفیع واقعی. با هر سطح یک نشان باز می‌شود؛ خرید و اعطای دستی ندارند.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RankBadgeProgression.all.forEach { badge ->
                    val earned = level >= badge.level
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (earned) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        )) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(38.dp).background(
                                if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape,
                            ), contentAlignment = Alignment.Center) {
                                Text(badge.level.toString(), color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(badge.title, style = MaterialTheme.typography.titleSmall)
                                Text("سطح ${badge.level}", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val activeBadge = effectiveActiveBadgeLevel(member, level)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(when {
                                    !earned -> "قفل"
                                    badge.level == activeBadge -> "نشان فعال"
                                    else -> "دریافت‌شده"
                                }, style = MaterialTheme.typography.labelSmall,
                                    color = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (earned && badge.level != activeBadge) TextButton(onClick = { onActivateBadge(badge.level) }) { Text("فعال‌سازی") }
                            }
                        }
                    }
                }
            } else {
                val category = when (inventoryTab) {
                    "آواتار" -> InventoryCategory.AVATAR
                    "قاب" -> InventoryCategory.FRAME
                    else -> InventoryCategory.REWARD
                }
                val visibleEntries = inventoryEntries.filter { it.category == category }
                if (visibleEntries.isEmpty()) {
                    Text("هنوز آیتمی در این بخش برای عضو ثبت نشده است", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    visibleEntries.forEach { entry ->
                        val isActive = activeInventoryItems.any { it.category == category && it.catalogItemId == entry.catalogItemId }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall)
                            entry.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            when {
                                category == InventoryCategory.REWARD -> Text("دریافت‌شده", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                isActive -> Text("فعال", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                else -> TextButton(onClick = { onActivateInventoryItem(entry) }) { Text("انتخاب به‌عنوان فعال") }
                            }
                        }
                    }
                }
                val grantableItems = availableCatalogItems.filter {
                    it.category == category && visibleEntries.none { owned -> owned.catalogItemId == it.id }
                }
                if (grantableItems.isNotEmpty()) {
                    Text("آیتم‌های قابل اعطا", style = MaterialTheme.typography.titleSmall)
                    grantableItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium)
                                item.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            TextButton(onClick = { onGrantCatalogItem(item) }) { Text("اعطا") }
                        }
                    }
                }
            }
            inventoryMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
        MemberReportCard(
            member = member,
            transactions = transactions,
            growthItems = growthItems,
            growthProgress = growthProgress,
            missions = missions,
            missionAssignments = missionAssignments,
        )
        val historyTransactions = transactions.filter { transaction ->
            when (historyFilter) {
                MemberHistoryFilter.ALL -> true
                MemberHistoryFilter.XP -> transaction.currency == EconomyCurrency.XP
                MemberHistoryFilter.POINTS -> transaction.currency == EconomyCurrency.POINTS
                MemberHistoryFilter.DIAMONDS -> transaction.currency == EconomyCurrency.DIAMONDS
                MemberHistoryFilter.ITEMS -> false
            }
        }
        val receivedHistoryRows = buildList {
            historyTransactions.forEach { transaction ->
                val currencyLabel = when (transaction.currency) {
                    EconomyCurrency.XP -> "XP"
                    EconomyCurrency.POINTS -> "اعتبار"
                    EconomyCurrency.DIAMONDS -> "الماس"
                }
                val kindLabel = when (transaction.kind) {
                    EconomyTransactionKind.EARN -> "دریافت"
                    EconomyTransactionKind.SPEND -> "خرج"
                    EconomyTransactionKind.CORRECTION -> "اصلاح"
                    EconomyTransactionKind.REFUND -> "بازگشت"
                }
                val sourceLabel = when (transaction.sourceType) {
                    "BIRTHDAY_REWARD" -> "پاداش تولد"
                    "COACH_ADJUSTMENT" -> "اصلاح مربی: ${transaction.sourceId.orEmpty()}"
                    "STORE_PURCHASE" -> "خرید از فروشگاه"
                    "MISSION_REWARD" -> "پاداش مأموریت"
                    else -> "ثبت‌شده در دفتر اقتصادی"
                }
                add(ReceivedHistoryRow(
                    id = transaction.id,
                    timestamp = transaction.createdAtEpochMillis,
                    title = "$kindLabel $currencyLabel",
                    detail = sourceLabel,
                    amount = if (transaction.amountDelta > 0) "+${transaction.amountDelta}" else transaction.amountDelta.toString(),
                    isPositive = transaction.amountDelta > 0,
                ))
            }
            if (historyFilter == MemberHistoryFilter.ALL || historyFilter == MemberHistoryFilter.ITEMS) {
                inventoryEntries.forEach { entry ->
                    val sourceLabel = when (entry.sourceType) {
                        "PURCHASE", "STORE_PURCHASE" -> "خرید از فروشگاه"
                        "COACH_GRANT" -> "اعطای مربی"
                        else -> "دریافت آیتم"
                    }
                    add(ReceivedHistoryRow(
                        id = "${entry.memberId}:${entry.catalogItemId}",
                        timestamp = entry.acquiredAtEpochMillis,
                        title = entry.title,
                        detail = "$sourceLabel • ${when (entry.category) {
                            InventoryCategory.AVATAR -> "آواتار"
                            InventoryCategory.FRAME -> "قاب"
                            InventoryCategory.REWARD -> "جایزه"
                        }}",
                    ))
                }
                val earnedBadgeTimes = mutableMapOf<Int, Long>()
                var xpBalanceAtTime = 0L
                transactions.filter { it.currency == EconomyCurrency.XP }
                    .sortedBy { it.createdAtEpochMillis }
                    .forEach { transaction ->
                        val previousXp = xpBalanceAtTime
                        xpBalanceAtTime = Math.addExact(xpBalanceAtTime, transaction.amountDelta).coerceAtLeast(0L)
                        RankBadgeProgression.all.forEach { badge ->
                            val threshold = LevelProgression.minimumXpForLevel(badge.level)
                            if (previousXp < threshold && xpBalanceAtTime >= threshold) {
                                earnedBadgeTimes.putIfAbsent(badge.level, transaction.createdAtEpochMillis)
                            }
                        }
                    }
                RankBadgeProgression.earnedForLevel(level).forEach { badge ->
                    earnedBadgeTimes[badge.level]?.let { earnedAt ->
                        add(ReceivedHistoryRow(
                            id = "rank-badge:${badge.level}",
                            timestamp = earnedAt,
                            title = badge.title,
                            detail = "نشان خودکار سطح ${badge.level}",
                        ))
                    }
                }
            }
        }.sortedByDescending { it.timestamp }
        Button(onClick = { showExportDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("خروجی پرونده عضو (PDF)") }
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("انتخاب بخش‌های خروجی پرونده") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("اطلاعات هویتی" to exportIdentity, "گنجینه" to exportTreasure, "نقشه کمال" to exportGrowth, "کارنامه" to exportReport, "سوابق دریافتی" to exportHistory).forEach { (label, checked) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = { value ->
                                    when (label) {
                                        "اطلاعات هویتی" -> exportIdentity = value
                                        "گنجینه" -> exportTreasure = value
                                        "نقشه کمال" -> exportGrowth = value
                                        "کارنامه" -> exportReport = value
                                        "سوابق دریافتی" -> exportHistory = value
                                    }
                                })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val reportCalendar = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
                        val reportYear = reportCalendar.get(Calendar.YEAR)
                        val reportMonth = reportCalendar.get(Calendar.MONTH) + 1
                        val reportableMissionIds = missions.filter { it.kind != MissionKind.GROUP_PUZZLE }.map { it.id }.toSet()
                        val reportAssignments = missionAssignments.filter { assignment ->
                            if (assignment.missionId !in reportableMissionIds) return@filter false
                            val date = Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply { timeInMillis = assignment.updatedAtEpochMillis }
                            assignment.memberId == member.id && date.get(Calendar.YEAR) == reportYear && date.get(Calendar.MONTH) + 1 == reportMonth
                        }
                        val reportCompletedMissions = reportAssignments.count { it.status == MissionAssignmentStatus.COMPLETED || it.status == MissionAssignmentStatus.APPROVED }
                        val reportApprovedMissions = reportAssignments.count { it.status == MissionAssignmentStatus.APPROVED }
                        val reportGrowthAverage = if (growthItems.isEmpty()) null else growthItems.map { item ->
                            growthProgress.firstOrNull { it.growthItemId == item.id }?.progressPercent ?: 0
                        }.average().toInt()
                        val reportSummary = calculateJalaliMemberReport(transactions, reportYear, reportMonth, true)
                        val reportLevel = LevelProgression.levelFor(member.totalXp)
                        pendingPdfSections = buildList {
                            if (exportIdentity) add("اطلاعات هویتی" to "نام: ${member.fullName}\nنام نمایشی: ${member.nickname ?: "ثبت نشده"}\nتاریخ تولد: ${member.birthDateJalali ?: "ثبت نشده"}\nسطح: $level\nنشان فعال: ${effectiveActiveBadgeLevel(member, level)?.let { RankBadgeProgression.all.getOrNull(it - 1)?.title } ?: "ندارد"}\nXP: ${member.totalXp}\nامتیاز قابل خرج: ${member.spendablePoints}\nالماس: ${member.diamonds}")
                            if (exportTreasure) add("گنجینه" to (inventoryEntries.joinToString("\n") { entry ->
                                val active = activeInventoryItems.any { it.category == entry.category && it.catalogItemId == entry.catalogItemId }
                                "${entry.title} · ${entry.category} · ${if (active) "فعال" else "دریافت‌شده"}"
                            }.ifBlank { "آیتمی ثبت نشده است" } + "\nنشان‌های بازشده: " + RankBadgeProgression.earnedForLevel(level).joinToString("، ") { it.title }))
                            if (exportGrowth) add("نقشه کمال" to growthItems.joinToString("\n") { item ->
                                val saved = growthProgress.firstOrNull { it.growthItemId == item.id }
                                "${item.title}: ${saved?.progressPercent ?: 0}٪ · ${saved?.note.orEmpty()}"
                            }.ifBlank { "شاخص فعالی ثبت نشده است" })
                            if (exportReport) add("کارنامه · ${reportYear}/${reportMonth.toString().padStart(2, '0')}" to """
مأموریت: $reportCompletedMissions از ${reportAssignments.size} تکمیل‌شده، $reportApprovedMissions تأییدشده
امتیاز قابل خرج: ${member.spendablePoints} · تغییر بازه: ${formatReportDelta(reportSummary.pointsDelta)}
نقشه کمال: ${reportGrowthAverage?.let { "$it٪ وضعیت فعلی" } ?: "بدون شاخص فعال"} · روند تاریخی ذخیره نشده
کتاب‌خوانی: داده‌ای ثبت نشده
سطح: $reportLevel · ${member.totalXp} XP · تغییر بازه: ${formatReportDelta(reportSummary.xpDelta)}
الماس: ${member.diamonds} · تغییر بازه: ${formatReportDelta(reportSummary.diamondsDelta)}
""")
                            if (exportHistory) add("سوابق دریافتی" to receivedHistoryRows.joinToString("\n\n") { row ->
                                "${row.title} · ${row.detail} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("fa", "IR")).format(Date(row.timestamp))} · ${row.amount.orEmpty()}"
                            }.ifBlank { "سابقه‌ای ثبت نشده است" })
                        }
                        showExportDialog = false
                        pdfLauncher.launch("پرونده-${member.fullName}.pdf")
                    }, enabled = listOf(exportIdentity, exportTreasure, exportGrowth, exportReport, exportHistory).any { it }) { Text("ساخت PDF") }
                },
                dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("انصراف") } },
            )
        }
        ProfileSection("سوابق دریافتی", "پاداش‌ها، تراکنش‌ها و آیتم‌های دریافت‌شده") {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MemberHistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = historyFilter == filter,
                        onClick = { onHistoryFilterChanged(filter); showAllReceivedHistory = false },
                        label = { Text(when (filter) {
                            MemberHistoryFilter.ALL -> "همه"
                            MemberHistoryFilter.XP -> "XP"
                            MemberHistoryFilter.POINTS -> "امتیاز"
                            MemberHistoryFilter.DIAMONDS -> "الماس"
                            MemberHistoryFilter.ITEMS -> "آیتم‌ها"
                        }) },
                    )
                }
            }
            if (receivedHistoryRows.isEmpty()) {
                Text("برای این فیلتر سابقه‌ای ثبت نشده است", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                receivedHistoryRows.take(if (showAllReceivedHistory) receivedHistoryRows.size else 3)
                    .groupBy { receivedHistorySolarMonth(it.timestamp) }
                    .forEach { (period, rows) ->
                        Text(period, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        rows.forEach { ReceivedHistoryRowCard(it) }
                    }
                if (receivedHistoryRows.size > 3) {
                    TextButton(onClick = { showAllReceivedHistory = !showAllReceivedHistory }) {
                        Text(if (showAllReceivedHistory) "نمایش کمتر" else "همه")
                    }
                }
            }
        }
        if (showEconomyAdjustmentDialog) {
            AlertDialog(
                onDismissRequest = { showEconomyAdjustmentDialog = false },
                title = { Text("اصلاح موجودی شناسنامه") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("واحد موردنظر را انتخاب کن")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(EconomyCurrency.XP, EconomyCurrency.POINTS, EconomyCurrency.DIAMONDS).forEach { currency ->
                                FilterChip(selected = adjustmentCurrency == currency, onClick = { adjustmentCurrency = currency }, label = {
                                    Text(when (currency) {
                                        EconomyCurrency.XP -> "XP"
                                        EconomyCurrency.POINTS -> "امتیاز"
                                        EconomyCurrency.DIAMONDS -> "الماس"
                                    })
                                })
                            }
                        }
                        OutlinedTextField(value = adjustmentAmountDraft, onValueChange = { adjustmentAmountDraft = it },
                            label = { Text("مقدار تغییر") }, placeholder = { Text("مثلاً ۲۰ یا -۵") }, singleLine = true,
                            supportingText = { Text("عدد مثبت برای افزایش و عدد منفی برای کاهش؛ XP قابل خرج‌کردن نیست") })
                        OutlinedTextField(value = adjustmentReasonDraft, onValueChange = { adjustmentReasonDraft = it },
                            label = { Text("دلیل اصلاح (الزامی)") }, singleLine = true)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val delta = parsedAdjustmentAmount
                        if (delta != null && delta != 0L && adjustmentReasonDraft.isNotBlank()) {
                            onAdjustEconomy(adjustmentCurrency, delta, adjustmentReasonDraft)
                            showEconomyAdjustmentDialog = false
                        }
                    }, enabled = parsedAdjustmentAmount?.let { it != 0L } == true && adjustmentReasonDraft.isNotBlank()) {
                        Text("ثبت اصلاح")
                    }
                },
                dismissButton = { TextButton(onClick = { showEconomyAdjustmentDialog = false }) { Text("انصراف") } },
            )
        }
        if (showProfileEditDialog) {
            AlertDialog(
                onDismissRequest = { showProfileEditDialog = false },
                title = { Text("ویرایش مشخصات عضو") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = profileNameDraft, onValueChange = { profileNameDraft = it }, label = { Text("نام و نام خانوادگی") }, singleLine = true)
                        OutlinedTextField(value = profileNicknameDraft, onValueChange = { profileNicknameDraft = it }, label = { Text("نام نمایشی (اختیاری)") }, singleLine = true)
                        OutlinedTextField(value = birthDateDraft, onValueChange = { birthDateDraft = it }, label = { Text("تاریخ تولد شمسی (اختیاری)") }, placeholder = { Text("۱۴۰۰/۰۵/۲۱") }, singleLine = true)
                    }
                },
                confirmButton = { Button(onClick = { onUpdateProfile(profileNameDraft, profileNicknameDraft, birthDateDraft) }) { Text("ذخیره") } },
                dismissButton = { TextButton(onClick = { showProfileEditDialog = false }) { Text("انصراف") } },
            )
        }
    }
}

@Composable
private fun MemberReportCard(
    member: Member,
    transactions: List<EconomyTransaction>,
    growthItems: List<GrowthItem>,
    growthProgress: List<MemberGrowthProgress>,
    missions: List<Mission>,
    missionAssignments: List<MissionAssignment>,
) {
    var periodMode by rememberSaveable { mutableStateOf("MONTH") }
    val currentPersianDate = remember { Calendar.getInstance(ULocale("fa_IR@calendar=persian")) }
    var selectedYear by rememberSaveable { mutableStateOf(currentPersianDate.get(Calendar.YEAR)) }
    var selectedMonth by rememberSaveable { mutableStateOf(currentPersianDate.get(Calendar.MONTH) + 1) }
    val summary = remember(transactions, periodMode, selectedYear, selectedMonth) {
        calculateJalaliMemberReport(transactions, selectedYear, selectedMonth, periodMode == "MONTH")
    }
    val level = LevelProgression.levelFor(member.totalXp)
    val levelStart = LevelProgression.minimumXpForLevel(level)
    val nextLevelStart = LevelProgression.minimumXpForLevel(level + 1)
    val levelProgress = ((member.totalXp - levelStart).coerceAtLeast(0L).toFloat() /
        (nextLevelStart - levelStart).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val growthProgressById = growthProgress.associateBy { it.growthItemId }
    val growthAverage = if (growthItems.isEmpty()) null else
        growthItems.map { growthProgressById[it.id]?.progressPercent ?: 0 }.average().toInt()

    ProfileSection("کارنامه", "شش محور مستقل رشد عضو؛ بازه را ماهانه یا سالانه انتخاب کن") {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = periodMode == "MONTH",
                onClick = { periodMode = "MONTH" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("ماهانه") }
            SegmentedButton(
                selected = periodMode == "YEAR",
                onClick = { periodMode = "YEAR" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("سالانه") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {
                if (periodMode == "MONTH") {
                    if (selectedMonth == 1) { selectedYear -= 1; selectedMonth = 12 } else selectedMonth -= 1
                } else selectedYear -= 1
            }) { Text("قبلی") }
            Text(
                if (periodMode == "MONTH") "${selectedYear}/${selectedMonth.toString().padStart(2, '0')}"
                else selectedYear.toString(),
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = {
                if (periodMode == "MONTH") {
                    if (selectedMonth == 12) { selectedYear += 1; selectedMonth = 1 } else selectedMonth += 1
                } else selectedYear += 1
            }) { Text("بعدی") }
        }

        val reportableMissionIds = missions.filter { it.kind != MissionKind.GROUP_PUZZLE }.map { it.id }.toSet()
        val memberMissionAssignments = missionAssignments.filter { it.memberId == member.id && it.missionId in reportableMissionIds }
        val missionBucketCount = if (periodMode == "MONTH") {
            Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply { clear(); set(selectedYear, selectedMonth - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        } else 12
        val missionSeries = remember(memberMissionAssignments, periodMode, selectedYear, selectedMonth, missionBucketCount) {
            val buckets = MutableList(missionBucketCount) { 0L }
            memberMissionAssignments.forEach { assignment ->
                val date = Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply { timeInMillis = assignment.updatedAtEpochMillis }
                if (date.get(Calendar.YEAR) != selectedYear) return@forEach
                val assignmentMonth = date.get(Calendar.MONTH) + 1
                if (periodMode == "MONTH" && assignmentMonth != selectedMonth) return@forEach
                if (assignment.status == MissionAssignmentStatus.COMPLETED || assignment.status == MissionAssignmentStatus.APPROVED) {
                    val index = if (periodMode == "MONTH") date.get(Calendar.DATE) - 1 else assignmentMonth - 1
                    buckets[index] += 1L
                }
            }
            buckets
        }
        val missionPeriodAssignments = memberMissionAssignments.filter { assignment ->
            val date = Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply { timeInMillis = assignment.updatedAtEpochMillis }
            date.get(Calendar.YEAR) == selectedYear && (periodMode != "MONTH" || date.get(Calendar.MONTH) + 1 == selectedMonth)
        }
        val missionCompletedCount = missionPeriodAssignments.count { it.status == MissionAssignmentStatus.COMPLETED || it.status == MissionAssignmentStatus.APPROVED }
        val missionApprovedCount = missionPeriodAssignments.count { it.status == MissionAssignmentStatus.APPROVED }
        MemberReportAxisCard(
            title = "مأموریت",
            value = "$missionCompletedCount / ${missionPeriodAssignments.size}",
            detail = if (missionPeriodAssignments.isEmpty()) "در این بازه مأموریتی به عضو اختصاص داده نشده است"
                else "$missionApprovedCount مأموریت تأیید و پاداش‌دهی شده از ${missionPeriodAssignments.size} مأموریت اختصاص‌یافته",
            progress = if (missionPeriodAssignments.isEmpty()) null else missionCompletedCount.toFloat() / missionPeriodAssignments.size,
            series = missionSeries,
        )
        MemberReportAxisCard(
            title = "امتیاز قابل‌خرج",
            value = member.spendablePoints.toString(),
            detail = "مانده فعلی • تغییر خالص بازه: ${formatReportDelta(summary.pointsDelta)}",
            series = summary.pointsSeries,
        )
        MemberReportAxisCard(
            title = "نقشه کمال",
            value = growthAverage?.let { "$it٪" } ?: "بدون شاخص فعال",
            detail = if (growthAverage == null) "برای این عضو شاخص فعالی تعریف نشده است"
                else "میانگین وضعیت فعلی از ${growthItems.size} شاخص فعال؛ تاریخچه روند هنوز ذخیره نمی‌شود",
            progress = growthAverage?.div(100f),
        )
        MemberReportAxisCard(
            title = "کتاب‌خوانی",
            value = "داده‌ای ثبت نشده",
            detail = "ثبت فعالیت کتاب‌خوانی هنوز در این نسخه فعال نیست",
        )
        MemberReportAxisCard(
            title = "سطح",
            value = "سطح $level",
            detail = "${member.totalXp} XP فعلی • تغییر XP در بازه: ${formatReportDelta(summary.xpDelta)}",
            progress = levelProgress,
            series = summary.xpSeries,
        )
        MemberReportAxisCard(
            title = "الماس",
            value = member.diamonds.toString(),
            detail = "مانده فعلی • تغییر خالص بازه: ${formatReportDelta(summary.diamondsDelta)}",
            series = summary.diamondsSeries,
        )
    }
}

@Composable
private fun MemberReportAxisCard(
    title: String,
    value: String,
    detail: String,
    progress: Float? = null,
    series: List<Long>? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(value, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            progress?.let {
                LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            series?.let {
                if (it.any { value -> value != 0L }) ReportSparkline(it, Modifier.fillMaxWidth())
                else Text("در این بازه تراکنشی ثبت نشده است", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReportSparkline(values: List<Long>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.height(42.dp)) {
        if (values.size < 2) return@Canvas
        val minimum = values.minOrNull() ?: 0L
        val maximum = values.maxOrNull() ?: 0L
        val range = (maximum - minimum).coerceAtLeast(1L).toFloat()
        val points = values.mapIndexed { index, value ->
            Offset(
                x = size.width * index / (values.size - 1).toFloat(),
                y = size.height - ((value - minimum).toFloat() / range * size.height),
            )
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun calculateJalaliMemberReport(
    transactions: List<EconomyTransaction>,
    year: Int,
    month: Int,
    monthly: Boolean,
): MemberReportSummary {
    val locale = ULocale("fa_IR@calendar=persian")
    val monthCalendar = Calendar.getInstance(locale).apply {
        clear()
        set(year, month - 1, 1, 0, 0, 0)
    }
    val bucketCount = if (monthly) monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) else 12
    val xp = MutableList(bucketCount) { 0L }
    val points = MutableList(bucketCount) { 0L }
    val diamonds = MutableList(bucketCount) { 0L }
    transactions.forEach { transaction ->
        val date = Calendar.getInstance(locale).apply { timeInMillis = transaction.createdAtEpochMillis }
        if (date.get(Calendar.YEAR) != year) return@forEach
        val transactionMonth = date.get(Calendar.MONTH) + 1
        if (monthly && transactionMonth != month) return@forEach
        val index = if (monthly) date.get(Calendar.DATE) - 1 else transactionMonth - 1
        when (transaction.currency) {
            EconomyCurrency.XP -> xp[index] = Math.addExact(xp[index], transaction.amountDelta)
            EconomyCurrency.POINTS -> points[index] = Math.addExact(points[index], transaction.amountDelta)
            EconomyCurrency.DIAMONDS -> diamonds[index] = Math.addExact(diamonds[index], transaction.amountDelta)
        }
    }
    return MemberReportSummary(
        xpDelta = xp.sum(),
        pointsDelta = points.sum(),
        diamondsDelta = diamonds.sum(),
        xpSeries = xp,
        pointsSeries = points,
        diamondsSeries = diamonds,
    )
}

private fun createMemberPdf(name: String, sections: List<Pair<String, String>>, avatarBytes: ByteArray?, activeAvatarBytes: ByteArray?, activeFrameBytes: ByteArray?): PdfDocument {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 38f
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(45, 55, 68); textSize = 11f }
    val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(20, 65, 90); textSize = 17f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(15, 50, 75); textSize = 23f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: AndroidCanvas? = null
    var y = margin
    fun startPage() {
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page!!.canvas
        canvas!!.drawColor(android.graphics.Color.WHITE)
        y = margin
        val header = "نگاه · پرونده عضو"
        val headerLayout = StaticLayout.Builder.obtain(header, 0, header.length, headingPaint, (pageWidth - margin * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build()
        canvas!!.save()
        canvas!!.translate(margin, y)
        headerLayout.draw(canvas!!)
        canvas!!.restore()
        y += headerLayout.height + 18f
    }
    fun finishPage() { page?.let { document.finishPage(it) }; page = null; canvas = null }
    fun drawLayout(layout: StaticLayout) {
        var line = 0
        while (line < layout.lineCount) {
            if (page == null) startPage()
            val available = pageHeight - margin - y
            var endLine = line
            while (endLine < layout.lineCount && layout.getLineBottom(endLine) - layout.getLineTop(line) <= available) endLine++
            if (endLine == line) { finishPage(); startPage(); continue }
            val top = layout.getLineTop(line)
            val bottom = layout.getLineBottom(endLine - 1)
            canvas!!.save()
            canvas!!.translate(margin, y - top)
            canvas!!.clipRect(0f, top.toFloat(), (pageWidth - margin * 2).toFloat(), bottom.toFloat())
            layout.draw(canvas!!)
            canvas!!.restore()
            y += (bottom - top) + 10f
            line = endLine
            if (line < layout.lineCount) { finishPage(); startPage() }
        }
    }
    startPage()
    val titleLayout = StaticLayout.Builder.obtain(name, 0, name.length, titlePaint, (pageWidth - margin * 2).toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build()
    drawLayout(titleLayout)
    avatarBytes?.let { bytes ->
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            if (y + 112f > pageHeight - margin) { finishPage(); startPage() }
            val side = 92f
            canvas!!.drawBitmap(bitmap, null, RectF(pageWidth - margin - side, y, pageWidth - margin, y + side), Paint(Paint.ANTI_ALIAS_FLAG))
            bitmap.recycle()
            y += side + 16f
        }
    }
    val activeAvatar = activeAvatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    val activeFrame = activeFrameBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    if (activeAvatar != null || activeFrame != null) {
        if (y + 112f > pageHeight - margin) { finishPage(); startPage() }
        val side = 92f
        activeAvatar?.let { canvas!!.drawBitmap(it, null, RectF(margin, y, margin + side, y + side), Paint(Paint.ANTI_ALIAS_FLAG)); it.recycle() }
        activeFrame?.let { canvas!!.drawBitmap(it, null, RectF(margin, y, margin + side, y + side), Paint(Paint.ANTI_ALIAS_FLAG)); it.recycle() }
        y += side + 16f
    }
    sections.forEach { (title, content) ->
        val heading = StaticLayout.Builder.obtain(title, 0, title.length, headingPaint, (pageWidth - margin * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build()
        if (y + heading.height + 18f > pageHeight - margin) { finishPage(); startPage() }
        drawLayout(heading)
        val body = StaticLayout.Builder.obtain(content, 0, content.length, bodyPaint, (pageWidth - margin * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).setLineSpacing(4f, 1f).build()
        drawLayout(body)
        y += 8f
    }
    finishPage()
    return document
}

private fun effectiveActiveBadgeLevel(member: Member, level: Int): Int? {
    val maxEarned = level.coerceIn(0, RankBadgeProgression.all.size)
    return member.activeBadgeLevel?.takeIf { it in 1..maxEarned }
        ?: maxEarned.takeIf { it > 0 }
}

private fun receivedHistorySolarMonth(timestamp: Long): String {
    val calendar = Calendar.getInstance(ULocale("fa_IR@calendar=persian")).apply { timeInMillis = timestamp }
    return "${calendar.get(Calendar.YEAR)}/${(calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}"
}

private fun formatReportDelta(value: Long): String = when {
    value > 0L -> "+$value"
    else -> value.toString()
}

private data class ReceivedHistoryRow(
    val id: String,
    val timestamp: Long,
    val title: String,
    val detail: String,
    val amount: String? = null,
    val isPositive: Boolean = true,
)

@Composable
private fun ReceivedHistoryRowCard(row: ReceivedHistoryRow) {
    val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("fa", "IR"))
        .format(Date(row.timestamp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall)
                Text(row.detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timestamp, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            row.amount?.let {
                Text(it, style = MaterialTheme.typography.titleMedium,
                    color = if (row.isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun GrowthTrackSection(
    title: String,
    items: List<GrowthItem>,
    progress: List<MemberGrowthProgress>,
    onSave: (GrowthItem, Int, String?) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    if (items.isEmpty()) {
        Text("هنوز آیتمی در این مسیر تعریف نشده است", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        items.forEach { item ->
            val saved = progress.firstOrNull { it.growthItemId == item.id }
            var percent by rememberSaveable(item.id, saved?.progressPercent) {
                mutableStateOf(saved?.progressPercent ?: 0)
            }
            var note by rememberSaveable(item.id, saved?.note) {
                mutableStateOf(saved?.note.orEmpty())
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("پیشرفت", style = MaterialTheme.typography.labelMedium)
                    Text("$percent٪", style = MaterialTheme.typography.labelMedium)
                }
                Slider(value = percent.toFloat(), onValueChange = { percent = it.toInt() }, valueRange = 0f..100f, steps = 99)
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("یادداشت مربی (اختیاری)") }, modifier = Modifier.fillMaxWidth(), minLines = 1)
                Button(onClick = { onSave(item, percent, note) }, modifier = Modifier.fillMaxWidth()) {
                    Text("ذخیره پیشرفت")
                }
            }
        }
    }
}

@Composable
private fun ProfileWalletCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
@Composable
private fun EconomyTransactionRow(transaction: EconomyTransaction) {
    val currencyLabel = when (transaction.currency) {
        EconomyCurrency.XP -> "XP"
        EconomyCurrency.POINTS -> "امتیاز"
        EconomyCurrency.DIAMONDS -> "الماس"
    }
    val kindLabel = when (transaction.kind) {
        EconomyTransactionKind.EARN -> "دریافت"
        EconomyTransactionKind.SPEND -> "خرج"
        EconomyTransactionKind.CORRECTION -> "اصلاح"
        EconomyTransactionKind.REFUND -> "بازگشت"
    }
    val signedAmount = if (transaction.amountDelta > 0) {
        "+${transaction.amountDelta}"
    } else transaction.amountDelta.toString()
    val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("fa", "IR"))
        .format(Date(transaction.createdAtEpochMillis))

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$kindLabel $currencyLabel", style = MaterialTheme.typography.titleSmall)
                Text(timestamp, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                signedAmount,
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.amountDelta < 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
