package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenDotaProPlayerDto(
    @Json(name = "account_id") val accountId: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "personaname") val personaName: String? = null,
    @Json(name = "team_name") val teamName: String? = null,
    @Json(name = "team_tag") val teamTag: String? = null,
    @Json(name = "avatarfull") val avatarFull: String? = null,
    @Json(name = "loccountrycode") val countryCode: String? = null,
    @Json(name = "fantasy_role") val fantasyRole: Int? = null,
    @Json(name = "is_pro") val isPro: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class OpenDotaPlayerResponseDto(
    @Json(name = "rank_tier") val rankTier: Int? = null,
    @Json(name = "leaderboard_rank") val leaderboardRank: Int? = null,
    @Json(name = "profile") val profile: OpenDotaProfileDto? = null,
)

@JsonClass(generateAdapter = true)
data class OpenDotaProfileDto(
    @Json(name = "account_id") val accountId: Long? = null,
    @Json(name = "personaname") val personaName: String? = null,
    @Json(name = "name") val proName: String? = null,
    @Json(name = "avatarfull") val avatarFull: String? = null,
)

@JsonClass(generateAdapter = true)
data class OpenDotaWinLossDto(
    @Json(name = "win") val win: Int? = null,
    @Json(name = "lose") val lose: Int? = null,
)

@JsonClass(generateAdapter = true)
data class OpenDotaRecentMatchDto(
    @Json(name = "match_id") val matchId: Long? = null,
    @Json(name = "player_slot") val playerSlot: Int? = null,
    @Json(name = "radiant_win") val radiantWin: Boolean? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "hero_id") val heroId: Int? = null,
    @Json(name = "start_time") val startTime: Long? = null,
    @Json(name = "kills") val kills: Int? = null,
    @Json(name = "deaths") val deaths: Int? = null,
    @Json(name = "assists") val assists: Int? = null,
)
