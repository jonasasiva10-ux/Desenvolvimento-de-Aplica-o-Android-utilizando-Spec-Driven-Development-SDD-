package com.example.domain.usecase

import com.example.domain.model.GameDetail
import com.example.domain.model.GameFilter
import com.example.domain.model.GameRank
import com.example.domain.model.SyncOutcome
import com.example.domain.model.TrendDirection
import com.example.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTrendingGamesUseCase(
    private val gameRepository: GameRepository,
) {
    operator fun invoke(
        query: String = "",
        filter: GameFilter = GameFilter.ALL,
    ): Flow<List<GameRank>> {
        val normalizedQuery = query.trim().lowercase()
        return gameRepository.observeTrendingGames().map { games ->
            games
                .asSequence()
                .filter { game ->
                    when (filter) {
                        GameFilter.ALL -> true
                        GameFilter.RISING -> game.trendDirection == TrendDirection.RISING || game.trendDirection == TrendDirection.NEW
                        GameFilter.FAVORITES -> game.isFavorite
                    }
                }
                .filter { game ->
                    if (normalizedQuery.isEmpty()) {
                        true
                    } else {
                        game.name.lowercase().contains(normalizedQuery) ||
                            game.genre.lowercase().contains(normalizedQuery) ||
                            game.appId.toString().contains(normalizedQuery)
                    }
                }
                .sortedBy { it.rank }
                .toList()
        }
    }
}

class RefreshTrendingGamesUseCase(
    private val gameRepository: GameRepository,
) {
    suspend operator fun invoke(force: Boolean = false): SyncOutcome {
        return gameRepository.refreshTrendingGames(force)
    }
}

class GetGameDetailUseCase(
    private val gameRepository: GameRepository,
) {
    fun observeCachedGame(appId: Int): Flow<GameRank?> = gameRepository.observeGameById(appId)

    suspend fun loadLiveDetail(appId: Int): Result<GameDetail> = gameRepository.fetchGameDetail(appId)
}

class ToggleFavoriteGameUseCase(
    private val gameRepository: GameRepository,
) {
    suspend operator fun invoke(appId: Int) {
        gameRepository.toggleFavorite(appId)
    }
}

class GetFavoriteGamesUseCase(
    private val gameRepository: GameRepository,
) {
    operator fun invoke(): Flow<List<GameRank>> = gameRepository.observeFavoriteGames()
}

class ObserveSyncMetadataUseCase(
    private val gameRepository: GameRepository,
) {
    fun isOnline(): Flow<Boolean> = gameRepository.observeConnectivity()
    fun lastSyncAt(syncKey: String): Flow<Long?> = gameRepository.observeLastSyncTimestamp(syncKey)
}
