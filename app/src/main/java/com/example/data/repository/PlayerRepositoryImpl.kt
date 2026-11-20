package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.PatchPulseDatabase
import com.example.data.local.dao.PlayerDao
import com.example.data.local.dao.SyncStatusDao
import com.example.data.local.entity.PlayerStatsEntity
import com.example.data.local.entity.SyncStatusEntity
import com.example.data.mapper.combinePlayerStats
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.network.safeApiCall
import com.example.data.remote.api.OpenDotaApi
import com.example.domain.model.AppError
import com.example.domain.model.PlayerStats
import com.example.domain.model.ProPlayer
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.PlayerRepository
import com.example.domain.repository.SyncKeys
import com.example.domain.usecase.CalculatePlayerMetricsUseCase
import com.example.domain.usecase.FormatRankTierUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlayerRepositoryImpl(
    private val database: PatchPulseDatabase,
    private val playerDao: PlayerDao,
    private val syncStatusDao: SyncStatusDao,
    private val openDotaApi: OpenDotaApi,
    private val formatRankTier: FormatRankTierUseCase,
    private val calculateMetrics: CalculatePlayerMetricsUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PlayerRepository {

    companion object {
        private const val PRO_PLAYERS_TTL_MILLIS = 15 * 60 * 1000L // 15 minutos
        private const val PRO_PLAYERS_LIMIT = 60
    }

    override fun observeProPlayers(): Flow<List<ProPlayer>> =
        playerDao.observeProPlayers().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshProPlayers(force: Boolean): SyncOutcome = withContext(ioDispatcher) {
        val now = clock()
        if (!force) {
            val lastSync = syncStatusDao.getLastSync(SyncKeys.PRO_PLAYERS)
            if (lastSync != null && (now - lastSync) < PRO_PLAYERS_TTL_MILLIS) {
                return@withContext SyncOutcome.Success
            }
        }

        val (response, error) = safeApiCall { openDotaApi.getProPlayers() }
        if (error != null || response == null) {
            return@withContext SyncOutcome.Failure(error ?: AppError.Unknown("Falha ao buscar pro players"))
        }

        val filtered = response
            .asSequence()
            .filter { (it.accountId ?: 0L) > 0L && !it.name.isNullOrBlank() && !it.teamName.isNullOrBlank() }
            .distinctBy { it.accountId }
            .take(PRO_PLAYERS_LIMIT)
            .mapIndexedNotNull { index, dto -> dto.toEntity(displayOrder = index) }
            .toList()

        if (filtered.isEmpty()) {
            return@withContext SyncOutcome.Failure(AppError.NotFound)
        }

        database.withTransaction {
            playerDao.clearProPlayers()
            playerDao.upsertProPlayers(filtered)
            syncStatusDao.upsertSyncStatus(SyncStatusEntity(SyncKeys.PRO_PLAYERS, now))
        }

        SyncOutcome.Success
    }

    override fun observePlayerStats(accountId: Long): Flow<PlayerStats?> =
        combine(
            playerDao.observePlayerStats(accountId),
            playerDao.observePlayerMatches(accountId),
        ) { statsEntity, matches ->
            statsEntity?.let {
                combinePlayerStats(
                    entity = it,
                    matchEntities = matches,
                    formatRankTier = formatRankTier,
                    calculateMetrics = calculateMetrics,
                )
            }
        }

    override suspend fun refreshPlayerStats(accountId: Long): SyncOutcome = withContext(ioDispatcher) {
        val now = clock()
        val (profilePair, wlPair, matchesPair) = coroutineScope {
            val profileDef = async { safeApiCall { openDotaApi.getPlayerProfile(accountId) } }
            val wlDef = async { safeApiCall { openDotaApi.getPlayerWinLoss(accountId) } }
            val matchesDef = async { safeApiCall { openDotaApi.getRecentMatches(accountId) } }
            Triple(profileDef.await(), wlDef.await(), matchesDef.await())
        }

        val (profileDto, profileError) = profilePair
        if (profileError != null || profileDto == null) {
            return@withContext SyncOutcome.Failure(profileError ?: AppError.NotFound)
        }

        val profile = profileDto.profile
        if (profile == null && profileDto.rankTier == null && profileDto.leaderboardRank == null) {
            return@withContext SyncOutcome.Failure(AppError.NotFound)
        }

        val wlDto = wlPair.first
        val matchDtos = matchesPair.first.orEmpty().take(15)

        val statsEntity = PlayerStatsEntity(
            accountId = accountId,
            personaName = profile?.proName?.takeIf { it.isNotBlank() }
                ?: profile?.personaName?.takeIf { it.isNotBlank() }
                ?: "Jogador #$accountId",
            avatarUrl = profile?.avatarFull.orEmpty(),
            rankTier = profileDto.rankTier,
            leaderboardRank = profileDto.leaderboardRank,
            wins = wlDto?.win ?: 0,
            losses = wlDto?.lose ?: 0,
            lastUpdatedAt = now,
        )
        val matchEntities = matchDtos.mapNotNull { it.toEntity(accountId) }

        database.withTransaction {
            playerDao.upsertPlayerStats(statsEntity)
            playerDao.deleteMatchesForPlayer(accountId)
            if (matchEntities.isNotEmpty()) {
                playerDao.upsertPlayerMatches(matchEntities)
            }
        }

        SyncOutcome.Success
    }
}
