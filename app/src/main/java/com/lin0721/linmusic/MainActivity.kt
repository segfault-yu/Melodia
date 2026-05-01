package com.lin0721.linmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.home.HomeScreen
import com.lin0721.linmusic.ui.home.HomeViewModel
import com.lin0721.linmusic.ui.player.FullPlayerScreen
import com.lin0721.linmusic.ui.theme.LinMusicTheme
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.BackgroundDark
import org.koin.androidx.compose.koinViewModel

enum class Screen {
    Home, Playlist, Login
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinMusicApp() {
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
                        onOpenPlayer = { isPlayerOpen = true },
                        onLoginClick = { currentScreen = Screen.Login }
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
                Screen.Login -> {
                    // 登录占位页
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Login,
                            contentDescription = null,
                            tint = NeteaseRed,
                            modifier = Modifier
                        )
                        Text(
                            text = "登录页面",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "此功能即将上线",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { currentScreen = Screen.Home },
                            colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Text("返回首页")
                        }
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