package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PatchPulseDatabase
import com.example.data.network.ConnectivityMonitor
import com.example.data.remote.api.SteamStoreApi
import com.example.data.remote.api.SteamWebApi
import com.example.data.remote.dto.SteamAppDetailsWrapperDto
import com.example.data.remote.dto.SteamCurrentPlayersDataDto
import com.example.data.remote.dto.SteamCurrentPlayersResponseDto
import com.example.data.remote.dto.SteamMostPlayedDataDto
import com.example.data.remote.dto.SteamMostPlayedResponseDto
import com.example.data.remote.dto.SteamNewsResponseDto
import com.example.data.remote.dto.SteamRankedGameDto
import com.example.domain.model.AppError
import com.example.domain.model.SyncOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineFirstGameRepositoryTest {

    private lateinit var database: PatchPulseDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PatchPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `refresh syncs remote rankings into Room and preserves local favorites across updates`() = runTest {
        var now = 10_000L
        val fakeWebApi = FakeSteamWebApi(
            ranks = listOf(
                SteamRankedGameDto(rank = 1, appId = 730, lastWeekRank = 1, peakInGame = 1_500_000),
                SteamRankedGameDto(rank = 2, appId = 570, lastWeekRank = 3, peakInGame = 690_000),
            ),
        )
        val repository = GameRepositoryImpl(
            database = database,
            gameDao = database.gameDao(),
            syncStatusDao = database.syncStatusDao(),
            steamWebApi = fakeWebApi,
            steamStoreApi = FakeSteamStoreApi(),
            connectivityMonitor = FakeConnectivityMonitor(true),
            ioDispatcher = Dispatchers.Unconfined,
            clock = { now },
        )

        val firstSync = repository.refreshTrendingGames(force = true)
        assertEquals(SyncOutcome.Success, firstSync)

        // Pin Counter-Strike 2 locally
        repository.toggleFavorite(730)

        // Force a second sync later — favorite flag must survive Room upsert
        now += 600_000L
        val secondSync = repository.refreshTrendingGames(force = true)
        assertEquals(SyncOutcome.Success, secondSync)

        val games = repository.observeTrendingGames().first()
        assertEquals(2, games.size)
        assertEquals("Counter-Strike 2", games[0].name)
        assertTrue(games[0].isFavorite)
        assertEquals(850_000, games[0].currentPlayers)
    }

    @Test
    fun `offline failure returns typed NetworkUnavailable while keeping Room cache intact`() = runTest {
        val fakeWebApi = FakeSteamWebApi(
            ranks = listOf(SteamRankedGameDto(rank = 1, appId = 730, lastWeekRank = 1, peakInGame = 1_400_000)),
        )
        val repository = GameRepositoryImpl(
            database = database,
            gameDao = database.gameDao(),
            syncStatusDao = database.syncStatusDao(),
            steamWebApi = fakeWebApi,
            steamStoreApi = FakeSteamStoreApi(),
            connectivityMonitor = FakeConnectivityMonitor(false),
            ioDispatcher = Dispatchers.Unconfined,
        )

        // Initial online sync populates cache
        assertEquals(SyncOutcome.Success, repository.refreshTrendingGames(force = true))

        // Simulate network outage on next sync
        fakeWebApi.failNextCall = true
        val offlineResult = repository.refreshTrendingGames(force = true)
        assertTrue(offlineResult is SyncOutcome.Failure)
        assertEquals(AppError.NetworkUnavailable, (offlineResult as SyncOutcome.Failure).error)

        // Cached game remains readable offline
        val cachedGames = repository.observeTrendingGames().first()
        assertEquals(1, cachedGames.size)
        assertEquals(730, cachedGames.first().appId)
    }

    private class FakeSteamWebApi(
        private val ranks: List<SteamRankedGameDto>,
        var failNextCall: Boolean = false,
    ) : SteamWebApi {
        override suspend fun getMostPlayedGames(): SteamMostPlayedResponseDto {
            if (failNextCall) throw IOException("No network")
            return SteamMostPlayedResponseDto(SteamMostPlayedDataDto(rollupDate = 1000L, ranks = ranks))
        }

        override suspend fun getCurrentPlayers(appId: Int): SteamCurrentPlayersResponseDto {
            if (failNextCall) throw IOException("No network")
            return SteamCurrentPlayersResponseDto(SteamCurrentPlayersDataDto(playerCount = 850_000, result = 1))
        }

        override suspend fun getNewsForApp(
            appId: Int,
            count: Int,
            maxLength: Int,
            format: String,
        ): SteamNewsResponseDto = SteamNewsResponseDto(null)
    }

    private class FakeSteamStoreApi : SteamStoreApi {
        override suspend fun getAppDetails(
            appIds: String,
            language: String,
        ): Map<String, SteamAppDetailsWrapperDto> = emptyMap()
    }

    private class FakeConnectivityMonitor(initialOnline: Boolean) : ConnectivityMonitor {
        private val flow = MutableStateFlow(initialOnline)
        override fun isOnline(): Flow<Boolean> = flow
        override fun isCurrentlyConnected(): Boolean = flow.value
    }
}
