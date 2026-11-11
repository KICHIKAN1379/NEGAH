package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.economy.EconomyRepository
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.member.MemberAvatarRepository
import com.kichikan.negah.domain.member.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeAssetsState(val avatars: Map<String, ByteArray> = emptyMap(), val transactions: List<EconomyTransaction> = emptyList())

@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val avatarRepository: MemberAvatarRepository,
    private val economyRepository: EconomyRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeAssetsState())
    val state = mutableState.asStateFlow()
    private var ledgerJob: Job? = null

    init {
        viewModelScope.launch {
            memberRepository.observeActiveMembers().collectLatest { members ->
                val avatars = coroutineScope {
                    members.map { member -> async { member.id to runCatching { avatarRepository.loadAvatar(member.id) }.getOrNull() } }
                        .awaitAll().mapNotNull { pair -> pair.second?.let { pair.first to it } }.toMap()
                }
                mutableState.update { it.copy(avatars = avatars) }
                ledgerJob?.cancel()
                if (members.isEmpty()) mutableState.update { it.copy(transactions = emptyList()) }
                else {
                    val sources = members.map { economyRepository.observeForMember(it.id) }
                    ledgerJob = viewModelScope.launch {
                        combine(sources) { batches -> batches.flatMap { it } }.collect { rows ->
                            mutableState.update { it.copy(transactions = rows.sortedByDescending(EconomyTransaction::createdAtEpochMillis)) }
                        }
                    }
                }
            }
        }
    }
}