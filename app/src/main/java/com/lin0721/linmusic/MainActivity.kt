package com.lin0721.linmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.home.BottomFloatingIsland
import com.lin0721.linmusic.ui.home.HomeScreen
import com.lin0721.linmusic.ui.home.HomeViewModel
import com.lin0721.linmusic.ui.player.FullPlayerScreen
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.LinMusicTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import org.koin.androidx.compose.koinViewModel

enum class Screen {
    Home, Playlist, Search
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinMusicTheme {
                LinMusicApp()
            }
        }
    }
}

@Composable
fun LinMusicApp() {
    val viewModel: HomeViewModel = koinViewModel()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.playerManager.duration.collectAsStateWithLifecycle()

    var isPlayerOpen by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var activePlaylistId by remember { mutableStateOf<Long?>(null) }
    var searchAutoFocus by remember { mutableStateOf(false) }

    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Box(modifier = Modifier.fillMaxSize().haze(hazeState)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val forward = targetState != Screen.Home
                    val offsetY = 40
                    if (forward) {
                        (fadeIn(tween(300, delayMillis = 100, easing = FastOutSlowInEasing))
                                + slideInVertically(tween(300, delayMillis = 100, easing = FastOutSlowInEasing)) { offsetY })
                            .togetherWith(
                                fadeOut(tween(200, easing = FastOutSlowInEasing))
                                        + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -offsetY }
                            )
                    } else {
                        (fadeIn(tween(300, delayMillis = 100, easing = FastOutSlowInEasing))
                                + slideInVertically(tween(300, delayMillis = 100, easing = FastOutSlowInEasing)) { -offsetY })
                            .togetherWith(
                                fadeOut(tween(200, easing = FastOutSlowInEasing))
                                        + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { offsetY }
                            )
                    }.using(SizeTransform(clip = false))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onPlaylistClick = { id ->
                                activePlaylistId = id
                                currentScreen = Screen.Playlist
                            },
                            onSearchClick = {
                                searchAutoFocus = true
                                currentScreen = Screen.Search
                            }
                        )
                    }
                    Screen.Playlist -> {
                        activePlaylistId?.let { id ->
                            com.lin0721.linmusic.ui.playlist.PlaylistScreen(
                                playlistId = id,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                    Screen.Search -> {
                        com.lin0721.linmusic.ui.search.SearchScreen(
                            autoFocus = searchAutoFocus,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            BottomFloatingIsland(
                hazeState = hazeState,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                onTogglePlay = { viewModel.togglePlayPause() },
                onOpenPlayer = { isPlayerOpen = true },
                currentScreen = currentScreen,
                onNavigate = { searchAutoFocus = false; currentScreen = it }
            )
        }

        AnimatedVisibility(
            visible = isPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn(tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayerScreen(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTogglePlay = { viewModel.togglePlayPause() },
                onClose = { isPlayerOpen = false }
            )
        }
    }
}
