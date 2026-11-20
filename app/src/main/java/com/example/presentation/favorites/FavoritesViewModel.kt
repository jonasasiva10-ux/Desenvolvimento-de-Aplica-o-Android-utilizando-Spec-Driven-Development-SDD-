package com.example.presentation.favorites

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.GameRank
import com.example.domain.model.PatchNote
import com.example.domain.repository.SyncKeys
import com.example.domain.usecase.GetFavoriteGamesUseCase
import com.example.domain.usecase.GetFavoritePatchesUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.ToggleFavoriteGameUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class FavoritesUiState(
    val favoriteGames: List<GameRank> = emptyList(),
    val favoritePatches: List<PatchNote> = emptyList(),
    val isOnline: Boolean = true,
    val lastSyncedAt: Long? = null,
)

class FavoritesViewModel(
    getFavoriteGames: GetFavoriteGamesUseCase,
    getFavoritePatches: GetFavoritePatchesUseCase,
    private val toggleFavoriteGame: ToggleFavoriteGameUseCase,
    observeSyncMetadata: ObserveSyncMetadataUseCase,
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = combine(
        getFavoriteGames(),
        getFavoritePatches(),
        observeSyncMetadata.isOnline(),
        observeSyncMetadata.lastSyncAt(SyncKeys.TRENDING_GAMES),
    ) { games, patches, online, lastSync ->
        FavoritesUiState(
            favoriteGames = games,
            favoritePatches = patches,
            isOnline = online,
            lastSyncedAt = lastSync,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState(),
    )

    fun onToggleFavorite(appId: Int) {
        viewModelScope.launch {
            toggleFavoriteGame(appId)
        }
    }
}
