package com.codex.stageset.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codex.stageset.data.repository.PreviewSettingsRepository
import com.codex.stageset.data.repository.SetlistRepository
import com.codex.stageset.data.repository.SongRepository
import com.codex.stageset.ui.setlists.SetlistEditorRoute
import com.codex.stageset.ui.setlists.SetlistsRoute
import com.codex.stageset.ui.songs.SongEditorRoute
import com.codex.stageset.ui.songs.SongPreviewRoute
import com.codex.stageset.ui.songs.SongsRoute

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private object Routes {
    const val Songs = "songs"
    const val SongPreview = "song-preview"
    const val SongEditor = "song-editor"
    const val Setlists = "setlists"
    const val SetlistEditor = "setlist-editor"

    fun songPreview(songId: Long) = "$SongPreview/$songId"
    fun songEditor(songId: Long = -1L) = "$SongEditor/$songId"
    fun setlistEditor(setlistId: Long = -1L) = "$SetlistEditor/$setlistId"
}

@Composable
fun StageSetApp(
    previewSettingsRepository: PreviewSettingsRepository,
    songRepository: SongRepository,
    setlistRepository: SetlistRepository,
) {
    val navController = rememberNavController()
    val topLevelDestinations = listOf(
        TopLevelDestination(
            route = Routes.Songs,
            label = "Songs",
            icon = Icons.Outlined.LibraryMusic,
        ),
        TopLevelDestination(
            route = Routes.Setlists,
            label = "Setlists",
            icon = Icons.AutoMirrored.Outlined.QueueMusic,
        ),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val useNavigationRail = maxWidth >= 900.dp
        val content: @Composable (PaddingValues) -> Unit = { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.Songs,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                composable(Routes.Songs) {
                    SongsRoute(
                        songRepository = songRepository,
                        onCreateSong = { navController.navigate(Routes.songEditor()) },
                        onOpenSong = { songId -> navController.navigate(Routes.songPreview(songId)) },
                    )
                }
                composable(
                    route = "${Routes.SongPreview}/{songId}",
                    arguments = listOf(
                        navArgument("songId") {
                            type = NavType.LongType
                        },
                    ),
                ) { backStack ->
                    SongPreviewRoute(
                        songId = backStack.arguments?.getLong("songId") ?: -1L,
                        previewSettingsRepository = previewSettingsRepository,
                        songRepository = songRepository,
                        onBack = { navController.popBackStack() },
                        onEditSong = { songId -> navController.navigate(Routes.songEditor(songId)) },
                    )
                }
                composable(
                    route = "${Routes.SongEditor}/{songId}",
                    arguments = listOf(
                        navArgument("songId") {
                            type = NavType.LongType
                        },
                    ),
                ) { backStack ->
                    SongEditorRoute(
                        songId = backStack.arguments?.getLong("songId") ?: -1L,
                        songRepository = songRepository,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.Setlists) {
                    SetlistsRoute(
                        setlistRepository = setlistRepository,
                        onCreateSetlist = { navController.navigate(Routes.setlistEditor()) },
                        onOpenSetlist = { setlistId -> navController.navigate(Routes.setlistEditor(setlistId)) },
                    )
                }
                composable(
                    route = "${Routes.SetlistEditor}/{setlistId}",
                    arguments = listOf(
                        navArgument("setlistId") {
                            type = NavType.LongType
                        },
                    ),
                ) { backStack ->
                    SetlistEditorRoute(
                        setlistId = backStack.arguments?.getLong("setlistId") ?: -1L,
                        songRepository = songRepository,
                        setlistRepository = setlistRepository,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        if (useNavigationRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                        .width(96.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ) {
                    topLevelDestinations.forEach { destination ->
                        val selected = destination.matches(currentDestination?.route)
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                ) {
                    content(PaddingValues(0.dp))
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
                bottomBar = {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 10.dp,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            windowInsets = NavigationBarDefaults.windowInsets,
                        ) {
                            topLevelDestinations.forEach { destination ->
                                val selected = destination.matches(currentDestination?.route)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                                    label = { Text(destination.label) },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    content(innerPadding)
                }
            }
        }
    }
}

private fun TopLevelDestination.matches(route: String?): Boolean = when (this.route) {
    Routes.Songs -> {
        route == Routes.Songs ||
            route?.startsWith("${Routes.SongPreview}/") == true ||
            route?.startsWith("${Routes.SongEditor}/") == true
    }
    Routes.Setlists -> route == Routes.Setlists || route?.startsWith("${Routes.SetlistEditor}/") == true
    else -> false
}
