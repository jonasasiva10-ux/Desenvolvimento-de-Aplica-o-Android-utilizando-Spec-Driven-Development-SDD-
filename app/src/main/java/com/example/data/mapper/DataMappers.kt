package com.example.data.mapper

import com.example.data.catalog.DotaHeroCatalog
import com.example.data.catalog.SteamGameCatalog
import com.example.data.local.entity.GameRankEntity
import com.example.data.local.entity.PatchNoteEntity
import com.example.data.local.entity.PlayerMatchEntity
import com.example.data.local.entity.PlayerStatsEntity
import com.example.data.local.entity.ProPlayerEntity
import com.example.data.remote.dto.OpenDotaProPlayerDto
import com.example.data.remote.dto.OpenDotaRecentMatchDto
import com.example.data.remote.dto.SteamNewsItemDto
import com.example.data.remote.dto.SteamRankedGameDto
import com.example.domain.model.GameRank
import com.example.domain.model.PatchNote
import com.example.domain.model.PlayerMatch
import com.example.domain.model.PlayerStats
import com.example.domain.model.ProPlayer
import com.example.domain.usecase.CalculatePlayerMetricsUseCase
import com.example.domain.usecase.FormatRankTierUseCase

fun SteamRankedGameDto.toEntity(
    existingEntity: GameRankEntity?,
    currentPlayers: Int?,
    resolvedStoreName: String? = null,
    resolvedStoreGenre: String? = null,
    isFavorite: Boolean,
    nowMillis: Long,
): GameRankEntity? {
    val safeAppId = appId?.takeIf { it > 0 } ?: return null
    val safeRank = rank?.takeIf { it > 0 } ?: return null
    val catalogEntry = SteamGameCatalog.lookup(safeAppId)
    val resolvedName = resolvedStoreName?.takeIf { it.isNotBlank() }
        ?: existingEntity?.name?.takeIf { !it.startsWith("Steam App #") }
        ?: catalogEntry?.name
        ?: "Steam App #$safeAppId"
    val resolvedGenre = resolvedStoreGenre?.takeIf { it.isNotBlank() }
        ?: existingEntity?.genre?.takeIf { it != "PC Gaming" }
        ?: catalogEntry?.genre
        ?: "PC Gaming"
    return GameRankEntity(
        appId = safeAppId,
        rank = safeRank,
        lastWeekRank = lastWeekRank ?: 0,
        peakInGame = peakInGame ?: 0,
        currentPlayers = currentPlayers ?: existingEntity?.currentPlayers,
        name = resolvedName,
        genre = resolvedGenre,
        headerImageUrl = SteamGameCatalog.headerImageUrl(safeAppId),
        isFavorite = isFavorite,
        lastUpdatedAt = nowMillis,
    )
}

fun GameRankEntity.toDomain(): GameRank = GameRank(
    appId = appId,
    rank = rank,
    lastWeekRank = lastWeekRank,
    peakInGame = peakInGame,
    currentPlayers = currentPlayers,
    name = name,
    genre = genre,
    headerImageUrl = headerImageUrl,
    isFavorite = isFavorite,
    lastUpdatedAt = lastUpdatedAt,
)

fun SteamNewsItemDto.toEntity(
    appId: Int,
    gameName: String,
): PatchNoteEntity {
    val safeTitle = title?.takeIf { it.isNotBlank() } ?: "Atualização de $gameName"
    val safeDate = date ?: 0L
    val sanitizedFull = PatchContentSanitizer.sanitizeFullText(contents)
    val summary = PatchContentSanitizer.buildSummary(sanitizedFull)
    val official = PatchContentSanitizer.isOfficialPatch(safeTitle, feedName, tags)
    return PatchNoteEntity(
        gid = gid?.takeIf { it.isNotBlank() } ?: "${appId}_${safeDate}_${safeTitle.hashCode()}",
        appId = appId,
        gameName = gameName,
        title = safeTitle,
        summary = summary,
        fullContent = sanitizedFull,
        url = url.orEmpty(),
        author = author?.takeIf { it.isNotBlank() } ?: "Equipe Oficial",
        feedLabel = feedLabel?.takeIf { it.isNotBlank() } ?: "Steam News",
        publishedAtEpochSeconds = safeDate,
        isOfficialPatch = official,
    )
}

