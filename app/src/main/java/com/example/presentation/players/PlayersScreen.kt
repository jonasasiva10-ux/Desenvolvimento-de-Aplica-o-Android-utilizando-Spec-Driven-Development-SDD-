package com.example.presentation.players

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.ProPlayer
import com.example.presentation.components.ConnectivityStatusStrip
import com.example.presentation.components.EmptyTelemetryState
import com.example.presentation.components.ErrorTelemetryState
import com.example.presentation.components.LoadingTelemetryState
import com.example.ui.theme.PulseMint
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TrophyGold
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayersScreen(
    onPlayerClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayersViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("players_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.players_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.players_subtitle),
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

            item(key = "search_row") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("players_search_input"),
                        placeholder = { Text(stringResource(R.string.players_search_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.PersonSearch,
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

                    uiState.parsedDirectAccountId?.let { accountId ->
                        Button(
                            onClick = { onPlayerClick(accountId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("lookup_account_id_button"),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${stringResource(R.string.players_lookup_button)} #$accountId")
                        }
                    }
                }
            }

            item(key = "quick_picks") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.players_quick_picks),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(
                            items = uiState.quickPicks,
                            key = { "quick_${it.accountId}" },
                        ) { shortcut ->
                            Surface(
                                modifier = Modifier
                                    .testTag("quick_player_${shortcut.accountId}")
                                    .clickable { onPlayerClick(shortcut.accountId) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.4f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = shortcut.label,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = shortcut.team,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TrophyGold,
                                    )
                                    Text(
                                        text = "ID ${shortcut.accountId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "section_pro") {
                Text(
                    text = stringResource(R.string.players_pro_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
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

                uiState.proPlayers.isEmpty() -> {
                    item(key = "empty") {
                        EmptyTelemetryState(
                            title = stringResource(R.string.players_empty_title),
                            subtitle = stringResource(R.string.players_empty_subtitle),
                            actionLabel = stringResource(R.string.action_refresh),
                            onAction = { viewModel.refresh(force = true) },
                        )
                    }
                }

                else -> {
                    items(
                        items = uiState.proPlayers,
                        key = { it.accountId },
                    ) { player ->
                        ProPlayerCard(
                            player = player,
                            onClick = { onPlayerClick(player.accountId) },
                        )
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProPlayerCard(
    player: ProPlayer,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pro_player_card_${player.accountId}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (player.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = player.proName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = player.proName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = PulseMint,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = player.proName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TelemetryCyan.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = player.teamTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = TelemetryCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Text(
                    text = "${player.teamName} • ${player.roleLabel}" +
                        if (player.countryCode.isNotBlank()) " • ${player.countryCode}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "Steam32 ID: ${player.accountId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseMint.copy(alpha = 0.85f),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
