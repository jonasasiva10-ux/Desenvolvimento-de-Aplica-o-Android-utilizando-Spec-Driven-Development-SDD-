package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "game_ranks")
data class GameRankEntity(
    @PrimaryKey val appId: Int,
    val rank: Int,
    val lastWeekRank: Int,
    val peakInGame: Int,
    val currentPlayers: Int?,
    val name: String,
    val genre: String,
    val headerImageUrl: String,
    val isFavorite: Boolean,
    val lastUpdatedAt: Long,
)

@Entity(
    tableName = "patch_notes",
    indices = [Index(value = ["appId"]), Index(value = ["publishedAtEpochSeconds"])],
)
data class PatchNoteEntity(
    @PrimaryKey val gid: String,
    val appId: Int,
    val gameName: String,
    val title: String,
    val summary: String,
    val fullContent: String,
    val url: String,
    val author: String,
    val feedLabel: String,
    val publishedAtEpochSeconds: Long,
    val isOfficialPatch: Boolean,
)

@Entity(tableName = "pro_players")
data class ProPlayerEntity(
    @PrimaryKey val accountId: Long,
    val proName: String,
    val personaName: String,
    val teamName: String,
    val teamTag: String,
    val avatarUrl: String,
    val countryCode: String,
    val fantasyRole: Int,
    val displayOrder: Int,
)

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val accountId: Long,
    val personaName: String,
    val avatarUrl: String,
    val rankTier: Int?,
    val leaderboardRank: Int?,
    val wins: Int,
    val losses: Int,
    val lastUpdatedAt: Long,
)

@Entity(
    tableName = "player_matches",
    primaryKeys = ["accountId", "matchId"],
    indices = [Index(value = ["accountId"])],
)
data class PlayerMatchEntity(
    val accountId: Long,
    val matchId: Long,
    val heroId: Int,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val durationSeconds: Int,
    val isWin: Boolean,
    val startTimeEpochSeconds: Long,
)

@Entity(tableName = "sync_status")
data class SyncStatusEntity(
    @PrimaryKey val syncKey: String,
    val lastSyncedAt: Long,
)
