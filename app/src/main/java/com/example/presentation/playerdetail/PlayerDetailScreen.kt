package com.example.presentation.playerdetail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.PlayerMatch
import com.example.presentation.components.ErrorTelemetryState
import com.example.presentation.components.LoadingTelemetryState
import com.example.presentation.components.TelemetryFormatter
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.PulseMint
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TrophyGold
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerDetailViewModel = koinViewModel(parameters = { parametersOf(accountId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.stats?.personaName ?: stringResource(R.string.player_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_detail_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                uiState.isLoading -> LoadingTelemetryState()
                uiState.error != null -> ErrorTelemetryState(error = uiState.error!!, onRetry = viewModel::refresh)
                uiState.stats != null -> {
                    val stats = uiState.stats!!
                    val winRateFraction = (stats.metrics.winRatePercent / 100.0).toFloat().coerceIn(0f, 1f)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item(key = "profile_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.45f)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (stats.avatarUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = stats.avatarUrl,
                                                contentDescription = stats.personaName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = TrophyGold,
                                                modifier = Modifier.size(32.dp),
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = stats.personaName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = TrophyGold.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.5f)),
                                        ) {
                                            Text(
                                                text = stats.rankLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TrophyGold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                        Text(
                                            text = "Account ID: ${stats.accountId} • ${TelemetryFormatter.formatSyncClock(stats.lastUpdatedAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "winrate_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.player_stat_winrate),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "${stats.metrics.winRatePercent}%",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                                            ),
                                            color = PulseMint,
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { winRateFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        color = PulseMint,
                                        trackColor = AlertCoral.copy(alpha = 0.45f),
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        PlayerKpiColumn(
                                            label = stringResource(R.string.player_stat_wins),
                                            value = stats.wins.toString(),
                                            accent = PulseMint,
                                        )
                                        PlayerKpiColumn(
                                            label = stringResource(R.string.player_stat_losses),
                                            value = stats.losses.toString(),
                                            accent = AlertCoral,
                                        )
                                        PlayerKpiColumn(
                                            label = stringResource(R.string.player_stat_kda),
                                            value = "${stats.metrics.avgKdaRatio}x",
                                            accent = TelemetryCyan,
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "recent_matches_title") {
                            Text(
                                text = stringResource(R.string.player_recent_matches),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        items(
                            items = stats.recentMatches,
                            key = { it.matchId },
                        ) { match ->
                            MatchHistoryCard(match = match)
                        }

                        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerKpiColumn(
    label: String,
    value: String,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = accent,
        )
    }
}

@Composable
private fun MatchHistoryCard(match: PlayerMatch) {
    val outcomeColor = if (match.isWin) PulseMint else AlertCoral
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = outcomeColor.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = stringResource(
                                if (match.isWin) R.string.player_match_win else R.string.player_match_loss,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = outcomeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = match.heroName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${TelemetryFormatter.formatDuration(match.durationSeconds)} • ${TelemetryFormatter.formatRelativeTimeFromEpochSeconds(match.startTimeEpochSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "K / D / A",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = match.kdaFormatted,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
