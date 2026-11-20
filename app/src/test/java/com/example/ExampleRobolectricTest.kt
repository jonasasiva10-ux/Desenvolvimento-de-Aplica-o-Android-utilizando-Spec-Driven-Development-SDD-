package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PatchPulseDatabase
import com.example.data.local.entity.GameRankEntity
import com.example.data.local.entity.PatchNoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: PatchPulseDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PatchPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun `app name resource matches SeuJogo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SeuJogo", appName)
    }

    @Test
    fun `room offline persistence stores games, toggles favorites, and queries favorite patches`() = runTest {
        val gameDao = database.gameDao()
        val patchDao = database.patchNoteDao()

        gameDao.upsertGames(
            listOf(
                GameRankEntity(730, 1, 1, 1_400_000, 950_000, "Counter-Strike 2", "FPS Tático", "", false, 1000L),
                GameRankEntity(570, 2, 3, 620_000, 480_000, "Dota 2", "MOBA", "", false, 1000L),
            ),
        )
        patchDao.upsertPatches(
            listOf(
                PatchNoteEntity("p1", 730, "Counter-Strike 2", "CS2 Patch 1.40", "Balance", "Full", "", "Valve", "Updates", 500L, true),
                PatchNoteEntity("p2", 570, "Dota 2", "Patch 7.38", "Map rework", "Full", "", "Valve", "Updates", 600L, true),
            ),
        )

        gameDao.toggleFavorite(730)

        val favorites = gameDao.observeFavoriteGames().first()
        assertEquals(1, favorites.size)
        assertEquals(730, favorites.first().appId)
        assertTrue(favorites.first().isFavorite)

        val favoritePatches = patchDao.observePatchesForFavorites().first()
        assertEquals(1, favoritePatches.size)
        assertEquals("p1", favoritePatches.first().gid)
    }
}
