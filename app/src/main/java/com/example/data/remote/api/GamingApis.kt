package com.example.data.remote.api

import com.example.data.remote.dto.OpenDotaPlayerResponseDto
import com.example.data.remote.dto.OpenDotaProPlayerDto
import com.example.data.remote.dto.OpenDotaRecentMatchDto
import com.example.data.remote.dto.OpenDotaWinLossDto
import com.example.data.remote.dto.SteamAppDetailsWrapperDto
import com.example.data.remote.dto.SteamCurrentPlayersResponseDto
import com.example.data.remote.dto.SteamMostPlayedResponseDto
import com.example.data.remote.dto.SteamNewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SteamWebApi {
    @GET("ISteamChartsService/GetMostPlayedGames/v1/")
    suspend fun getMostPlayedGames(): SteamMostPlayedResponseDto

    @GET("ISteamUserStats/GetNumberOfCurrentPlayers/v1/")
    suspend fun getCurrentPlayers(
        @Query("appid") appId: Int,
    ): SteamCurrentPlayersResponseDto

    @GET("ISteamNews/GetNewsForApp/v2/")
    suspend fun getNewsForApp(
        @Query("appid") appId: Int,
        @Query("count") count: Int = 12,
        @Query("maxlength") maxLength: Int = 2400,
        @Query("format") format: String = "json",
    ): SteamNewsResponseDto
}

interface SteamStoreApi {
    @GET("api/appdetails")
    suspend fun getAppDetails(
        @Query("appids") appIds: String,
        @Query("l") language: String = "brazilian",
    ): Map<String, SteamAppDetailsWrapperDto>
}

interface OpenDotaApi {
    @GET("proPlayers")
    suspend fun getProPlayers(): List<OpenDotaProPlayerDto>

    @GET("players/{accountId}")
    suspend fun getPlayerProfile(
        @Path("accountId") accountId: Long,
    ): OpenDotaPlayerResponseDto

    @GET("players/{accountId}/wl")
    suspend fun getPlayerWinLoss(
        @Path("accountId") accountId: Long,
    ): OpenDotaWinLossDto

    @GET("players/{accountId}/recentMatches")
    suspend fun getRecentMatches(
        @Path("accountId") accountId: Long,
    ): List<OpenDotaRecentMatchDto>
}
