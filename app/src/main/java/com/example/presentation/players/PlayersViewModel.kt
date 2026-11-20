package com.example.presentation.players

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.ProPlayer
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.SyncKeys
import com.example.domain.usecase.GetProPlayersUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.RefreshProPlayersUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class FeaturedAccountShortcut(
    val accountId: Long,
    val label: String,
    val team: String,
)

@Immutable
data class PlayersUiState(
    val proPlayers: List<ProPlayer> = emptyList(),
    val searchQuery: String = "",
    val quickPicks: List<FeaturedAccountShortcut> = DEFAULT_SHORTCUTS,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val error: AppError? = null,
) {
    val parsedDirectAccountId: Long?
        get() = searchQuery.trim().toLongOrNull()?.takeIf { it > 1000L }

    companion object {
        val DEFAULT_SHORTCUTS = listOf(
            FeaturedAccountShortcut(321580662L, "Yatoro", "Team Spirit"),
            FeaturedAccountShortcut(105248644L, "Miracle-", "Nigma Galaxy"),
            FeaturedAccountShortcut(86745912L, "Arteezy", "Shopify"),
            FeaturedAccountShortcut(94054712L, "Topson", "Tundra"),
            FeaturedAccountShortcut(70388657L, "Dendi", "B8"),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayersViewModel(
    private val getProPlayers: GetProPlayersUseCase,
    private val refreshProPlayers: RefreshProPlayersUseCase,
    private val observeSyncMetadata: ObserveSyncMetadataUseCase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isSyncing = MutableStateFlow(false)
    private val syncError = MutableStateFlow<AppError?>(null)
    private val hasAttemptedInitialLoad = MutableStateFlow(false)

    private val filteredPlayersFlow = searchQuery.flatMapLatest { query ->
        getProPlayers(query)
    }

    val uiState: StateFlow<PlayersUiState> = combine(
        filteredPlayersFlow,
        searchQuery,
        combine(
            isSyncing,
            observeSyncMetadata.isOnline(),
            observeSyncMetadata.lastSyncAt(SyncKeys.PRO_PLAYERS),
            syncError,
            hasAttemptedInitialLoad,
        ) { syncing, online, lastSync, err, loaded ->
            SyncMeta(syncing, online, lastSync, err, loaded)
        },
    ) { players, query, meta ->
        PlayersUiState(
            proPlayers = players,
            searchQuery = query,
            isLoading = players.isEmpty() && (meta.syncing || !meta.loaded) && meta.error == null,
            isSyncing = meta.syncing,
            isOnline = meta.online,
            lastSyncedAt = meta.lastSync,
            error = if (players.isEmpty()) meta.error else null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayersUiState(),
    )

    init {
        refresh(force = false)
        observeReconnect()
    }

    private fun observeReconnect() {
        viewModelScope.launch {
            var wasOffline = false
            observeSyncMetadata.isOnline().collect { online ->
                if (!online) {
                    wasOffline = true
                } else if (wasOffline) {
                    wasOffline = false
                    refresh(force = true)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun refresh(force: Boolean = true) {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            when (val outcome = refreshProPlayers(force = force)) {
                SyncOutcome.Success -> syncError.value = null
                is SyncOutcome.Failure -> syncError.value = outcome.error
            }
            hasAttemptedInitialLoad.value = true
            isSyncing.value = false
        }
    }

    private data class SyncMeta(
        val syncing: Boolean,
        val online: Boolean,
        val lastSync: Long?,
        val error: AppError?,
        val loaded: Boolean,
    )
}
