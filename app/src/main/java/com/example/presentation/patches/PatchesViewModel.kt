package com.example.presentation.patches

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.GameRank
import com.example.domain.model.PatchNote
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.SyncKeys
import com.example.domain.usecase.GetPatchNotesUseCase
import com.example.domain.usecase.GetTrendingGamesUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.RefreshPatchNotesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class PatchesUiState(
    val patches: List<PatchNote> = emptyList(),
    val availableGames: List<GameRank> = emptyList(),
    val selectedAppId: Int? = null,
    val onlyOfficialPatches: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
    val error: AppError? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PatchesViewModel(
    private val getPatchNotes: GetPatchNotesUseCase,
    private val refreshPatchNotes: RefreshPatchNotesUseCase,
    private val getTrendingGames: GetTrendingGamesUseCase,
    private val observeSyncMetadata: ObserveSyncMetadataUseCase,
) : ViewModel() {

    private val selectedAppId = MutableStateFlow<Int?>(null)
    private val onlyOfficialPatches = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val isSyncing = MutableStateFlow(false)
    private val syncError = MutableStateFlow<AppError?>(null)
    private val hasAttemptedInitialLoad = MutableStateFlow(false)

    private val patchesFlow = combine(
        selectedAppId,
        onlyOfficialPatches,
        searchQuery,
    ) { appId, officialOnly, query ->
        Triple(appId, officialOnly, query)
    }.flatMapLatest { (appId, officialOnly, query) ->
        getPatchNotes(
            selectedAppId = appId,
            onlyOfficialPatches = officialOnly,
            query = query,
        )
    }

    val uiState: StateFlow<PatchesUiState> = combine(
        patchesFlow,
        getTrendingGames(),
        combine(selectedAppId, onlyOfficialPatches, searchQuery) { a, o, q -> Triple(a, o, q) },
        combine(
            isSyncing,
            observeSyncMetadata.isOnline(),
            observeSyncMetadata.lastSyncAt(SyncKeys.PATCH_NOTES),
            syncError,
            hasAttemptedInitialLoad,
        ) { syncing, online, lastSync, err, loaded ->
            SyncStatusTuple(syncing, online, lastSync, err, loaded)
        },
    ) { patches, games, filters, meta ->
        val (appId, officialOnly, query) = filters
        PatchesUiState(
            patches = patches,
            availableGames = games.take(10),
            selectedAppId = appId,
            onlyOfficialPatches = officialOnly,
            searchQuery = query,
            isLoading = patches.isEmpty() && (meta.syncing || !meta.loaded) && meta.error == null,
            isSyncing = meta.syncing,
            isOnline = meta.online,
            lastSyncedAt = meta.lastSync,
            error = if (patches.isEmpty()) meta.error else null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PatchesUiState(),
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

    fun onGameSelected(appId: Int?) {
        selectedAppId.value = appId
    }

    fun onToggleOfficialOnly() {
        onlyOfficialPatches.value = !onlyOfficialPatches.value
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun refresh(force: Boolean = true) {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            when (val outcome = refreshPatchNotes.refreshAll(force = force)) {
                SyncOutcome.Success -> syncError.value = null
                is SyncOutcome.Failure -> syncError.value = outcome.error
            }
            hasAttemptedInitialLoad.value = true
            isSyncing.value = false
        }
    }

    private data class SyncStatusTuple(
        val syncing: Boolean,
        val online: Boolean,
        val lastSync: Long?,
        val error: AppError?,
        val loaded: Boolean,
    )
}
