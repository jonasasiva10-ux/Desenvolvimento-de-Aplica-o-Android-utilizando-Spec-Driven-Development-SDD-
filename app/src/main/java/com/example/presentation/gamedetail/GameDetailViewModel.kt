package com.example.presentation.gamedetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppError
import com.example.domain.model.GameDetail
import com.example.domain.model.PatchNote
import com.example.domain.usecase.GetGameDetailUseCase
import com.example.domain.usecase.GetPatchNotesUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.RefreshPatchNotesUseCase
import com.example.domain.usecase.ToggleFavoriteGameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class GameDetailUiState(
    val detail: GameDetail? = null,
    val patches: List<PatchNote> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val error: AppError? = null,
)

class GameDetailViewModel(
    private val appId: Int,
    private val getGameDetail: GetGameDetailUseCase,
    getPatchNotes: GetPatchNotesUseCase,
    private val refreshPatchNotes: RefreshPatchNotesUseCase,
    private val toggleFavoriteGame: ToggleFavoriteGameUseCase,
    observeSyncMetadata: ObserveSyncMetadataUseCase,
) : ViewModel() {

    private val loadedDetail = MutableStateFlow<GameDetail?>(null)
    private val isSyncing = MutableStateFlow(false)
    private val loadError = MutableStateFlow<AppError?>(null)

    val uiState: StateFlow<GameDetailUiState> = combine(
        getGameDetail.observeCachedGame(appId),
        loadedDetail,
        getPatchNotes(selectedAppId = appId),
        isSyncing,
        combine(observeSyncMetadata.isOnline(), loadError) { online, err -> online to err },
    ) { cached, liveDetail, patches, syncing, (online, error) ->
        val mergedDetail = liveDetail?.copy(
            isFavorite = cached?.isFavorite ?: liveDetail.isFavorite,
            currentPlayers = liveDetail.currentPlayers ?: cached?.currentPlayers,
            rank = cached?.rank ?: liveDetail.rank,
        ) ?: cached?.let {
            GameDetail(
                appId = it.appId,
                name = it.name,
                shortDescription = "Telemetria em cache local. Conecte-se para obter a descrição completa da Steam Store.",
                developer = "Steam Official",
                genres = listOf(it.genre),
                headerImageUrl = it.headerImageUrl,
                metacriticScore = null,
                currentPlayers = it.currentPlayers,
                peakInGame = it.peakInGame,
                rank = it.rank,
                lastWeekRank = it.lastWeekRank,
                isFavorite = it.isFavorite,
            )
        }

        GameDetailUiState(
            detail = mergedDetail,
            patches = patches,
            isLoading = mergedDetail == null && syncing,
            isSyncing = syncing,
            isOnline = online,
            error = if (mergedDetail == null) error else null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GameDetailUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            loadError.value = null
            val detailResult = getGameDetail.loadLiveDetail(appId)
            detailResult
                .onSuccess { detail ->
                    loadedDetail.value = detail
                    refreshPatchNotes.refreshForGame(appId = appId, gameName = detail.name)
                }
                .onFailure {
                    loadError.value = AppError.NetworkUnavailable
                }
            isSyncing.value = false
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            toggleFavoriteGame(appId)
        }
    }
}
