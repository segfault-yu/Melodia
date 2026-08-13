package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.auth.UserProfile

// 首页信息流骨架，按加载状态装配各区块
@Composable
fun HomeContent(
    uiState: HomeUiState,
    userProfile: UserProfile?,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onIntelligenceClick: () -> Unit,
    onRoamingClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 180.dp)
    ) {
        item {
            TopGreetingBar(
                userProfile = userProfile,
                onLoginClick = onAvatarClick,
                onSearchClick = onSearchClick
            )
        }

        item { FilterPills() }

        when (uiState) {
            is HomeUiState.Loading -> { item { LoadingIndicator() } }
            is HomeUiState.Error -> {
                item {
                    ErrorContent(message = uiState.message, onRetry = onRetry)
                }
            }
            is HomeUiState.Success -> {
                val data = uiState.data

                item {
                    ForYouSection(
                        dailySongs = data.dailySongs,
                        toplists = data.toplistItems,
                        recommendPlaylists = data.recommendPlaylists,
                        onDailyRecommendClick = { onPlaylistClick(-1L) },
                        onHotlistClick = { onPlaylistClick(it) },
                        onIntelligenceClick = onIntelligenceClick,
                        onRadarClick = { onPlaylistClick(it) },
                        onRoamingClick = onRoamingClick
                    )
                }

                // 推荐歌单
                item { SectionHeader(title = "推荐歌单", showAction = false) }
                item {
                    RecommendationCarousel(
                        playlists = data.recommendPlaylists,
                        onClick = { onPlaylistClick(it.id) }
                    )
                }

                // 最近播放
                if (data.recentPlaylists.isNotEmpty()) {
                    item {
                        RecentPlaySection(
                            items = data.recentPlaylists,
                            onClick = { item -> onPlaylistClick(item.data.id) }
                        )
                    }
                }

                // 排行榜
                if (data.toplistItems.isNotEmpty()) {
                    item { SectionHeader(title = "排行榜", showAction = false) }
                    item { ToplistCarousel(toplists = data.toplistItems) }
                }

                // 你最爱的艺人
                if (data.favoriteArtists.isNotEmpty()) {
                    item { FavoriteArtistsSection(artists = data.favoriteArtists) }
                }
            }
        }
    }
}
