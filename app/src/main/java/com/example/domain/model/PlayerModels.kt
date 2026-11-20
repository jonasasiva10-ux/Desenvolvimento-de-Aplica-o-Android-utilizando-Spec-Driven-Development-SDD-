package com.example.domain.model

data class ProPlayer(
    val accountId: Long,
    val proName: String,
    val personaName: String,
    val teamName: String,
    val teamTag: String,
    val avatarUrl: String,
    val countryCode: String,
    val fantasyRole: Int,
) {
    val roleLabel: String
        get() = when (fantasyRole) {
            1 -> "Core"
            2 -> "Support"
            else -> "Pro"
        }
}

data class PlayerMatch(
    val matchId: Long,
    val heroId: Int,
    val heroName: String,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val durationSeconds: Int,
    val isWin: Boolean,
    val startTimeEpochSeconds: Long,
) {
    val kdaFormatted: String
        get() = "$kills / $deaths / $assists"
}

data class PlayerPerformanceMetrics(
    val winRatePercent: Double,
    val avgKills: Double,
    val avgDeaths: Double,
    val avgAssists: Double,
    val avgKdaRatio: Double,
)

data class PlayerStats(
    val accountId: Long,
    val personaName: String,
    val avatarUrl: String,
    val rankTier: Int?,
    val rankLabel: String,
    val leaderboardRank: Int?,
    val wins: Int,
    val losses: Int,
    val metrics: PlayerPerformanceMetrics,
    val recentMatches: List<PlayerMatch>,
    val lastUpdatedAt: Long,
)
