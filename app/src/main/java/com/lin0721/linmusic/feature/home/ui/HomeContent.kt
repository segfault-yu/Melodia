package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.feature.home.domain.HomeCard

// 货架与轮播自带左右边距，顶部既有区块各自已有内边距，故外层容器不设横向 contentPadding
private val EdgePadding = 20.dp

// 首页信息流骨架：单个 LazyColumn 承载顶部固定区块与服务端下发的货架序列。
// 货架内部两列是手写 Row，不能换成懒加载网格——嵌进 LazyColumn 会因无界高度约束崩溃。
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

    LazyColumn(
        state = listState,
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
            is HomeUiState.Loading -> item { LoadingIndicator() }

            is HomeUiState.Error -> item {
                ErrorContent(message = uiState.message, onRetry = onRetry)
            }

            is HomeUiState.Success -> {
                val data = uiState.data

                item {
                    HomeBannerSection(
                        banners = data.banners,
                        onPlaylistClick = { onPlaylistClick(it, false) },
                        modifier = Modifier.padding(horizontal = EdgePadding)
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

                if (data.recentPlaylists.isNotEmpty()) {
                    item {
                        RecentPlaySection(
                            items = data.recentPlaylists,
                            onClick = { item -> onPlaylistClick(item.data.id, false) }
                        )
                    }
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
                        },
                        modifier = Modifier.padding(horizontal = EdgePadding)
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
    val feed = (uiState as? HomeUiState.Success)?.data
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
