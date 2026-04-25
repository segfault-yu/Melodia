package com.lin0721.linmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.home.HomeScreen
import com.lin0721.linmusic.ui.home.HomeViewModel
import com.lin0721.linmusic.ui.player.FullPlayerScreen
import com.lin0721.linmusic.ui.theme.LinMusicTheme
import org.koin.androidx.compose.koinViewModel

enum class Screen {
    Home, Playlist
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinMusicTheme {
                val viewModel: HomeViewModel = koinViewModel()
                val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
                val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
                
                var isPlayerOpen by remember { mutableStateOf(false) }
                
                var currentScreen by remember { mutableStateOf(Screen.Home) }
                var activePlaylistId by remember { mutableStateOf<Long?>(null) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                        when (screen) {
                            Screen.Home -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onPlaylistClick = { id ->
                                        activePlaylistId = id
                                        currentScreen = Screen.Playlist
                                    },
                                    onOpenPlayer = { isPlayerOpen = true }
                                )
                            }
                            Screen.Playlist -> {
                                activePlaylistId?.let { id ->
                                    com.lin0721.linmusic.ui.playlist.PlaylistScreen(
                                        playlistId = id,
                                        onBack = { currentScreen = Screen.Home },
                                        onOpenPlayer = { isPlayerOpen = true }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isPlayerOpen,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)),
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
        }
    }
}