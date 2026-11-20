package com.example.presentation.playerdetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.PlayerStats
import com.example.domain.model.SyncOutcome
import com.example.domain.usecase.GetPlayerStatsUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class PlayerDetailUiState(
    val stats: PlayerStats? = null,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val error: AppError? = null,
)

class PlayerDetailViewModel(
    private val accountId: Long,
    private val getPlayerStats: GetPlayerStatsUseCase,
    observeSyncMetadata: ObserveSyncMetadataUseCase,
) : ViewModel() {

    private val isSyncing = MutableStateFlow(false)
    private val syncError = MutableStateFlow<AppError?>(null)
    private val hasAttemptedLoad = MutableStateFlow(false)

    val uiState: StateFlow<PlayerDetailUiState> = combine(
        getPlayerStats.observe(accountId),
        isSyncing,
        observeSyncMetadata.isOnline(),
        syncError,
        hasAttemptedLoad,
    ) { stats, syncing, online, error, loaded ->
        PlayerDetailUiState(
            stats = stats,
            isLoading = stats == null && (syncing || !loaded) && error == null,
            isSyncing = syncing,
            isOnline = online,
            error = if (stats == null) error else null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerDetailUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            when (val outcome = getPlayerStats.refresh(accountId)) {
                SyncOutcome.Success -> syncError.value = null
                is SyncOutcome.Failure -> syncError.value = outcome.error
            }
            hasAttemptedLoad.value = true
            isSyncing.value = false
        }
    }
}
