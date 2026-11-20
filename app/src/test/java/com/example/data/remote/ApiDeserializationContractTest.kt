package com.example.data.remote

import com.example.data.mapper.toEntity
import com.example.data.remote.dto.OpenDotaPlayerResponseDto
import com.example.data.remote.dto.OpenDotaProPlayerDto
import com.example.data.remote.dto.OpenDotaRecentMatchDto
import com.example.data.remote.dto.OpenDotaWinLossDto
import com.example.data.remote.dto.SteamAppDetailsWrapperDto
import com.example.data.remote.dto.SteamCurrentPlayersResponseDto
import com.example.data.remote.dto.SteamMostPlayedResponseDto
import com.example.data.remote.dto.SteamNewsResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiDeserializationContractTest {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `parses official Steam GetMostPlayedGames payload including null last_week_rank`() {
        val json = """
            {
              "response": {
                "rollup_date": 1742428800,
                "ranks": [
                  {"rank": 1, "appid": 730, "last_week_rank": 1, "peak_in_game": 1624510},
                  {"rank": 2, "appid": 999999, "peak_in_game": 112000}
                ]
              }
            }
        """.trimIndent()

        val dto = moshi.adapter(SteamMostPlayedResponseDto::class.java).fromJson(json)
        assertNotNull(dto)
        val ranks = dto?.response?.ranks.orEmpty()
        assertEquals(2, ranks.size)

        val secondEntity = ranks[1].toEntity(
            existingEntity = null,
            currentPlayers = 95_000,
            resolvedStoreName = "Novo Hit Indie",
            resolvedStoreGenre = "Ação",
            isFavorite = false,
            nowMillis = 1234L,
        )
        assertNotNull(secondEntity)
        assertEquals(0, secondEntity?.lastWeekRank)
        assertEquals("Novo Hit Indie", secondEntity?.name)
    }

    @Test
    fun `parses official Steam GetNumberOfCurrentPlayers and GetNewsForApp payloads`() {
        val playersJson = """{"response":{"player_count":1045210,"result":1}}"""
        val playersDto = moshi.adapter(SteamCurrentPlayersResponseDto::class.java).fromJson(playersJson)
        assertEquals(1045210, playersDto?.response?.playerCount)

        val newsJson = """
            {
              "appnews": {
                "appid": 730,
                "newsitems": [
                  {
                    "gid": "51234567890",
                    "title": "Counter-Strike 2 Update",
                    "url": "https://steamstore-a.akamaihd.net/news/externalpost/1",
                    "author": "",
                    "contents": "[h2]Gameplay[/h2][list][*]Adjusted smoke duration[/list]",
                    "feedlabel": "Product Update",
                    "date": 1742400000,
                    "feedname": "steam_community_announcements",
                    "tags": ["patchnotes"]
                  }
                ]
              }
            }
        """.trimIndent()
        val newsDto = moshi.adapter(SteamNewsResponseDto::class.java).fromJson(newsJson)
        val item = newsDto?.appNews?.newsItems?.firstOrNull()
        assertNotNull(item)
        val entity = item!!.toEntity(appId = 730, gameName = "Counter-Strike 2")
        assertTrue(entity.isOfficialPatch)
        assertEquals("Equipe Oficial", entity.author)
        assertTrue(entity.fullContent.contains("• Adjusted smoke duration"))
    }

    @Test
    fun `parses official Steam Store appdetails dynamic map including failed entries`() {
        val storeJson = """
            {
              "730": {
                "success": true,
                "data": {
                  "name": "Counter-Strike 2",
                  "short_description": "A nova era competitiva.",
                  "header_image": "https://shared.akamai.steamstatic.com/730/header.jpg",
                  "developers": ["Valve"],
                  "genres": [{"id": "1", "description": "Ação"}],
                  "metacritic": {"score": 82}
                }
              },
              "9999999": {
                "success": false
              }
            }
        """.trimIndent()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, SteamAppDetailsWrapperDto::class.java)
        val map = moshi.adapter<Map<String, SteamAppDetailsWrapperDto>>(mapType).fromJson(storeJson)

        assertNotNull(map)
        assertEquals(true, map?.get("730")?.success)
        assertEquals("Counter-Strike 2", map?.get("730")?.data?.name)
        assertEquals(82, map?.get("730")?.data?.metacritic?.score)
        assertEquals(false, map?.get("9999999")?.success)
    }

    @Test
    fun `parses official OpenDota proPlayers, player profile, wl, and nullable recentMatches`() {
        val proJson = """
            [
              {"account_id": 321580662, "name": "Yatoro", "personaname": "Raddan", "team_name": "Team Spirit", "team_tag": "TSpirit", "avatarfull": "https://img/y.jpg", "loccountrycode": "ua", "fantasy_role": 1, "is_pro": true},
              {"account_id": null, "name": null, "team_name": null}
            ]
        """.trimIndent()
        val proListType = Types.newParameterizedType(List::class.java, OpenDotaProPlayerDto::class.java)
        val pros = moshi.adapter<List<OpenDotaProPlayerDto>>(proListType).fromJson(proJson).orEmpty()
        assertEquals(2, pros.size)
        assertNotNull(pros[0].toEntity(0))
        assertEquals(null, pros[1].toEntity(1))

        val playerJson = """{"rank_tier": 80, "leaderboard_rank": 5, "profile": {"account_id": 321580662, "personaname": "Yatoro", "avatarfull": "https://img/y.jpg"}}"""
        val playerDto = moshi.adapter(OpenDotaPlayerResponseDto::class.java).fromJson(playerJson)
        assertEquals(80, playerDto?.rankTier)
        assertEquals(5, playerDto?.leaderboardRank)

        val wlJson = """{"win": 2100, "lose": 1750}"""
        val wlDto = moshi.adapter(OpenDotaWinLossDto::class.java).fromJson(wlJson)
        assertEquals(2100, wlDto?.win)

        val matchesJson = """
            [
              {"match_id": 8001, "player_slot": 1, "radiant_win": true, "duration": 2210, "hero_id": 8, "start_time": 1742400000, "kills": 14, "deaths": 2, "assists": 9},
              {"match_id": 8002, "player_slot": 130, "radiant_win": null, "duration": 1800, "hero_id": 1, "start_time": 1742300000, "kills": 3, "deaths": 5, "assists": 4}
            ]
        """.trimIndent()
        val matchesListType = Types.newParameterizedType(List::class.java, OpenDotaRecentMatchDto::class.java)
        val matches = moshi.adapter<List<OpenDotaRecentMatchDto>>(matchesListType).fromJson(matchesJson).orEmpty()
        assertEquals(2, matches.size)
        val m1 = matches[0].toEntity(321580662L)
        val m2 = matches[1].toEntity(321580662L)
        assertTrue(m1!!.isWin)
        assertTrue(m2!!.isWin) // Dire slot >= 128 and radiant_win null -> false
        assertFalse(m1.durationSeconds == 0)
    }
}
