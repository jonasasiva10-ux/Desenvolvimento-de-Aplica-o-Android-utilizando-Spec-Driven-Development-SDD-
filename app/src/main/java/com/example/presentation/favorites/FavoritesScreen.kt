package com.example.presentation.favorites

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.PatchNote
import com.example.presentation.components.EmptyTelemetryState
import com.example.presentation.components.GameRankCard
import com.example.presentation.components.PatchDetailBottomSheet
import com.example.presentation.components.PatchNoteCard
import com.example.presentation.components.TelemetryFormatter
import com.example.ui.theme.PulseMint
import com.example.ui.theme.TrophyGold
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onGameClick: (Int) -> Unit,
    onExploreTrends: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedPatch by remember { mutableStateOf<PatchNote?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("favorites_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.saved_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.saved_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = (if (uiState.isOnline) "ONLINE" else "OFFLINE") +
                            " • ${TelemetryFormatter.formatSyncClock(uiState.lastSyncedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isOnline) PulseMint else TrophyGold,
                    )
                }
            }

            if (uiState.favoriteGames.isEmpty()) {
                item(key = "empty_saved") {
                    EmptyTelemetryState(
                        title = stringResource(R.string.saved_empty_title),
                        subtitle = stringResource(R.string.saved_empty_subtitle),
                        actionLabel = stringResource(R.string.nav_trends),
                        onAction = onExploreTrends,
                    )
                }
            } else {
                items(
                    items = uiState.favoriteGames,
                    key = { "fav_game_${it.appId}" },
                ) { game ->
                    GameRankCard(
                        game = game,
                        onClick = { onGameClick(game.appId) },
                        onToggleFavorite = { viewModel.onToggleFavorite(game.appId) },
                    )
                }

                if (uiState.favoritePatches.isNotEmpty()) {
                    item(key = "fav_patches_header") {
                        Text(
                            text = "Patches em Cache dos Favoritos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(
                        items = uiState.favoritePatches,
                        key = { "fav_patch_${it.gid}" },
                    ) { patch ->
                        PatchNoteCard(
                            patch = patch,
                            onClick = { selectedPatch = patch },
                        )
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    selectedPatch?.let { patch ->
        PatchDetailBottomSheet(
            patch = patch,
            onDismiss = { selectedPatch = null },
            onOpenOfficialUrl = { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
        )
    }
}
