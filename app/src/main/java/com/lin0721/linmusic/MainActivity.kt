package com.lin0721.linmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.lin0721.linmusic.ui.components.MiniPlayerCard
import com.lin0721.linmusic.ui.components.MelodiaNavigationBar
import com.lin0721.linmusic.ui.home.HomeScreen
import com.lin0721.linmusic.ui.home.HomeViewModel
import com.lin0721.linmusic.ui.player.FullPlayerScreen
import com.lin0721.linmusic.ui.theme.BackgroundDark
// 导入 Melodia 全局主题配置
import com.lin0721.linmusic.ui.theme.MelodiaTheme
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import com.lin0721.linmusic.ui.components.ProfileSidebar
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import com.lin0721.linmusic.ui.create.CreatePopupMenu
import androidx.compose.ui.layout.onSizeChanged
// offset 同时移动视觉位置和布局边界(hit-test)，避免 graphicsLayer 只移动渲染层导致的手势拦截
import androidx.compose.ui.unit.IntOffset

enum class Screen {
    Home, Playlist, Search, Library, Settings, Artist
}

enum class AppSidebarState {
    Closed, Open
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelodiaTheme {
                MelodiaApp()
            }
        }
    }
}

@Composable
fun MelodiaApp() {
    val viewModel: HomeViewModel = koinViewModel()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.playerManager.duration.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var isPlayerOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 初始化为极大值，屏幕尺寸测量前 FullPlayerScreen 始终在屏幕外不可见
    var playerOffsetY by remember { mutableStateOf(10000f) }
    var screenHeightPx by remember { mutableStateOf(0f) }
    // springJob 不用 mutableStateOf，避免写入时触发不必要的 recomposition
    val springJobRef = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }

    // 统一控制播放界面展开和收起的弹簧动画
    fun animatePlayerTo(open: Boolean, velocity: Float, initialOffset: Float = Float.NaN) {
        springJobRef[0]?.cancel()
        if (open) {
            isPlayerOpen = true
            springJobRef[0] = scope.launch {
                animate(
                    initialValue = playerOffsetY,
                    targetValue = 0f,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ -> playerOffsetY = value.coerceIn(0f, screenHeightPx) }
            }
        } else {
            if (!initialOffset.isNaN()) {
                playerOffsetY = initialOffset
            }
            springJobRef[0] = scope.launch {
                animate(
                    initialValue = playerOffsetY,
                    targetValue = screenHeightPx,
                    initialVelocity = velocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { value, _ -> playerOffsetY = value.coerceIn(0f, screenHeightPx) }
                playerOffsetY = screenHeightPx
                isPlayerOpen = false
            }
        }
    }

    // 屏幕高度首次测量完成后将播放器初始化到屏幕底部
    LaunchedEffect(screenHeightPx) {
        if (screenHeightPx > 0f && !isPlayerOpen) {
            playerOffsetY = screenHeightPx
        }
    }


    // 全局自定义 Toast 状态管理
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        com.lin0721.linmusic.ui.components.ToastManager.toastFlow.collect { msg ->
            toastMessage = msg
            toastTrigger++
        }
    }

    LaunchedEffect(toastTrigger) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
        }
    }

    // 导航历史栈与当前屏幕状态
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen by remember { derivedStateOf { backStack.lastOrNull() ?: Screen.Home } }

    // 页面跳转函数
    val navigateTo: (Screen) -> Unit = { screen ->
        if (backStack.lastOrNull() != screen) {
            if (screen == Screen.Home) {
                backStack.clear()
                backStack.add(Screen.Home)
            } else if (screen == Screen.Search || screen == Screen.Library) {
                backStack.clear()
                backStack.add(Screen.Home)
                backStack.add(screen)
            } else {
                backStack.add(screen)
            }
        }
    }

    // 页面回退函数
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    var activePlaylistId by remember { mutableStateOf<Long?>(null) }
    var activePlaylistIsAlbum by remember { mutableStateOf(false) }
    var activeArtistId by remember { mutableStateOf<Long?>(null) }
    var searchAutoFocus by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    // 网页登录界面可见性状态
    var isLoginScreenVisible by remember { mutableStateOf(false) }

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

    // 侧边栏边缘滑动判断：避免抽屉打开手势与列表左右滑动冲突
    var isTouchStartingAtEdge by remember { mutableStateOf(false) }

    LaunchedEffect(drawerWidthPx) {
        drawerState.updateAnchors(
            DraggableAnchors {
                AppSidebarState.Closed at 0f
                AppSidebarState.Open at drawerWidthPx
            }
        )
    }

    val openSidebar: () -> Unit = {
        scope.launch { drawerState.animateTo(AppSidebarState.Open) }
    }

    // 系统返回键与侧滑返回拦截逻辑：按优先级关闭或返回上一级
    val isAnyOverlayOpen = isPlayerOpen ||
            (drawerState.currentValue == AppSidebarState.Open) ||
            showCreateSheet ||
            (backStack.size > 1)

    BackHandler(enabled = isAnyOverlayOpen) {
        when {
            isPlayerOpen -> {
                animatePlayerTo(false, 0f)
            }
            drawerState.currentValue == AppSidebarState.Open -> {
                scope.launch { drawerState.animateTo(AppSidebarState.Closed) }
            }
            showCreateSheet -> {
                showCreateSheet = false
            }
            backStack.size > 1 -> {
                navigateBack()
            }
        }
    }

    val isDrawerDraggable = userProfile != null && (drawerState.currentValue == AppSidebarState.Open || isTouchStartingAtEdge)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenHeightPx = it.height.toFloat() }
            .pointerInput(drawerState.currentValue) {
                val edgeWidthPx = with(density) { 32.dp.toPx() }
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.firstOrNull { it.changedToDown() }
                        if (down != null) {
                            // 侧边栏已打开时允许在任意位置向左滑动关闭；侧边栏关闭时，仅允许在左边缘向右拉出
                            isTouchStartingAtEdge = drawerState.currentValue == AppSidebarState.Open || down.position.x < edgeWidthPx
                        }
                        val allUp = event.changes.all { !it.pressed }
                        if (allUp) {
                            isTouchStartingAtEdge = false
                        }
                    }
                }
            }
            .anchoredDraggable(
                state = drawerState,
                orientation = Orientation.Horizontal,
                enabled = isDrawerDraggable
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
                    },
                    onNavigateToSettings = {
                        navigateTo(Screen.Settings)
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
                                    activePlaylistIsAlbum = false
                                    navigateTo(Screen.Playlist)
                                },
                                onSearchClick = {
                                    searchAutoFocus = true
                                    navigateTo(Screen.Search)
                                },
                                onOpenSidebar = openSidebar,
                                onLoginScreenVisibilityChanged = { isLoginScreenVisible = it }
                            )
                        }
                        Screen.Playlist -> {
                            activePlaylistId?.let { id ->
                                com.lin0721.linmusic.ui.playlist.PlaylistScreen(
                                    playlistId = id,
                                    isAlbum = activePlaylistIsAlbum,
                                    onBack = navigateBack,
                                    onArtistClick = { artistId ->
                                        activeArtistId = artistId
                                        navigateTo(Screen.Artist)
                                    }
                                )
                            }
                        }
                        Screen.Search -> {
                            com.lin0721.linmusic.ui.search.SearchScreen(
                                autoFocus = searchAutoFocus,
                                onBack = navigateBack,
                                onOpenSidebar = openSidebar
                            )
                        }
                        Screen.Library -> {
                            com.lin0721.linmusic.ui.library.LibraryScreen(
                                onPlaylistClick = { id ->
                                    activePlaylistId = id
                                    activePlaylistIsAlbum = false
                                    navigateTo(Screen.Playlist)
                                },
                                onArtistClick = { id ->
                                    activeArtistId = id
                                    navigateTo(Screen.Artist)
                                },
                                onBack = navigateBack,
                                onOpenSidebar = openSidebar,
                                onLoginScreenVisibilityChanged = { isLoginScreenVisible = it }
                            )
                        }
                        Screen.Settings -> {
                            com.lin0721.linmusic.ui.settings.SettingsScreen(
                                onBack = navigateBack
                            )
                        }
                        Screen.Artist -> {
                            activeArtistId?.let { id ->
                                com.lin0721.linmusic.ui.artist.ArtistScreen(
                                    artistId = id,
                                    onBack = navigateBack,
                                    onArtistClick = { nextId ->
                                        activeArtistId = nextId
                                        navigateTo(Screen.Artist)
                                    },
                                    onPlaylistClick = { playlistId ->
                                        activePlaylistId = playlistId
                                        activePlaylistIsAlbum = false
                                        navigateTo(Screen.Playlist)
                                    },
                                    onAlbumClick = { albumId ->
                                        activePlaylistId = albumId
                                        activePlaylistIsAlbum = true
                                        navigateTo(Screen.Playlist)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 创建菜单遮罩
            if (showCreateSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showCreateSheet = false }
                )
            }

            // 放置在应用了平移 graphicsLayer 的主 Box 内部的底部
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // 0. 创建菜单弹出层 (当需要显示时展示，置于播放器卡片上方)
                AnimatedVisibility(
                    visible = showCreateSheet && !isLoginScreenVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(200)),
                    exit = slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        CreatePopupMenu(
                            onDismiss = { showCreateSheet = false },
                            onLoginRequest = {
                                // TODO: 触发登录流程
                            }
                        )
                    }
                }

                // 1. 浮动播放卡片 (只有在有曲目且非登录状态下显示)
                AnimatedVisibility(
                    visible = currentTrack != null && !isLoginScreenVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayerCard(
                        hazeState = hazeState,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onNext = { viewModel.playerManager.playNext() },
                        onClick = { animatePlayerTo(true, 0f) },
                        onDrag = { delta ->
                            springJobRef[0]?.cancel()
                            if (!isPlayerOpen) isPlayerOpen = true
                            playerOffsetY = (playerOffsetY + delta).coerceIn(0f, screenHeightPx)
                        },
                        onDragEnd = { velocity ->
                            val shouldOpen = playerOffsetY < screenHeightPx * 0.80f || velocity < -1000f
                            animatePlayerTo(shouldOpen, velocity)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // 2. M3 导航栏 (在非登录状态下显示)
                AnimatedVisibility(
                    visible = !isLoginScreenVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    MelodiaNavigationBar(
                        currentScreen = currentScreen,
                        onNavigate = { searchAutoFocus = false; navigateTo(it) },
                        onCreateClick = { showCreateSheet = !showCreateSheet },
                        isCreateMenuOpen = showCreateSheet
                    )
                }
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

        if (currentTrack != null && screenHeightPx > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, playerOffsetY.toInt()) }
                    .clip(
                        RoundedCornerShape(
                            topStart = if (playerOffsetY > 0f) 24.dp else 0.dp,
                            topEnd = if (playerOffsetY > 0f) 24.dp else 0.dp
                        )
                    )
            ) {
                FullPlayerScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSeek = { viewModel.playerManager.seekTo(it) },
                    onClose = {
                        animatePlayerTo(false, 0f)
                    },
                    onDragClose = { offset, velocity ->
                        animatePlayerTo(false, velocity, offset)
                    },
                    isPlayerOpen = isPlayerOpen,
                    onArtistClick = { artistId ->
                        animatePlayerTo(false, 0f)
                        activeArtistId = artistId
                        navigateTo(Screen.Artist)
                    }
                )
            }
        }

        // 4. 全局自定义 Toast 提示
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp) // 位于底部浮岛上方
                    .zIndex(999f)
            ) {
                toastMessage?.let { msg ->
                    com.lin0721.linmusic.ui.components.CustomToast(message = msg)
                }
            }
        }

    }
}
