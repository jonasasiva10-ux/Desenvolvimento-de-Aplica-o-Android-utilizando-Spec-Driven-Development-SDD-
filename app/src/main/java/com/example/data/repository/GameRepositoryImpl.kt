package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.catalog.SteamGameCatalog
import com.example.data.local.PatchPulseDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.SyncStatusDao
import com.example.data.local.entity.SyncStatusEntity
import com.example.data.mapper.PatchContentSanitizer
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.network.ConnectivityMonitor
import com.example.data.network.safeApiCall
import com.example.data.remote.api.SteamStoreApi
import com.example.data.remote.api.SteamWebApi
import com.example.domain.model.AppError
import com.example.domain.model.GameDetail
import com.example.domain.model.GameRank
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.GameRepository
import com.example.domain.repository.SyncKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepositoryImpl(
    private val database: PatchPulseDatabase,
    private val gameDao: GameDao,
    private val syncStatusDao: SyncStatusDao,
    private val steamWebApi: SteamWebApi,
    private val steamStoreApi: SteamStoreApi,
    private val connectivityMonitor: ConnectivityMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : GameRepository {

    companion object {
        private const val TRENDING_TTL_MILLIS = 5 * 60 * 1000L // 5 minutos
        private const val TOP_RANK_LIMIT = 35
        private const val LIVE_PLAYER_CONCURRENT_ENRICH_LIMIT = 12
        private const val UNKNOWN_TITLE_RESOLVE_LIMIT = 10
    }

    override fun observeTrendingGames(): Flow<List<GameRank>> =
        gameDao.observeAllRankedGames().map { entities -> entities.map { it.toDomain() } }

    override fun observeFavoriteGames(): Flow<List<GameRank>> =
        gameDao.observeFavoriteGames().map { entities -> entities.map { it.toDomain() } }

    override fun observeGameById(appId: Int): Flow<GameRank?> =
        gameDao.observeGameById(appId).map { it?.toDomain() }

    override fun observeLastSyncTimestamp(syncKey: String): Flow<Long?> =
        syncStatusDao.observeLastSync(syncKey)

    override fun observeConnectivity(): Flow<Boolean> = connectivityMonitor.isOnline()

    override suspend fun refreshTrendingGames(force: Boolean): SyncOutcome = withContext(ioDispatcher) {
        val now = clock()
        if (!force) {
            val lastSync = syncStatusDao.getLastSync(SyncKeys.TRENDING_GAMES)
            val cachedTop = gameDao.getTopGames(1)
            if (lastSync != null && cachedTop.isNotEmpty() && (now - lastSync) < TRENDING_TTL_MILLIS) {
                return@withContext SyncOutcome.Success
            }
        }

        val (response, error) = safeApiCall { steamWebApi.getMostPlayedGames() }
        if (error != null || response == null) {
            return@withContext SyncOutcome.Failure(error ?: AppError.Unknown("Resposta vazia da Steam API"))
        }

        val rawRanks = response.response?.ranks.orEmpty()
            .filter { (it.appId ?: 0) > 0 && (it.rank ?: 0) > 0 }
            .take(TOP_RANK_LIMIT)
        if (rawRanks.isEmpty()) {
            return@withContext SyncOutcome.Failure(AppError.NotFound)
        }

        val existingByAppId = rawRanks.mapNotNull { dto ->
            val id = dto.appId ?: return@mapNotNull null
            gameDao.getGameById(id)?.let { id to it }
        }.toMap()

        val unknownAppIds = rawRanks
            .mapNotNull { it.appId }
            .filter { id ->
                SteamGameCatalog.lookup(id) == null &&
                    (existingByAppId[id]?.name?.startsWith("Steam App #") != false)
            }
            .take(UNKNOWN_TITLE_RESOLVE_LIMIT)

        val (liveCountsByAppId, resolvedStoreMetaByAppId) = coroutineScope {
            val countsDeferred = rawRanks.take(LIVE_PLAYER_CONCURRENT_ENRICH_LIMIT).mapNotNull { ranked ->
                val id = ranked.appId ?: return@mapNotNull null
                async {
                    val (countDto, _) = safeApiCall { steamWebApi.getCurrentPlayers(id) }
                    id to countDto?.response?.playerCount
                }
            }
            val metadataDeferred = unknownAppIds.map { id ->
                async {
                    val (detailsMap, _) = safeApiCall { steamStoreApi.getAppDetails(id.toString()) }
                    val data = detailsMap?.get(id.toString())?.takeIf { it.success == true }?.data
                    val title = data?.name?.takeIf { it.isNotBlank() }
                    val genre = data?.genres?.firstOrNull()?.description?.takeIf { it.isNotBlank() }
                    id to (title to genre)
                }
            }
            countsDeferred.awaitAll().toMap() to metadataDeferred.awaitAll().toMap()
        }

        val favoriteIds = gameDao.getFavoriteAppIds().toSet()
        val entities = rawRanks.mapNotNull { dto ->
            val id = dto.appId ?: return@mapNotNull null
            val resolvedMeta = resolvedStoreMetaByAppId[id]
            dto.toEntity(
                existingEntity = existingByAppId[id],
                currentPlayers = liveCountsByAppId[id],
                resolvedStoreName = resolvedMeta?.first,
                resolvedStoreGenre = resolvedMeta?.second,
                isFavorite = favoriteIds.contains(id),
                nowMillis = now,
            )
        }

        database.withTransaction {
            gameDao.upsertGames(entities)
            gameDao.pruneNonFavoriteGamesNotIn(entities.map { it.appId })
            syncStatusDao.upsertSyncStatus(SyncStatusEntity(SyncKeys.TRENDING_GAMES, now))
        }

        SyncOutcome.Success
    }

    override suspend fun fetchGameDetail(appId: Int): Result<GameDetail> = withContext(ioDispatcher) {
        val cached = gameDao.getGameById(appId)
        val catalogEntry = SteamGameCatalog.lookup(appId)

        val (liveCount, storeData) = coroutineScope {
            val countDeferred = async {
                safeApiCall { steamWebApi.getCurrentPlayers(appId) }.first?.response?.playerCount
            }
            val detailsDeferred = async {
                safeApiCall { steamStoreApi.getAppDetails(appId.toString()) }
                    .first
                    ?.get(appId.toString())
                    ?.takeIf { it.success == true }
                    ?.data
            }
            countDeferred.await() to detailsDeferred.await()
        }

        val resolvedName = storeData?.name?.takeIf { it.isNotBlank() }
            ?: cached?.name
            ?: catalogEntry?.name
            ?: "Steam App #$appId"
        val resolvedGenres = storeData?.genres
            ?.mapNotNull { it.description?.takeIf { d -> d.isNotBlank() } }
            ?.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(cached?.genre ?: catalogEntry?.genre ?: "PC Gaming")

        if (cached != null) {
            gameDao.updateLiveMetadata(
                appId = appId,
                currentPlayers = liveCount ?: cached.currentPlayers,
                name = resolvedName,
                genre = resolvedGenres.first(),
            )
        }

        if (storeData == null && liveCount == null && cached == null && catalogEntry == null) {
            return@withContext Result.failure(IllegalStateException("Não foi possível carregar dados do jogo #$appId"))
        }

        val description = PatchContentSanitizer.sanitizeFullText(
            storeData?.shortDescription
                ?: "Acompanhe jogadores em tempo real, picos simultâneos e notas oficiais de atualização de $resolvedName.",
        )

        Result.success(
            GameDetail(
                appId = appId,
                name = resolvedName,
                shortDescription = description,
                developer = storeData?.developers?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Desenvolvedor Oficial",
                genres = resolvedGenres,
                headerImageUrl = storeData?.headerImage?.takeIf { it.isNotBlank() }
                    ?: cached?.headerImageUrl
                    ?: SteamGameCatalog.headerImageUrl(appId),
                metacriticScore = storeData?.metacritic?.score,
                currentPlayers = liveCount ?: cached?.currentPlayers,
                peakInGame = cached?.peakInGame ?: (liveCount ?: 0),
                rank = cached?.rank,
                lastWeekRank = cached?.lastWeekRank,
                isFavorite = cached?.isFavorite ?: false,
            ),
        )
    }

    override suspend fun toggleFavorite(appId: Int) = withContext(ioDispatcher) {
        gameDao.toggleFavorite(appId)
    }
}
