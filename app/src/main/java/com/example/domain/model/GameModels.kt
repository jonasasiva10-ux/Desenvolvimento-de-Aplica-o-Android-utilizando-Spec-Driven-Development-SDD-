package com.example.domain.model

enum class TrendDirection {
    RISING,
    FALLING,
    STEADY,
    NEW,
}

enum class GameFilter {
    ALL,
    RISING,
    FAVORITES,
}

data class GameRank(
    val appId: Int,
    val rank: Int,
    val lastWeekRank: Int,
    val peakInGame: Int,
    val currentPlayers: Int?,
    val name: String,
    val genre: String,
    val headerImageUrl: String,
    val isFavorite: Boolean,
    val lastUpdatedAt: Long,
) {
    val rankDelta: Int
        get() = if (lastWeekRank <= 0) 0 else lastWeekRank - rank

    val trendDirection: TrendDirection
        get() = when {
            lastWeekRank <= 0 -> TrendDirection.NEW
            rankDelta > 0 -> TrendDirection.RISING
            rankDelta < 0 -> TrendDirection.FALLING
            else -> TrendDirection.STEADY
        }
}

data class GameDetail(
    val appId: Int,
    val name: String,
    val shortDescription: String,
    val developer: String,
    val genres: List<String>,
    val headerImageUrl: String,
    val metacriticScore: Int?,
    val currentPlayers: Int?,
    val peakInGame: Int,
    val rank: Int?,
    val lastWeekRank: Int?,
    val isFavorite: Boolean,
)
