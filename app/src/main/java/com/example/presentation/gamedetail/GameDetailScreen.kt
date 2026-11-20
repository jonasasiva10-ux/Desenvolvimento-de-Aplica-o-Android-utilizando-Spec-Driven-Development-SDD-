package com.example.presentation.gamedetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.PatchNote
import com.example.presentation.components.EmptyTelemetryState
import com.example.presentation.components.ErrorTelemetryState
import com.example.presentation.components.LoadingTelemetryState
import com.example.presentation.components.PatchDetailBottomSheet
import com.example.presentation.components.PatchNoteCard
import com.example.presentation.components.TelemetryFormatter
import com.example.ui.theme.PulseMint
import com.example.ui.theme.TelemetryCyan
import com.example.ui.theme.TrophyGold
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameDetailScreen(
    appId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = koinViewModel(parameters = { parametersOf(appId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedPatch by remember { mutableStateOf<PatchNote?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.detail?.name ?: stringResource(R.string.game_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("game_detail_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    uiState.detail?.let { detail ->
                        IconButton(
                            onClick = viewModel::onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("game_detail_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (detail.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(
                                    if (detail.isFavorite) R.string.cd_favorite_remove else R.string.cd_favorite_add,
                                ),
                                tint = if (detail.isFavorite) TrophyGold else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
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
                uiState.detail != null -> {
                    val detail = uiState.detail!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item(key = "hero") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(196.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                            ) {
                                AsyncImage(
                                    model = detail.headerImageUrl,
                                    contentDescription = detail.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color(0xE60B0E14)),
                                            ),
                                        ),
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                ) {
                                    Text(
                                        text = detail.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "${stringResource(R.string.game_detail_developer)}: ${detail.developer}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                    )
                                }
                            }
                        }

                        item(key = "kpi_row") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StatTile(
                                    title = stringResource(R.string.label_live_now),
                                    value = TelemetryFormatter.formatPlayerCount(detail.currentPlayers),
                                    accent = PulseMint,
                                    modifier = Modifier.weight(1f),
                                )
                                StatTile(
                                    title = stringResource(R.string.label_peak_24h),
                                    value = TelemetryFormatter.formatPlayerCount(detail.peakInGame),
                                    accent = TelemetryCyan,
                                    modifier = Modifier.weight(1f),
                                )
                                StatTile(
                                    title = if (detail.metacriticScore != null) {
                                        stringResource(R.string.game_detail_metacritic)
                                    } else {
                                        "Rank Steam"
                                    },
                                    value = detail.metacriticScore?.toString()
                                        ?: detail.rank?.let { "#$it" }
                                        ?: "--",
                                    accent = TrophyGold,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        item(key = "genres") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                detail.genres.forEach { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "about") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.game_detail_about),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = detail.shortDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        item(key = "patches_title") {
                            Text(
                                text = stringResource(R.string.game_detail_recent_patches),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        if (uiState.patches.isEmpty()) {
                            item(key = "empty_patches") {
                                EmptyTelemetryState(
                                    title = stringResource(R.string.patches_empty_title),
                                    subtitle = stringResource(R.string.patches_empty_subtitle),
                                    actionLabel = stringResource(R.string.action_refresh),
                                    onAction = viewModel::refresh,
                                )
                            }
                        } else {
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

                        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
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

@Composable
private fun StatTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = accent,
                maxLines = 1,
            )
        }
    }
}
