package com.kichikan.negah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kichikan.negah.domain.economy.EconomyRepository
import com.kichikan.negah.domain.economy.EconomyTransaction
import com.kichikan.negah.domain.member.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeAssetsState(val transactions: List<EconomyTransaction> = emptyList())

@HiltViewModel
class HomeDashboardViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val economyRepository: EconomyRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeAssetsState())
    val state = mutableState.asStateFlow()
    private var ledgerJob: Job? = null

    init {
        viewModelScope.launch {
            memberRepository.observeActiveMembers().collectLatest { members ->
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