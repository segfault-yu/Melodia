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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onPlaylistClick: (Long, Boolean) -> Unit = { _, _ -> },
    onSearchClick: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    onLoginScreenVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

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
        HomeContent(
            uiState = uiState,
            userProfile = userProfile,
            onAvatarClick = onAvatarClick,
            onSearchClick = onSearchClick,
            onPlaylistClick = onPlaylistClick,
            onSongClick = { song -> viewModel.playShelfSong(song) },
            onRetry = { viewModel.loadHomeData() },
            onLoadMore = { viewModel.loadMoreShelves() },
            onIntelligenceClick = { viewModel.startIntelligenceMode() },
            onRoamingClick = { viewModel.startRoaming() }
        )

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
