package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.feature.music.ui.MusicContent
import com.lin0721.linmusic.feature.music.ui.MusicViewModel
import org.koin.androidx.compose.koinViewModel

private const val TAB_ALL = 0
private const val TAB_MUSIC = 1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    musicViewModel: MusicViewModel = koinViewModel(),
    onPlaylistClick: (Long, Boolean) -> Unit = { _, _ -> },
    onArtistClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    onLoginScreenVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val musicUiState by musicViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_ALL) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    // 监听网页登录界面可见性变化，并通知上层以隐藏悬浮底栏
    LaunchedEffect(showWebViewLogin) {
        onLoginScreenVisibilityChanged(showWebViewLogin)
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            ToastManager.showToast(message)
        }
    }

    LaunchedEffect(musicViewModel) {
        musicViewModel.toastEvent.collect { message ->
            ToastManager.showToast(message)
        }
    }

    // 曲风数据只在真正切到「音乐」时才拉
    LaunchedEffect(selectedTab) {
        if (selectedTab == TAB_MUSIC) musicViewModel.loadIfNeeded()
    }

    val onAvatarClick: () -> Unit = {
        if (userProfile != null) {
            onOpenSidebar()
        } else {
            showLoginSheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (selectedTab) {
            TAB_MUSIC -> MusicContent(
                uiState = musicUiState,
                userProfile = userProfile,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAvatarClick = onAvatarClick,
                onSearchClick = onSearchClick,
                onStyleSelect = { musicViewModel.selectStyle(it) },
                onChildStyleSelect = { musicViewModel.selectChildStyle(it) },
                onPlaylistClick = { onPlaylistClick(it.id, false) },
                onArtistClick = { onArtistClick(it.id) },
                onPlaySongAt = { musicViewModel.playSongAt(it) },
                onPlayFavourite = {
                    (musicUiState as? com.lin0721.linmusic.feature.music.ui.MusicUiState.Success)
                        ?.data?.content?.head?.favouriteSong
                        ?.let { musicViewModel.playFavouriteSong(it) }
                },
                onRetry = { musicViewModel.loadStyles() }
            )

            else -> HomeContent(
                uiState = uiState,
                userProfile = userProfile,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAvatarClick = onAvatarClick,
                onSearchClick = onSearchClick,
                onPlaylistClick = onPlaylistClick,
                onSongClick = { song -> viewModel.playShelfSong(song) },
                onRetry = { viewModel.loadHomeData() },
                onLoadMore = { viewModel.loadMoreShelves() },
                onIntelligenceClick = { viewModel.startIntelligenceMode() },
                onRoamingClick = { viewModel.startRoaming() }
            )
        }

        // 登录相关的弹窗保持在最顶层
        if (showLoginSheet) {
            LoginBottomSheet(
                onDismiss = { showLoginSheet = false },
                onWebLogin = {
                    showLoginSheet = false
                    showWebViewLogin = true
                },
            )
        }

        if (showWebViewLogin) {
            WebViewLoginScreen(
                onClose = { showWebViewLogin = false },
                onLoginSuccess = { cookies ->
                    showWebViewLogin = false
                    viewModel.handleLoginSuccess(cookies)
                }
            )
        }
    }
}
