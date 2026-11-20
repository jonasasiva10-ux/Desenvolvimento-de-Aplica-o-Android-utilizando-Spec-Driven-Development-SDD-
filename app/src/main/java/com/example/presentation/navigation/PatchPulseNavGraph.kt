package com.example.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.presentation.favorites.FavoritesScreen
import com.example.presentation.gamedetail.GameDetailScreen
import com.example.presentation.patches.PatchesScreen
import com.example.presentation.playerdetail.PlayerDetailScreen
import com.example.presentation.players.PlayersScreen
import com.example.presentation.trends.TrendsScreen

object PatchPulseRoutes {
    const val ROUTE_TRENDS = "trends"
    const val ROUTE_PATCHES = "patches"
    const val ROUTE_PLAYERS = "players"
    const val ROUTE_SAVED = "saved"
    const val ROUTE_GAME_DETAIL = "game_detail/{appId}"
    const val ROUTE_PLAYER_DETAIL = "player_detail/{accountId}"

    fun gameDetail(appId: Int): String = "game_detail/$appId"
    fun playerDetail(accountId: Long): String = "player_detail/$accountId"
}

private enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    TRENDS(
        route = PatchPulseRoutes.ROUTE_TRENDS,
        labelRes = R.string.nav_trends,
        selectedIcon = Icons.Filled.Whatshot,
        unselectedIcon = Icons.Outlined.Whatshot,
        testTag = "nav_item_trends",
    ),
    PATCHES(
        route = PatchPulseRoutes.ROUTE_PATCHES,
        labelRes = R.string.nav_patches,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        testTag = "nav_item_patches",
    ),
    PLAYERS(
        route = PatchPulseRoutes.ROUTE_PLAYERS,
        labelRes = R.string.nav_players,
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        testTag = "nav_item_players",
    ),
    SAVED(
        route = PatchPulseRoutes.ROUTE_SAVED,
        labelRes = R.string.nav_saved,
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.StarBorder,
        testTag = "nav_item_saved",
    ),
}

@Composable
fun PatchPulseApp(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
    val showTopLevelNav = currentDestination?.route in topLevelRoutes

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                    ) {
                        TopLevelDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(dest.route) },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        TopLevelDestination.entries.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(dest.route) },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = PatchPulseRoutes.ROUTE_TRENDS,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(PatchPulseRoutes.ROUTE_TRENDS) {
                        TrendsScreen(
                            onGameClick = { appId ->
                                navController.navigate(PatchPulseRoutes.gameDetail(appId))
                            },
                        )
                    }
                    composable(PatchPulseRoutes.ROUTE_PATCHES) {
                        PatchesScreen()
                    }
                    composable(PatchPulseRoutes.ROUTE_PLAYERS) {
                        PlayersScreen(
                            onPlayerClick = { accountId ->
                                navController.navigate(PatchPulseRoutes.playerDetail(accountId))
                            },
                        )
                    }
                    composable(PatchPulseRoutes.ROUTE_SAVED) {
                        FavoritesScreen(
                            onGameClick = { appId ->
                                navController.navigate(PatchPulseRoutes.gameDetail(appId))
                            },
                            onExploreTrends = {
                                navigateToTopLevel(PatchPulseRoutes.ROUTE_TRENDS)
                            },
                        )
                    }
                    composable(
                        route = PatchPulseRoutes.ROUTE_GAME_DETAIL,
                        arguments = listOf(navArgument("appId") { type = NavType.IntType }),
                    ) { backStackEntry ->
                        val appId = backStackEntry.arguments?.getInt("appId") ?: 730
                        GameDetailScreen(
                            appId = appId,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = PatchPulseRoutes.ROUTE_PLAYER_DETAIL,
                        arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
                    ) { backStackEntry ->
                        val accountId = backStackEntry.arguments?.getLong("accountId") ?: 321580662L
                        PlayerDetailScreen(
                            accountId = accountId,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
