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
import androidx.compose.foundation.gestures.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.lin0721.linmusic.ui.components.ProfileSidebar
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable

enum class Screen {
    Home, Playlist, Search, Library
}

enum class AppSidebarState {
    Closed, Open
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
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var isPlayerOpen by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var activePlaylistId by remember { mutableStateOf<Long?>(null) }
    var searchAutoFocus by remember { mutableStateOf(false) }

    val hazeState = remember { HazeState() }

    val density = LocalDensity.current
    val drawerWidth = 310.dp
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    val drawerState = remember {
        AnchoredDraggableState<AppSidebarState>(
            initialValue = AppSidebarState.Closed,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(drawerWidthPx) {
        drawerState.updateAnchors(
            DraggableAnchors {
                AppSidebarState.Closed at 0f
                AppSidebarState.Open at drawerWidthPx
            }
        )
    }

    val scope = rememberCoroutineScope()
    val openSidebar: () -> Unit = {
        scope.launch { drawerState.animateTo(AppSidebarState.Open) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = drawerState,
                orientation = Orientation.Horizontal,
                enabled = userProfile != null
            )
            .background(BackgroundDark)
    ) {
        // 1. 侧边栏层 (位于最底层或同步移动)
        userProfile?.let { profile ->
            Box(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val offset = drawerState.offset
                        val progress = if (offset.isNaN()) 0f else (offset / drawerWidthPx).coerceIn(0f, 1f)
                        
                        translationX = (if (offset.isNaN()) 0f else offset) - drawerWidthPx
                        alpha = 0.5f + (0.5f * progress)
                    }
            ) {
                ProfileSidebar(
                    userProfile = profile,
                    onLogout = {
                        viewModel.logout()
                        scope.launch { drawerState.animateTo(AppSidebarState.Closed) }
                    },
                    onDismiss = {
                        scope.launch { drawerState.animateTo(AppSidebarState.Closed) }
                    }
                )
            }
        }

        // 2. 主页面内容层 (支持平移动画，带圆角过渡和阴影)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = drawerState.offset
                    val progress = if (offset.isNaN()) 0f else (offset / drawerWidthPx).coerceIn(0f, 1f)
                    
                    translationX = if (offset.isNaN()) 0f else offset
                    clip = true
                    shape = RoundedCornerShape((progress * 32).dp)
                    shadowElevation = (progress * 30f)
                }
                .background(BackgroundDark)
        ) {
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
                                },
                                onOpenSidebar = openSidebar
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
                                onBack = { currentScreen = Screen.Home },
                                onOpenSidebar = openSidebar
                            )
                        }
                        Screen.Library -> {
                            com.lin0721.linmusic.ui.library.LibraryScreen(
                                onPlaylistClick = { id ->
                                    activePlaylistId = id
                                    currentScreen = Screen.Playlist
                                },
                                onBack = { currentScreen = Screen.Home },
                                onOpenSidebar = openSidebar
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

            // 侧边栏打开时的遮罩与点击收起事件
            val currentOffset = drawerState.offset
            if (!currentOffset.isNaN() && currentOffset > 0f) {
                val progress = currentOffset / drawerWidthPx
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * progress))
                        .clickable(
                            enabled = drawerState.currentValue == AppSidebarState.Open,
                            onClick = { scope.launch { drawerState.animateTo(AppSidebarState.Closed) } }
                        )
                )
            }
        }

        // 3. 全屏播放器层 (置于最顶层)
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
