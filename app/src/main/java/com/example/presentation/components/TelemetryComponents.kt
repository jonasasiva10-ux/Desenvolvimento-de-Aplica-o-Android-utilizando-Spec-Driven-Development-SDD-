package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.AppError
import com.example.domain.model.GameRank
import com.example.domain.model.PatchNote
import com.example.domain.model.TrendDirection
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.PulseMint
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TrophyGold

@Composable
fun ConnectivityStatusStrip(
    isOnline: Boolean,
    lastSyncedAt: Long?,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeColor by animateColorAsState(
        targetValue = if (isOnline) PulseMint else TrophyGold,
        label = "connectivityBadgeColor",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (isOnline) {
                            stringResource(R.string.status_live_online)
                        } else {
                            stringResource(R.string.status_offline_cache)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isSyncing) {
                            stringResource(R.string.status_syncing)
                        } else {
                            TelemetryFormatter.formatSyncClock(lastSyncedAt)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isSyncing,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("sync_refresh_button"),
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun RankTrendBadge(
    game: GameRank,
    modifier: Modifier = Modifier,
) {
    val (label, accent) = when (game.trendDirection) {
        TrendDirection.RISING -> "▲ +${game.rankDelta}" to PulseMint
        TrendDirection.FALLING -> "▼ ${game.rankDelta}" to AlertCoral
        TrendDirection.NEW -> "★ ${stringResource(R.string.label_rank_new)}" to TelemetryCyan
        TrendDirection.STEADY -> "● ${stringResource(R.string.label_rank_steady)}" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun GameRankCard(
    game: GameRank,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rankAccent = when (game.rank) {
        1 -> TrophyGold
        2 -> Color(0xFFD1D5DB)
        3 -> Color(0xFFE08D55)
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_card_${game.appId}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
            ) {
                AsyncImage(
                    model = game.headerImageUrl,
                    contentDescription = game.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                                ),
                            ),
                        ),
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xCC0B0E14),
                    border = BorderStroke(1.dp, rankAccent.copy(alpha = 0.7f)),
                ) {
                    Text(
                        text = "#${game.rank}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = rankAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("favorite_toggle_${game.appId}"),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xAA0B0E14),
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(
                                if (game.isFavorite) R.string.cd_favorite_remove else R.string.cd_favorite_add,
                            ),
                            tint = if (game.isFavorite) TrophyGold else Color.White,
                            modifier = Modifier.padding(6.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = game.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MetricBlock(
                        label = stringResource(R.string.label_peak_24h),
                        value = TelemetryFormatter.formatPlayerCount(game.peakInGame),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                    )
                    if (game.currentPlayers != null) {
                        MetricBlock(
                            label = stringResource(R.string.label_live_now),
                            value = TelemetryFormatter.formatPlayerCount(game.currentPlayers),
                            valueColor = PulseMint,
                        )
                    }
                }
                RankTrendBadge(game = game)
            }
        }
    }
}

@Composable
fun MetricBlock(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
            color = valueColor,
        )
    }
}

@Composable
fun PatchNoteCard(
    patch: PatchNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeAccent = if (patch.isOfficialPatch) PulseMint else TelemetryCyan

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("patch_card_${patch.gid}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = stringResource(
                                if (patch.isOfficialPatch) R.string.patch_badge_official else R.string.patch_badge_update,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeAccent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = patch.gameName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = TelemetryFormatter.formatRelativeTimeFromEpochSeconds(patch.publishedAtEpochSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = patch.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = patch.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${patch.feedLabel} • ${patch.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchDetailBottomSheet(
    patch: PatchNote,
    onDismiss: () -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = patch.gameName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = patch.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${patch.feedLabel} • ${patch.author} • ${TelemetryFormatter.formatRelativeTimeFromEpochSeconds(patch.publishedAtEpochSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = patch.fullContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize(),
                ) {
                    Text(stringResource(R.string.action_close))
                }
                if (patch.url.isNotBlank()) {
                    Button(
                        onClick = { onOpenOfficialUrl(patch.url) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("open_official_patch_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_open_official), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingTelemetryState(
    message: String = stringResource(R.string.status_syncing),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag("loading_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyTelemetryState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_offline_radar_1795196145298),
            contentDescription = stringResource(R.string.cd_offline_illustration),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 180.dp, height = 132.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("empty_state_action_button"),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorTelemetryState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_offline_radar_1795196145298),
            contentDescription = stringResource(R.string.cd_offline_illustration),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 180.dp, height = 132.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, AlertCoral.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        )
        Text(
            text = stringResource(TelemetryFormatter.errorTitleRes(error)),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(TelemetryFormatter.errorMessageRes(error)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("error_retry_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_retry))
        }
    }
}
