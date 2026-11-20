package com.example.presentation.trends

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.GameFilter
import com.example.domain.model.GameRank
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.SyncKeys
import com.example.domain.usecase.GetTrendingGamesUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.RefreshTrendingGamesUseCase
import com.example.domain.usecase.ToggleFavoriteGameUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class TrendsUiState(
    val games: List<GameRank> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: GameFilter = GameFilter.ALL,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val error: AppError? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModel(
    private val getTrendingGames: GetTrendingGamesUseCase,
    private val refreshTrendingGames: RefreshTrendingGamesUseCase,
    private val toggleFavoriteGame: ToggleFavoriteGameUseCase,
    private val observeSyncMetadata: ObserveSyncMetadataUseCase,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(GameFilter.ALL)
    private val isSyncing = MutableStateFlow(false)
    private val syncError = MutableStateFlow<AppError?>(null)
    private val hasAttemptedInitialLoad = MutableStateFlow(false)

    private val filteredGamesFlow = combine(searchQuery, selectedFilter) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        getTrendingGames(query = query, filter = filter)
    }

    val uiState: StateFlow<TrendsUiState> = combine(
        filteredGamesFlow,
        searchQuery,
        selectedFilter,
        combine(
            isSyncing,
            observeSyncMetadata.isOnline(),
            observeSyncMetadata.lastSyncAt(SyncKeys.TRENDING_GAMES),
            syncError,
            hasAttemptedInitialLoad,
        ) { syncing, online, lastSync, err, loaded ->
            SyncMetaTuple(syncing, online, lastSync, err, loaded)
        },
    ) { games, query, filter, meta ->
        val showInitialLoading = games.isEmpty() && (meta.syncing || !meta.loaded) && meta.error == null
        TrendsUiState(
            games = games,
            searchQuery = query,
            selectedFilter = filter,
            isLoading = showInitialLoading,
            isSyncing = meta.syncing,
            isOnline = meta.online,
            lastSyncedAt = meta.lastSync,
            error = if (games.isEmpty()) meta.error else null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendsUiState(),
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

    fun onFilterSelected(filter: GameFilter) {
        selectedFilter.value = filter
    }

    fun onToggleFavorite(appId: Int) {
        viewModelScope.launch {
            toggleFavoriteGame(appId)
        }
    }

    fun refresh(force: Boolean = true) {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            when (val outcome = refreshTrendingGames(force = force)) {
                SyncOutcome.Success -> syncError.value = null
                is SyncOutcome.Failure -> syncError.value = outcome.error
            }
            hasAttemptedInitialLoad.value = true
            isSyncing.value = false
        }
    }

    private data class SyncMetaTuple(
        val syncing: Boolean,
        val online: Boolean,
        val lastSync: Long?,
        val error: AppError?,
        val loaded: Boolean,
    )
}
