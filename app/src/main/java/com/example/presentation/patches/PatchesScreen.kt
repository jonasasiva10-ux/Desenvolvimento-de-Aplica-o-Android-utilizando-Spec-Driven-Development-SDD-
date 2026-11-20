package com.example.presentation.patches

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.presentation.components.ConnectivityStatusStrip
import com.example.presentation.components.EmptyTelemetryState
import com.example.presentation.components.ErrorTelemetryState
import com.example.presentation.components.LoadingTelemetryState
import com.example.presentation.components.PatchDetailBottomSheet
import com.example.presentation.components.PatchNoteCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun PatchesScreen(
    modifier: Modifier = Modifier,
    viewModel: PatchesViewModel = koinViewModel(),
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
                .testTag("patches_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.patches_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.patches_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "connectivity") {
                ConnectivityStatusStrip(
                    isOnline = uiState.isOnline,
                    lastSyncedAt = uiState.lastSyncedAt,
                    isSyncing = uiState.isSyncing,
                    onRefresh = { viewModel.refresh(force = true) },
                )
            }

            item(key = "search") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patches_search_input"),
                    placeholder = { Text("Filtrar por título, patch ou jogo…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }

            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "only_patches") {
                        FilterChip(
                            selected = uiState.onlyOfficialPatches,
                            onClick = viewModel::onToggleOfficialOnly,
                            label = { Text(stringResource(R.string.patches_only_filter)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.testTag("filter_official_patches_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    item(key = "all_games") {
                        FilterChip(
                            selected = uiState.selectedAppId == null,
                            onClick = { viewModel.onGameSelected(null) },
                            label = { Text(stringResource(R.string.patches_all_games)) },
                        )
                    }
                    items(
                        items = uiState.availableGames,
                        key = { "chip_${it.appId}" },
                    ) { game ->
                        FilterChip(
                            selected = uiState.selectedAppId == game.appId,
                            onClick = {
                                viewModel.onGameSelected(
                                    if (uiState.selectedAppId == game.appId) null else game.appId,
                                )
                            },
                            label = { Text(game.name) },
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    item(key = "loading") { LoadingTelemetryState() }
                }

                uiState.error != null -> {
                    item(key = "error") {
                        ErrorTelemetryState(
                            error = uiState.error!!,
                            onRetry = { viewModel.refresh(force = true) },
                        )
                    }
                }

                uiState.patches.isEmpty() -> {
                    item(key = "empty") {
                        EmptyTelemetryState(
                            title = stringResource(R.string.patches_empty_title),
                            subtitle = stringResource(R.string.patches_empty_subtitle),
                            actionLabel = stringResource(R.string.action_refresh),
                            onAction = { viewModel.refresh(force = true) },
                        )
                    }
                }

                else -> {
                    items(
                        items = uiState.patches,
                        key = { it.gid },
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
