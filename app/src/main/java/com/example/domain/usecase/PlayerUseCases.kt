package com.example.domain.usecase

import com.example.domain.model.PlayerMatch
import com.example.domain.model.PlayerPerformanceMetrics
import com.example.domain.model.PlayerStats
import com.example.domain.model.ProPlayer
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class FormatRankTierUseCase {
    operator fun invoke(rankTier: Int?, leaderboardRank: Int? = null): String {
        if (leaderboardRank != null && leaderboardRank > 0) {
            return "Immortal #$leaderboardRank"
        }
        if (rankTier == null || rankTier <= 0) {
            return "Sem Rank Calibrado"
        }
        val medalIndex = rankTier / 10
        val star = rankTier % 10
        val medalName = when (medalIndex) {
            1 -> "Herald"
            2 -> "Guardian"
            3 -> "Crusader"
            4 -> "Archon"
            5 -> "Legend"
            6 -> "Ancient"
            7 -> "Divine"
            8 -> "Immortal"
            else -> "Competitivo"
        }
        return if (medalIndex >= 8 || star <= 0) medalName else "$medalName $star"
    }
}

class CalculatePlayerMetricsUseCase {
    operator fun invoke(
        wins: Int,
        losses: Int,
        recentMatches: List<PlayerMatch>,
    ): PlayerPerformanceMetrics {
        val totalGames = (wins + losses).coerceAtLeast(0)
        val winRate = if (totalGames == 0) {
            0.0
        } else {
            ((wins.toDouble() / totalGames.toDouble()) * 1000.0).roundToInt() / 10.0
        }

        if (recentMatches.isEmpty()) {
            return PlayerPerformanceMetrics(
                winRatePercent = winRate,
                avgKills = 0.0,
                avgDeaths = 0.0,
                avgAssists = 0.0,
                avgKdaRatio = 0.0,
            )
        }

        val count = recentMatches.size.toDouble()
        val avgKills = ((recentMatches.sumOf { it.kills } / count) * 10.0).roundToInt() / 10.0
        val avgDeaths = ((recentMatches.sumOf { it.deaths } / count) * 10.0).roundToInt() / 10.0
        val avgAssists = ((recentMatches.sumOf { it.assists } / count) * 10.0).roundToInt() / 10.0
        val denominator = avgDeaths.coerceAtLeast(1.0)
        val kdaRatio = (((avgKills + avgAssists) / denominator) * 100.0).roundToInt() / 100.0

        return PlayerPerformanceMetrics(
            winRatePercent = winRate,
            avgKills = avgKills,
            avgDeaths = avgDeaths,
            avgAssists = avgAssists,
            avgKdaRatio = kdaRatio,
        )
    }
}

class GetProPlayersUseCase(
    private val playerRepository: PlayerRepository,
) {
    operator fun invoke(query: String = ""): Flow<List<ProPlayer>> {
        val normalized = query.trim().lowercase()
        return playerRepository.observeProPlayers().map { players ->
            if (normalized.isEmpty()) {
                players
            } else {
                players.filter { player ->
                    player.proName.lowercase().contains(normalized) ||
                        player.personaName.lowercase().contains(normalized) ||
                        player.teamName.lowercase().contains(normalized) ||
                        player.teamTag.lowercase().contains(normalized) ||
                        player.accountId.toString().contains(normalized)
                }
            }
        }
    }
}

class RefreshProPlayersUseCase(
    private val playerRepository: PlayerRepository,
) {
    suspend operator fun invoke(force: Boolean = false): SyncOutcome {
        return playerRepository.refreshProPlayers(force)
    }
}

class GetPlayerStatsUseCase(
    private val playerRepository: PlayerRepository,
) {
    fun observe(accountId: Long): Flow<PlayerStats?> = playerRepository.observePlayerStats(accountId)

    suspend fun refresh(accountId: Long): SyncOutcome = playerRepository.refreshPlayerStats(accountId)
}
