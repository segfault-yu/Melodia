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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.feature.music.ui.MusicContent
import com.lin0721.linmusic.feature.music.ui.MusicViewModel
import com.lin0721.linmusic.feature.podcast.ui.PodcastContent
import com.lin0721.linmusic.feature.podcast.ui.PodcastViewModel
import org.koin.androidx.compose.koinViewModel

private const val TAB_ALL = 0
private const val TAB_MUSIC = 1
private const val TAB_PODCAST = 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    musicViewModel: MusicViewModel = koinViewModel(),
    podcastViewModel: PodcastViewModel = koinViewModel(),
    selectedTab: Int = TAB_ALL,
    onTabSelected: (Int) -> Unit = {},
    onPlaylistClick: (Long, Boolean) -> Unit = { _, _ -> },
    onArtistClick: (Long) -> Unit = {},
    onRadioClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    onLoginScreenVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val musicUiState by musicViewModel.uiState.collectAsStateWithLifecycle()
    val podcastUiState by podcastViewModel.uiState.collectAsStateWithLifecycle()

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

    // 各 tab 的数据都等真正切过去才拉，避免拖慢「全部」的首屏
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            TAB_MUSIC -> musicViewModel.loadIfNeeded()
            TAB_PODCAST -> podcastViewModel.loadIfNeeded()
        }
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
                onTabSelected = onTabSelected,
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

            TAB_PODCAST -> PodcastContent(
                uiState = podcastUiState,
                userProfile = userProfile,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onAvatarClick = onAvatarClick,
                onSearchClick = onSearchClick,
                onCategorySelect = { podcastViewModel.selectCategory(it) },
                onProgramClick = { podcastViewModel.playProgramAt(it) },
                onRadioClick = { onRadioClick(it.id) },
                onRetry = { podcastViewModel.loadFeed() }
            )

            else -> HomeContent(
                uiState = uiState,
                userProfile = userProfile,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onAvatarClick = onAvatarClick,
                onSearchClick = onSearchClick,
                onPlaylistClick = onPlaylistClick,
                onSongClick = { song -> viewModel.playShelfSong(song) },
                onVoiceClick = { voice -> viewModel.playShelfVoice(voice) },
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
