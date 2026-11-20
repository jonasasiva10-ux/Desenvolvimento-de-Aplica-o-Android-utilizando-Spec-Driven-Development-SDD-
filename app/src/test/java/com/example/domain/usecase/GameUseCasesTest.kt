package com.example.domain.usecase

import com.example.domain.model.AppError
import com.example.domain.model.GameDetail
import com.example.domain.model.GameFilter
import com.example.domain.model.GameRank
import com.example.domain.model.SyncOutcome
import com.example.domain.model.TrendDirection
import com.example.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUseCasesTest {

    private val sampleGames = listOf(
        GameRank(
            appId = 730,
            rank = 1,
            lastWeekRank = 1,
            peakInGame = 1_420_000,
            currentPlayers = 980_000,
            name = "Counter-Strike 2",
            genre = "FPS Tático",
            headerImageUrl = "https://example.com/cs2.jpg",
            isFavorite = true,
            lastUpdatedAt = 1000L,
        ),
        GameRank(
            appId = 2767030,
            rank = 2,
            lastWeekRank = 5,
            peakInGame = 450_000,
            currentPlayers = 320_000,
            name = "Marvel Rivals",
            genre = "Hero Shooter",
            headerImageUrl = "https://example.com/mr.jpg",
            isFavorite = false,
            lastUpdatedAt = 1000L,
        ),
        GameRank(
            appId = 570,
            rank = 3,
            lastWeekRank = 2,
            peakInGame = 680_000,
            currentPlayers = 510_000,
            name = "Dota 2",
            genre = "MOBA Competitivo",
            headerImageUrl = "https://example.com/dota2.jpg",
            isFavorite = false,
            lastUpdatedAt = 1000L,
        ),
        GameRank(
            appId = 2694490,
            rank = 4,
            lastWeekRank = 0,
            peakInGame = 290_000,
            currentPlayers = 210_000,
            name = "Path of Exile 2",
            genre = "Action RPG",
            headerImageUrl = "https://example.com/poe2.jpg",
            isFavorite = true,
            lastUpdatedAt = 1000L,
        ),
    )

    @Test
    fun `rank delta and trend direction are computed accurately`() {
        assertEquals(0, sampleGames[0].rankDelta)
        assertEquals(TrendDirection.STEADY, sampleGames[0].trendDirection)

        assertEquals(3, sampleGames[1].rankDelta)
        assertEquals(TrendDirection.RISING, sampleGames[1].trendDirection)

        assertEquals(-1, sampleGames[2].rankDelta)
        assertEquals(TrendDirection.FALLING, sampleGames[2].trendDirection)

        assertEquals(0, sampleGames[3].rankDelta)
        assertEquals(TrendDirection.NEW, sampleGames[3].trendDirection)
    }

    @Test
    fun `get trending games filters by rising and new games`() = runTest {
        val repo = FakeGameRepository(sampleGames)
        val useCase = GetTrendingGamesUseCase(repo)

        val rising = useCase(filter = GameFilter.RISING).first()
        assertEquals(listOf(2767030, 2694490), rising.map { it.appId })
    }

    @Test
    fun `get trending games filters by favorites and search query`() = runTest {
        val repo = FakeGameRepository(sampleGames)
        val useCase = GetTrendingGamesUseCase(repo)

        val favorites = useCase(filter = GameFilter.FAVORITES).first()
        assertEquals(listOf(730, 2694490), favorites.map { it.appId })

        val searched = useCase(query = "moba", filter = GameFilter.ALL).first()
        assertEquals(1, searched.size)
        assertEquals("Dota 2", searched.first().name)
    }

    @Test
    fun `refresh trending games propagates offline error`() = runTest {
        val repo = FakeGameRepository(sampleGames, refreshResult = SyncOutcome.Failure(AppError.NetworkUnavailable))
        val useCase = RefreshTrendingGamesUseCase(repo)

        val result = useCase(force = true)
        assertTrue(result is SyncOutcome.Failure)
        assertEquals(AppError.NetworkUnavailable, (result as SyncOutcome.Failure).error)
    }

    private class FakeGameRepository(
        initialGames: List<GameRank>,
        private val refreshResult: SyncOutcome = SyncOutcome.Success,
    ) : GameRepository {
        private val gamesFlow = MutableStateFlow(initialGames)
        override fun observeTrendingGames(): Flow<List<GameRank>> = gamesFlow
        override fun observeFavoriteGames(): Flow<List<GameRank>> =
            gamesFlow.map { list -> list.filter { it.isFavorite } }
        override fun observeGameById(appId: Int): Flow<GameRank?> =
            gamesFlow.map { list -> list.find { it.appId == appId } }
        override fun observeLastSyncTimestamp(syncKey: String): Flow<Long?> = MutableStateFlow(1000L)
        override fun observeConnectivity(): Flow<Boolean> = MutableStateFlow(true)
        override suspend fun refreshTrendingGames(force: Boolean): SyncOutcome = refreshResult
        override suspend fun fetchGameDetail(appId: Int): Result<GameDetail> =
            Result.failure(UnsupportedOperationException())
        override suspend fun toggleFavorite(appId: Int) {
            gamesFlow.value = gamesFlow.value.map {
                if (it.appId == appId) it.copy(isFavorite = !it.isFavorite) else it
            }
        }
    }
}
