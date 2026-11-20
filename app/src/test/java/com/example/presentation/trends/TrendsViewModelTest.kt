package com.example.presentation.trends

import com.example.domain.model.AppError
import com.example.domain.model.GameDetail
import com.example.domain.model.GameFilter
import com.example.domain.model.GameRank
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.GameRepository
import com.example.domain.usecase.GetTrendingGamesUseCase
import com.example.domain.usecase.ObserveSyncMetadataUseCase
import com.example.domain.usecase.RefreshTrendingGamesUseCase
import com.example.domain.usecase.ToggleFavoriteGameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `offline failure preserves cached games without blocking error state`() = runTest {
        val cached = listOf(
            GameRank(730, 1, 2, 1_300_000, 900_000, "Counter-Strike 2", "FPS", "", false, 5000L),
            GameRank(570, 2, 1, 600_000, 410_000, "Dota 2", "MOBA", "", true, 5000L),
        )
        val repo = InMemoryGameRepository(
            initialGames = cached,
            online = false,
            syncOutcome = SyncOutcome.Failure(AppError.NetworkUnavailable),
        )
        val viewModel = buildViewModel(repo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.games.size)
        assertEquals(false, state.isOnline)
        assertNull(state.error)

        viewModel.onFilterSelected(GameFilter.FAVORITES)
        advanceUntilIdle()
        assertEquals(listOf(570), viewModel.uiState.value.games.map { it.appId })

        collectJob.cancel()
    }

    @Test
    fun `offline failure with empty cache exposes typed network error`() = runTest {
        val repo = InMemoryGameRepository(
            initialGames = emptyList(),
            online = false,
            syncOutcome = SyncOutcome.Failure(AppError.NetworkUnavailable),
        )
        val viewModel = buildViewModel(repo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.games.size)
        assertEquals(AppError.NetworkUnavailable, state.error)

        collectJob.cancel()
    }

    private fun buildViewModel(repo: GameRepository): TrendsViewModel = TrendsViewModel(
        getTrendingGames = GetTrendingGamesUseCase(repo),
        refreshTrendingGames = RefreshTrendingGamesUseCase(repo),
        toggleFavoriteGame = ToggleFavoriteGameUseCase(repo),
        observeSyncMetadata = ObserveSyncMetadataUseCase(repo),
    )

    private class InMemoryGameRepository(
        initialGames: List<GameRank>,
        online: Boolean,
        private val syncOutcome: SyncOutcome,
    ) : GameRepository {
        private val games = MutableStateFlow(initialGames)
        private val connectivity = MutableStateFlow(online)
        override fun observeTrendingGames(): Flow<List<GameRank>> = games
        override fun observeFavoriteGames(): Flow<List<GameRank>> = games.map { it.filter { g -> g.isFavorite } }
        override fun observeGameById(appId: Int): Flow<GameRank?> = games.map { it.find { g -> g.appId == appId } }
        override fun observeLastSyncTimestamp(syncKey: String): Flow<Long?> = MutableStateFlow(5000L)
        override fun observeConnectivity(): Flow<Boolean> = connectivity
        override suspend fun refreshTrendingGames(force: Boolean): SyncOutcome = syncOutcome
        override suspend fun fetchGameDetail(appId: Int): Result<GameDetail> = Result.failure(IllegalStateException())
        override suspend fun toggleFavorite(appId: Int) {
            games.value = games.value.map { if (it.appId == appId) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }
}
