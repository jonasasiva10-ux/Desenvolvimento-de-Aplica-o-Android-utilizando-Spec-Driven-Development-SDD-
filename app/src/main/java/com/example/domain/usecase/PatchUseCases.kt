package com.example.domain.usecase

import com.example.domain.model.PatchNote
import com.example.domain.model.SyncOutcome
import com.example.domain.repository.PatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetPatchNotesUseCase(
    private val patchRepository: PatchRepository,
) {
    operator fun invoke(
        selectedAppId: Int? = null,
        onlyOfficialPatches: Boolean = false,
        query: String = "",
    ): Flow<List<PatchNote>> {
        val normalizedQuery = query.trim().lowercase()
        val source = if (selectedAppId != null) {
            patchRepository.observePatchesForGame(selectedAppId)
        } else {
            patchRepository.observeAllPatches()
        }
        return source.map { patches ->
            patches
                .asSequence()
                .filter { patch -> !onlyOfficialPatches || patch.isOfficialPatch }
                .filter { patch ->
                    if (normalizedQuery.isEmpty()) {
                        true
                    } else {
                        patch.title.lowercase().contains(normalizedQuery) ||
                            patch.gameName.lowercase().contains(normalizedQuery) ||
                            patch.summary.lowercase().contains(normalizedQuery)
                    }
                }
                .sortedByDescending { it.publishedAtEpochSeconds }
                .toList()
        }
    }
}

class RefreshPatchNotesUseCase(
    private val patchRepository: PatchRepository,
) {
    suspend fun refreshAll(force: Boolean = false): SyncOutcome {
        return patchRepository.refreshPatchesForTopGames(force)
    }

    suspend fun refreshForGame(appId: Int, gameName: String): SyncOutcome {
        return patchRepository.refreshPatchesForGame(appId, gameName)
    }
}

class GetFavoritePatchesUseCase(
    private val patchRepository: PatchRepository,
) {
    operator fun invoke(): Flow<List<PatchNote>> = patchRepository.observePatchesForFavorites()
}
