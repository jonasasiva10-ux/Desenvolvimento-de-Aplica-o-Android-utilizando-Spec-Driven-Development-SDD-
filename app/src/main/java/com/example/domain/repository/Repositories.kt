package com.example.domain.repository

import com.example.domain.model.GameDetail
import com.example.domain.model.GameRank
import com.example.domain.model.PatchNote
import com.example.domain.model.PlayerStats
import com.example.domain.model.ProPlayer
import com.example.domain.model.SyncOutcome
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeTrendingGames(): Flow<List<GameRank>>
    fun observeFavoriteGames(): Flow<List<GameRank>>
    fun observeGameById(appId: Int): Flow<GameRank?>
    fun observeLastSyncTimestamp(syncKey: String): Flow<Long?>
    fun observeConnectivity(): Flow<Boolean>
    suspend fun refreshTrendingGames(force: Boolean = false): SyncOutcome
    suspend fun fetchGameDetail(appId: Int): Result<GameDetail>
    suspend fun toggleFavorite(appId: Int)
}

interface PatchRepository {
    fun observeAllPatches(): Flow<List<PatchNote>>
    fun observePatchesForGame(appId: Int): Flow<List<PatchNote>>
    fun observePatchesForFavorites(): Flow<List<PatchNote>>
    suspend fun refreshPatchesForTopGames(force: Boolean = false): SyncOutcome
    suspend fun refreshPatchesForGame(appId: Int, gameName: String): SyncOutcome
}

interface PlayerRepository {
    fun observeProPlayers(): Flow<List<ProPlayer>>
    suspend fun refreshProPlayers(force: Boolean = false): SyncOutcome
    fun observePlayerStats(accountId: Long): Flow<PlayerStats?>
    suspend fun refreshPlayerStats(accountId: Long): SyncOutcome
}

object SyncKeys {
    const val TRENDING_GAMES = "trending_games"
    const val PATCH_NOTES = "patch_notes"
    const val PRO_PLAYERS = "pro_players"
}
