package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SteamMostPlayedResponseDto(
    @Json(name = "response") val response: SteamMostPlayedDataDto? = null,
)

@JsonClass(generateAdapter = true)
data class SteamMostPlayedDataDto(
    @Json(name = "rollup_date") val rollupDate: Long? = null,
    @Json(name = "ranks") val ranks: List<SteamRankedGameDto>? = null,
)

@JsonClass(generateAdapter = true)
data class SteamRankedGameDto(
    @Json(name = "rank") val rank: Int? = null,
    @Json(name = "appid") val appId: Int? = null,
    @Json(name = "last_week_rank") val lastWeekRank: Int? = null,
    @Json(name = "peak_in_game") val peakInGame: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SteamCurrentPlayersResponseDto(
    @Json(name = "response") val response: SteamCurrentPlayersDataDto? = null,
)

@JsonClass(generateAdapter = true)
data class SteamCurrentPlayersDataDto(
    @Json(name = "player_count") val playerCount: Int? = null,
    @Json(name = "result") val result: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SteamNewsResponseDto(
    @Json(name = "appnews") val appNews: SteamAppNewsDto? = null,
)

@JsonClass(generateAdapter = true)
data class SteamAppNewsDto(
    @Json(name = "appid") val appId: Int? = null,
    @Json(name = "newsitems") val newsItems: List<SteamNewsItemDto>? = null,
)

@JsonClass(generateAdapter = true)
data class SteamNewsItemDto(
    @Json(name = "gid") val gid: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "contents") val contents: String? = null,
    @Json(name = "feedlabel") val feedLabel: String? = null,
    @Json(name = "date") val date: Long? = null,
    @Json(name = "feedname") val feedName: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class SteamAppDetailsWrapperDto(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: SteamAppDetailsDataDto? = null,
)

@JsonClass(generateAdapter = true)
data class SteamAppDetailsDataDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "short_description") val shortDescription: String? = null,
    @Json(name = "header_image") val headerImage: String? = null,
    @Json(name = "developers") val developers: List<String>? = null,
    @Json(name = "genres") val genres: List<SteamGenreDto>? = null,
    @Json(name = "metacritic") val metacritic: SteamMetacriticDto? = null,
)

@JsonClass(generateAdapter = true)
data class SteamGenreDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "description") val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class SteamMetacriticDto(
    @Json(name = "score") val score: Int? = null,
)
