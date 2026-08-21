package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.GradientStart
import com.lin0721.linmusic.feature.home.domain.HomeCard
import kotlinx.coroutines.flow.distinctUntilChanged

// 首页信息流骨架：单个 LazyColumn 承载顶栏与服务端下发的货架序列。
// 货架内部两列是手写 Row，不能换成懒加载网格，嵌进 LazyColumn 会因无界高度约束崩溃。
@Composable
fun HomeContent(
    uiState: HomeUiState,
    userProfile: UserProfile?,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPlaylistClick: (Long, Boolean) -> Unit,
    onSongClick: (HomeCard.Song) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onIntelligenceClick: () -> Unit,
    onRoamingClick: () -> Unit
) {
    val listState = rememberLazyListState()
    val feed = (uiState as? HomeUiState.Success)?.data

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 180.dp)
    ) {
        // 渐变铺在状态栏内边距之前，色彩才能顶到状态栏后面，顶栏不至于是一块死黑
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(GradientStart, BackgroundDark)))
                    .statusBarsPadding()
            ) {
                TopGreetingBar(
                    userProfile = userProfile,
                    onLoginClick = onAvatarClick,
                    onSearchClick = onSearchClick
                )
                FilterPills()
            }
        }

        when (uiState) {
            is HomeUiState.Loading -> item { LoadingIndicator() }

            is HomeUiState.Error -> item {
                ErrorContent(message = uiState.message, onRetry = onRetry)
            }

            is HomeUiState.Success -> {
                val data = uiState.data

                item {
                    RecentPlaySection(
                        items = data.recentPlaylists,
                        onClick = { item -> onPlaylistClick(item.data.id, false) }
                    )
                }

                item {
                    ForYouSection(
                        dailySongs = data.dailySongs,
                        toplists = data.toplistItems,
                        recommendPlaylists = data.recommendPlaylists,
                        onDailyRecommendClick = { onPlaylistClick(-1L, false) },
                        onHotlistClick = { onPlaylistClick(it, false) },
                        onIntelligenceClick = onIntelligenceClick,
                        onRadarClick = { onPlaylistClick(it, false) },
                        onRoamingClick = onRoamingClick
                    )
                }

                items(
                    items = data.shelves,
                    key = { it.blockCode }
                ) { shelf ->
                    HomeShelfSection(
                        shelf = shelf,
                        onCardClick = { card ->
                            when (card) {
                                is HomeCard.Playlist -> onPlaylistClick(card.id, false)
                                is HomeCard.Album -> onPlaylistClick(card.id, true)
                                is HomeCard.Song -> onSongClick(card)
                            }
                        }
                    )
                }

                if (data.isLoadingMore) {
                    item { HomeShelfLoadingMore() }
                }
            }
        }
    }

    // 翻页条件只能进 LaunchedEffect 的 key，不能塞进 snapshotFlow 的闭包——
    // uiState 是入参而非 State，闭包会一直捕获创建时那一份，翻页将永远触发不了。
    val canLoadMore = feed != null && feed.hasMore && !feed.isLoadingMore

    LaunchedEffect(listState, canLoadMore) {
        if (!canLoadMore) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            // 距底部两个区块时预取
            lastVisible >= info.totalItemsCount - 2
        }
            .distinctUntilChanged()
            .collect { if (it) onLoadMore() }
    }
}
