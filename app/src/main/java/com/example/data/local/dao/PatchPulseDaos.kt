package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.GameRankEntity
import com.example.data.local.entity.PatchNoteEntity
import com.example.data.local.entity.PlayerMatchEntity
import com.example.data.local.entity.PlayerStatsEntity
import com.example.data.local.entity.ProPlayerEntity
import com.example.data.local.entity.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_ranks ORDER BY rank ASC")
    fun observeAllRankedGames(): Flow<List<GameRankEntity>>

    @Query("SELECT * FROM game_ranks WHERE isFavorite = 1 ORDER BY rank ASC")
    fun observeFavoriteGames(): Flow<List<GameRankEntity>>

    @Query("SELECT * FROM game_ranks WHERE appId = :appId LIMIT 1")
    fun observeGameById(appId: Int): Flow<GameRankEntity?>

    @Query("SELECT * FROM game_ranks WHERE appId = :appId LIMIT 1")
    suspend fun getGameById(appId: Int): GameRankEntity?

    @Query("SELECT appId FROM game_ranks WHERE isFavorite = 1")
    suspend fun getFavoriteAppIds(): List<Int>

    @Query("SELECT * FROM game_ranks ORDER BY rank ASC LIMIT :limit")
    suspend fun getTopGames(limit: Int): List<GameRankEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGames(games: List<GameRankEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameRankEntity)

    @Query("DELETE FROM game_ranks WHERE isFavorite = 0 AND appId NOT IN (:keepAppIds)")
    suspend fun pruneNonFavoriteGamesNotIn(keepAppIds: List<Int>)

    @Query("UPDATE game_ranks SET isFavorite = NOT isFavorite WHERE appId = :appId")
    suspend fun toggleFavorite(appId: Int)

    @Query("UPDATE game_ranks SET currentPlayers = :currentPlayers, name = :name, genre = :genre WHERE appId = :appId")
    suspend fun updateLiveMetadata(appId: Int, currentPlayers: Int?, name: String, genre: String)
}

@Dao
interface PatchNoteDao {
    @Query("SELECT * FROM patch_notes ORDER BY publishedAtEpochSeconds DESC LIMIT 120")
    fun observeAllPatches(): Flow<List<PatchNoteEntity>>

    @Query("SELECT * FROM patch_notes WHERE appId = :appId ORDER BY publishedAtEpochSeconds DESC")
    fun observePatchesForGame(appId: Int): Flow<List<PatchNoteEntity>>

    @Query(
        """
        SELECT p.* FROM patch_notes p
        INNER JOIN game_ranks g ON p.appId = g.appId
        WHERE g.isFavorite = 1
        ORDER BY p.publishedAtEpochSeconds DESC
        LIMIT 60
        """,
    )
    fun observePatchesForFavorites(): Flow<List<PatchNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPatches(patches: List<PatchNoteEntity>)

    @Query("DELETE FROM patch_notes WHERE appId = :appId")
    suspend fun deletePatchesForGame(appId: Int)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM pro_players ORDER BY displayOrder ASC")
    fun observeProPlayers(): Flow<List<ProPlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProPlayers(players: List<ProPlayerEntity>)

    @Query("DELETE FROM pro_players")
    suspend fun clearProPlayers()

    @Query("SELECT * FROM player_stats WHERE accountId = :accountId LIMIT 1")
    fun observePlayerStats(accountId: Long): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_matches WHERE accountId = :accountId ORDER BY startTimeEpochSeconds DESC")
    fun observePlayerMatches(accountId: Long): Flow<List<PlayerMatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerStats(stats: PlayerStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerMatches(matches: List<PlayerMatchEntity>)

    @Query("DELETE FROM player_matches WHERE accountId = :accountId")
    suspend fun deleteMatchesForPlayer(accountId: Long)
}

@Dao
interface SyncStatusDao {
    @Query("SELECT lastSyncedAt FROM sync_status WHERE syncKey = :syncKey LIMIT 1")
    fun observeLastSync(syncKey: String): Flow<Long?>

    @Query("SELECT lastSyncedAt FROM sync_status WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getLastSync(syncKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncStatus(status: SyncStatusEntity)
}