fun PatchNoteEntity.toDomain(): PatchNote = PatchNote(
    gid = gid,
    appId = appId,
    gameName = gameName,
    title = title,
    summary = summary,
    fullContent = fullContent,
    url = url,
    author = author,
    feedLabel = feedLabel,
    publishedAtEpochSeconds = publishedAtEpochSeconds,
    isOfficialPatch = isOfficialPatch,
)

fun OpenDotaProPlayerDto.toEntity(displayOrder: Int): ProPlayerEntity? {
    val safeAccountId = accountId?.takeIf { it > 0L } ?: return null
    return ProPlayerEntity(
        accountId = safeAccountId,
        proName = name?.takeIf { it.isNotBlank() }
            ?: personaName?.takeIf { it.isNotBlank() }
            ?: "Player #$safeAccountId",
        personaName = personaName?.takeIf { it.isNotBlank() } ?: name.orEmpty(),
        teamName = teamName?.takeIf { it.isNotBlank() } ?: "Agente Livre",
        teamTag = teamTag?.takeIf { it.isNotBlank() } ?: "PRO",
        avatarUrl = avatarFull.orEmpty(),
        countryCode = countryCode?.uppercase().orEmpty(),
        fantasyRole = fantasyRole ?: 0,
        displayOrder = displayOrder,
    )
}

fun ProPlayerEntity.toDomain(): ProPlayer = ProPlayer(
    accountId = accountId,
    proName = proName,
    personaName = personaName,
    teamName = teamName,
    teamTag = teamTag,
    avatarUrl = avatarUrl,
    countryCode = countryCode,
    fantasyRole = fantasyRole,
)

fun OpenDotaRecentMatchDto.toEntity(accountId: Long): PlayerMatchEntity? {
    val safeMatchId = matchId?.takeIf { it > 0L } ?: return null
    val safeSlot = playerSlot ?: 0
    val safeRadiantWin = radiantWin ?: false
    val isRadiant = safeSlot < 128
    val isWin = (isRadiant && safeRadiantWin) || (!isRadiant && !safeRadiantWin)
    return PlayerMatchEntity(
        accountId = accountId,
        matchId = safeMatchId,
        heroId = heroId ?: 0,
        kills = kills ?: 0,
        deaths = deaths ?: 0,
        assists = assists ?: 0,
        durationSeconds = duration ?: 0,
        isWin = isWin,
        startTimeEpochSeconds = startTime ?: 0L,
    )
}

fun PlayerMatchEntity.toDomain(): PlayerMatch = PlayerMatch(
    matchId = matchId,
    heroId = heroId,
    heroName = DotaHeroCatalog.nameFor(heroId),
    kills = kills,
    deaths = deaths,
    assists = assists,
    durationSeconds = durationSeconds,
    isWin = isWin,
    startTimeEpochSeconds = startTimeEpochSeconds,
)

fun combinePlayerStats(
    entity: PlayerStatsEntity,
    matchEntities: List<PlayerMatchEntity>,
    formatRankTier: FormatRankTierUseCase,
    calculateMetrics: CalculatePlayerMetricsUseCase,
): PlayerStats {
    val matches = matchEntities.map { it.toDomain() }
    val metrics = calculateMetrics(
        wins = entity.wins,
        losses = entity.losses,
        recentMatches = matches,
    )
    return PlayerStats(
        accountId = entity.accountId,
        personaName = entity.personaName,
        avatarUrl = entity.avatarUrl,
        rankTier = entity.rankTier,
        rankLabel = formatRankTier(entity.rankTier, entity.leaderboardRank),
        leaderboardRank = entity.leaderboardRank,
        wins = entity.wins,
        losses = entity.losses,
        metrics = metrics,
        recentMatches = matches,
        lastUpdatedAt = entity.lastUpdatedAt,
    )
}
