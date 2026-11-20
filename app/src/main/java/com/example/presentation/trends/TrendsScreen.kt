package com.example.presentation.trends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.GameFilter
import com.example.presentation.components.ConnectivityStatusStrip
import com.example.presentation.components.EmptyTelemetryState
import com.example.presentation.components.ErrorTelemetryState
import com.example.presentation.components.GameRankCard
import com.example.presentation.components.LoadingTelemetryState
import com.example.ui.theme.PulseMint
import org.koin.androidx.compose.koinViewModel

@Composable
fun TrendsScreen(
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrendsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TrendsContent(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onToggleFavorite = viewModel::onToggleFavorite,
        onRefresh = { viewModel.refresh(force = true) },
        onGameClick = onGameClick,
        modifier = modifier,
    )
}

@Composable
fun TrendsContent(
    uiState: TrendsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (GameFilter) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRefresh: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("trends_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero_banner") {
                HeroTelemetryBanner()
            }

            item(key = "connectivity_strip") {
                ConnectivityStatusStrip(
                    isOnline = uiState.isOnline,
                    lastSyncedAt = uiState.lastSyncedAt,
                    isSyncing = uiState.isSyncing,
                    onRefresh = onRefresh,
                )
            }

            item(key = "search_bar") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trends_search_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.trends_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
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

            item(key = "filter_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "filter_all") {
                        FilterChip(
                            selected = uiState.selectedFilter == GameFilter.ALL,
                            onClick = { onFilterSelected(GameFilter.ALL) },
                            label = { Text(stringResource(R.string.filter_all)) },
                            modifier = Modifier.testTag("filter_chip_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    item(key = "filter_rising") {
                        FilterChip(
                            selected = uiState.selectedFilter == GameFilter.RISING,
                            onClick = { onFilterSelected(GameFilter.RISING) },
                            label = { Text(stringResource(R.string.filter_rising)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_rising"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                    item(key = "filter_favorites") {
                        FilterChip(
                            selected = uiState.selectedFilter == GameFilter.FAVORITES,
                            onClick = { onFilterSelected(GameFilter.FAVORITES) },
                            label = { Text(stringResource(R.string.filter_favorites)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_favorites"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    item(key = "loading") {
                        LoadingTelemetryState()
                    }
                }

                uiState.error != null -> {
                    item(key = "error") {
                        ErrorTelemetryState(
                            error = uiState.error,
                            onRetry = onRefresh,
                        )
                    }
                }

                uiState.games.isEmpty() -> {
                    item(key = "empty") {
                        EmptyTelemetryState(
                            title = stringResource(R.string.trends_empty_title),
                            subtitle = stringResource(R.string.trends_empty_subtitle),
                            actionLabel = stringResource(R.string.action_refresh),
                            onAction = onRefresh,
                        )
                    }
                }

                else -> {
                    items(
                        items = uiState.games,
                        key = { it.appId },
                    ) { game ->
                        GameRankCard(
                            game = game,
                            onClick = { onGameClick(game.appId) },
                            onToggleFavorite = { onToggleFavorite(game.appId) },
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroTelemetryBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_gaming_pulse_1795196145560),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEA0B0E14),
                                Color(0x990B0E14),
                                Color(0x220B0E14),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.trends_hero_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseMint,
                    )
                }
                Text(
                    text = stringResource(R.string.trends_hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.trends_hero_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.fillMaxWidth(0.78f),
                )
            }
        }
    }
}
