package com.example.data.repository

import com.example.data.catalog.SteamGameCatalog
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.PatchNoteDao
import com.example.data.local.dao.SyncStatusDao
import com.example.data.local.entity.SyncStatusEntity
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.network.safeApiCall
import com.example.data.remote.api.SteamWebApi
import com.example.domain.model.AppError
import com.example.domain.model.PatchNote
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.PatchRepository
import com.example.domain.repository.SyncKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PatchRepositoryImpl(
    private val patchNoteDao: PatchNoteDao,
    private val gameDao: GameDao,
    private val syncStatusDao: SyncStatusDao,
    private val steamWebApi: SteamWebApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PatchRepository {

    companion object {
        private const val PATCH_TTL_MILLIS = 5 * 60 * 1000L // 5 minutos
        private const val TRACKED_GAMES_COUNT = 8
        private const val ITEMS_PER_GAME = 8
    }

    override fun observeAllPatches(): Flow<List<PatchNote>> =
        patchNoteDao.observeAllPatches().map { list -> list.map { it.toDomain() } }

    override fun observePatchesForGame(appId: Int): Flow<List<PatchNote>> =
        patchNoteDao.observePatchesForGame(appId).map { list -> list.map { it.toDomain() } }

    override fun observePatchesForFavorites(): Flow<List<PatchNote>> =
        patchNoteDao.observePatchesForFavorites().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshPatchesForTopGames(force: Boolean): SyncOutcome = withContext(ioDispatcher) {
        val now = clock()
        if (!force) {
            val lastSync = syncStatusDao.getLastSync(SyncKeys.PATCH_NOTES)
            if (lastSync != null && (now - lastSync) < PATCH_TTL_MILLIS) {
                return@withContext SyncOutcome.Success
            }
        }

        val cachedTop = gameDao.getTopGames(TRACKED_GAMES_COUNT)
        val targets: List<Pair<Int, String>> = if (cachedTop.isNotEmpty()) {
            val favs = gameDao.getFavoriteAppIds().mapNotNull { id ->
                gameDao.getGameById(id)?.let { it.appId to it.name }
            }
            (favs + cachedTop.map { it.appId to it.name })
                .distinctBy { it.first }
                .take(TRACKED_GAMES_COUNT)
        } else {
            SteamGameCatalog.defaultPatchTrackedGames.map { it.appId to it.name }
        }

        var lastError: AppError? = null
        val collectedEntities = coroutineScope {
            targets.map { (appId, gameName) ->
                async {
                    val (response, error) = safeApiCall {
                        steamWebApi.getNewsForApp(appId = appId, count = ITEMS_PER_GAME)
                    }
                    if (error != null) {
                        lastError = error
                        emptyList()
                    } else {
                        response?.appNews?.newsItems.orEmpty().map { dto ->
                            dto.toEntity(appId = appId, gameName = gameName)
                        }
                    }
                }
            }.awaitAll().flatten()
        }

        if (collectedEntities.isEmpty()) {
            return@withContext SyncOutcome.Failure(lastError ?: AppError.NetworkUnavailable)
        }

        patchNoteDao.upsertPatches(collectedEntities)
        syncStatusDao.upsertSyncStatus(SyncStatusEntity(SyncKeys.PATCH_NOTES, now))
        SyncOutcome.Success
    }

    override suspend fun refreshPatchesForGame(appId: Int, gameName: String): SyncOutcome = withContext(ioDispatcher) {
        val (response, error) = safeApiCall {
            steamWebApi.getNewsForApp(appId = appId, count = 15)
        }
        if (error != null || response == null) {
            return@withContext SyncOutcome.Failure(error ?: AppError.Unknown("Falha ao buscar patches"))
        }

        val entities = response.appNews?.newsItems.orEmpty().map { dto ->
            dto.toEntity(appId = appId, gameName = gameName)
        }
        if (entities.isNotEmpty()) {
            patchNoteDao.upsertPatches(entities)
        }
        SyncOutcome.Success
    }
}
