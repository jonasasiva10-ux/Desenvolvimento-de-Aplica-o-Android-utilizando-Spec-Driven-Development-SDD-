package com.example.domain.usecase

import com.example.data.mapper.PatchContentSanitizer
import com.example.domain.model.PatchNote
import com.example.domain.model.PlayerMatch
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.PatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatchAndPlayerUseCasesTest {

    @Test
    fun `format rank tier decodes leaderboard, medals, and uncalibrated players`() {
        val formatter = FormatRankTierUseCase()

        assertEquals("Immortal #14", formatter(rankTier = 80, leaderboardRank = 14))
        assertEquals("Immortal", formatter(rankTier = 80, leaderboardRank = null))
        assertEquals("Divine 5", formatter(rankTier = 75, leaderboardRank = null))
        assertEquals("Ancient 3", formatter(rankTier = 63, leaderboardRank = null))
        assertEquals("Legend 1", formatter(rankTier = 51, leaderboardRank = null))
        assertEquals("Sem Rank Calibrado", formatter(rankTier = null, leaderboardRank = null))
    }

    @Test
    fun `calculate player metrics computes winrate and kda accurately including zero deaths`() {
        val calculator = CalculatePlayerMetricsUseCase()
        val matches = listOf(
            PlayerMatch(1L, 1, "Anti-Mage", kills = 12, deaths = 2, assists = 8, durationSeconds = 2100, isWin = true, startTimeEpochSeconds = 100L),
            PlayerMatch(2L, 8, "Juggernaut", kills = 8, deaths = 2, assists = 12, durationSeconds = 2400, isWin = true, startTimeEpochSeconds = 200L),
        )

        val metrics = calculator(wins = 65, losses = 35, recentMatches = matches)
        assertEquals(65.0, metrics.winRatePercent, 0.01)
        assertEquals(10.0, metrics.avgKills, 0.01)
        assertEquals(2.0, metrics.avgDeaths, 0.01)
        assertEquals(10.0, metrics.avgAssists, 0.01)
        assertEquals(10.0, metrics.avgKdaRatio, 0.01)

        val zeroDeathsMatches = listOf(
            PlayerMatch(3L, 14, "Pudge", kills = 10, deaths = 0, assists = 15, durationSeconds = 1800, isWin = true, startTimeEpochSeconds = 300L),
        )
        val perfectMetrics = calculator(wins = 0, losses = 0, recentMatches = zeroDeathsMatches)
        assertEquals(0.0, perfectMetrics.winRatePercent, 0.01)
        assertEquals(25.0, perfectMetrics.avgKdaRatio, 0.01)
    }

    @Test
    fun `patch sanitizer strips steam bbcode, html, and image tokens cleanly`() {
        val raw = "[h2]Balance Update 7.38[/h2]{STEAM_CLAN_IMAGE}/12345/banner.png[list][*]Buffed armor +2[*]Fixed crash &amp; lag[/list]<p>Read more</p>"
        val clean = PatchContentSanitizer.sanitizeFullText(raw)

        assertFalse(clean.contains("[h2]"))
        assertFalse(clean.contains("STEAM_CLAN_IMAGE"))
        assertFalse(clean.contains("<p>"))
        assertTrue(clean.contains("Balance Update 7.38"))
        assertTrue(clean.contains("• Buffed armor +2"))
        assertTrue(clean.contains("Fixed crash & lag"))
    }

    @Test
    fun `patch notes use case filters official patches and sorts newest first`() = runTest {
        val patches = listOf(
            PatchNote("1", 730, "CS2", "Community Spotlight", "Highlight", "Full", "", "Valve", "Blog", 100L, isOfficialPatch = false),
            PatchNote("2", 730, "CS2", "Release Notes 1.40", "Weapon balance", "Full", "", "Valve", "Updates", 300L, isOfficialPatch = true),
            PatchNote("3", 570, "Dota 2", "Patch 7.38b Hotfix", "Hero tweaks", "Full", "", "Valve", "Updates", 200L, isOfficialPatch = true),
        )
        val repo = FakePatchRepository(patches)
        val useCase = GetPatchNotesUseCase(repo)

        val officialOnly = useCase(onlyOfficialPatches = true).first()
        assertEquals(listOf("2", "3"), officialOnly.map { it.gid })

        val dotaOnly = useCase(selectedAppId = 570).first()
        assertEquals(listOf("3"), dotaOnly.map { it.gid })
    }

    private class FakePatchRepository(
        items: List<PatchNote>,
    ) : PatchRepository {
        private val flow = MutableStateFlow(items)
        override fun observeAllPatches(): Flow<List<PatchNote>> = flow
        override fun observePatchesForGame(appId: Int): Flow<List<PatchNote>> =
            flow.map { list -> list.filter { it.appId == appId } }
        override fun observePatchesForFavorites(): Flow<List<PatchNote>> = flow
        override suspend fun refreshPatchesForTopGames(force: Boolean): SyncOutcome = SyncOutcome.Success
        override suspend fun refreshPatchesForGame(appId: Int, gameName: String): SyncOutcome = SyncOutcome.Success
    }
}
