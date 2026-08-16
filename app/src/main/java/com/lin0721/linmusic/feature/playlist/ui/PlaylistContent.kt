package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectSheet
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.ui.theme.FallbackDominant

// TopBar 操作区高度（不含状态栏）
private val TOP_BAR_HEIGHT = 56.dp
// 封面动画范围
private val COVER_MAX_SIZE = 260.dp
private val COVER_MIN_SIZE = 36.dp

// ────────────────────────────────────────────────────────────────────────────
// 主内容：折叠状态计算 + 各区块装配
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistContent(
    playlist: PlaylistDetail,
    currentTrackId: String?,
    likedSongIds: Set<Long>,
    collectState: PlaylistCollectState,
    isLoggedIn: Boolean,
    recommendedSongs: List<Track>,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onToggleLike: (Long, Boolean) -> Unit,
    onPlaySong: (Track) -> Unit,
    onAddToPlayNext: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveCollection: (Long, List<PlaylistCollectItem>) -> Unit,
    onSaveNewCollection: (String, Long) -> Unit,
    onRequireLogin: () -> Unit,
    onRefreshRecommendations: () -> Unit,
    onAddRecommendSong: (Track) -> Unit,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onMoreClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    historyDates: List<String> = emptyList(),
    historySongsLoading: Boolean = false,
    showHistoryDatePicker: Boolean = false,
    selectedHistoryDate: String = "今天",
    onSelectedHistoryDateChange: (String) -> Unit = {},
    onLoadHistoryDetail: (String) -> Unit = {},
    onLoadDailyRecommend: () -> Unit = {}
) {
    val density = LocalDensity.current

    val isDailyRecommend = playlist.id == -1L || playlist.id == -2L
    // 初始显示 index=1（封面），搜索栏 index=0 藏于上方，下拉可见（每日推荐无搜索栏，初始显示 index=0）
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (isDailyRecommend) 0 else 1)
    var searchQuery by remember { mutableStateOf("") }

    // 从封面提取的主色调，默认为深灰色
    var dominantColor by remember { mutableStateOf(FallbackDominant) }

    // 获取系统状态栏高度（因为开启了沉浸式，内容会画在状态栏下面）
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Overlay 总高度：状态栏 + 操作区(56dp)
    val overlayHeight = TOP_BAR_HEIGHT + statusBarHeight

    // 折叠进度 0f→1f（从封面完整显示到完全折叠）
    val collapseThresholdPx = with(density) { 300.dp.toPx() }
    val progress by remember {
        derivedStateOf {
            if (isDailyRecommend) {
                when {
                    listState.firstVisibleItemIndex == 0 ->
                        (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
                    else -> 1f
                }
            } else {
                when {
                    listState.firstVisibleItemIndex == 0 -> 0f
                    listState.firstVisibleItemIndex == 1 ->
                        (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
                    else -> 1f
                }
            }
        }
    }

    val coverSize: Dp     = lerp(COVER_MAX_SIZE, COVER_MIN_SIZE, progress)
    // 封面透明度优化：前 40% 不透明，之后平滑淡出
    val coverAlpha        = 1f - ((progress - 0.4f) / 0.5f).coerceIn(0f, 1f)
    // 到达临界点后瞬间切换，不做渐隐
    val isCollapsed       = progress >= 0.8f

    var collectSongId by remember { mutableStateOf<Long?>(null) }
    var activeSongMoreOptions by remember { mutableStateOf<Track?>(null) }

    val searchBarSnapConnection = rememberSearchBarSnapConnection(
        listState        = listState,
        isDailyRecommend = isDailyRecommend
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. 滚动内容 ───────────────────────────────────────────────────
        LazyColumn(
            state          = listState,
            contentPadding = PaddingValues(bottom = 180.dp),
            modifier       = Modifier.nestedScroll(searchBarSnapConnection)
        ) {
            // Item 0：搜索栏（下拉可见）
            if (!isDailyRecommend) {
                item(key = "search") {
                    SearchBarItem(
                        query           = searchQuery,
                        onQueryChange   = { searchQuery = it },
                        topPadding      = overlayHeight,
                        backgroundColor = dominantColor
                    )
                }
            }


            // Item 1：全出血 Hero
            item(key = "header") {
                PlaylistHeaderItem(
                    playlist            = playlist,
                    coverSize           = coverSize,
                    coverAlpha          = coverAlpha,
                    isCollapsed         = isCollapsed,
                    statusBarHeight     = statusBarHeight,
                    dominantColor       = dominantColor,
                    onColorCalculated   = { dominantColor = it },
                    onPlayAll           = onPlayAll,
                    onShufflePlay       = onShufflePlay,
                    isSubscribed        = isSubscribed,
                    onSubscribeClick    = onSubscribeClick,
                    onCommentsClick     = onCommentsClick,
                    onMoreClick         = onMoreClick,
                    onHistoryClick      = onHistoryClick,
                    selectedHistoryDate = selectedHistoryDate
                )
            }

            if (playlist.id == -1L && showHistoryDatePicker) {
                item(key = "history_date_picker") {
                    PlaylistHistoryDateRow(
                        historyDates                = historyDates,
                        selectedHistoryDate         = selectedHistoryDate,
                        onSelectedHistoryDateChange = onSelectedHistoryDateChange,
                        onLoadHistoryDetail         = onLoadHistoryDetail,
                        onLoadDailyRecommend        = onLoadDailyRecommend
                    )
                }
            }

            if (playlist.id == -2L) {
                item(key = "user_record_filter") {
                    PlaylistRecordFilterRow(
                        selectedHistoryDate         = selectedHistoryDate,
                        onSelectedHistoryDateChange = onSelectedHistoryDateChange,
                        onLoadHistoryDetail         = onLoadHistoryDetail
                    )
                }
            }

            if (historySongsLoading) {
                item(key = "history_songs_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                playlistTrackItems(
                    tracks         = playlist.tracks,
                    searchQuery    = searchQuery,
                    currentTrackId = currentTrackId,
                    onPlaySong     = onPlaySong,
                    onArtistClick  = onArtistClick,
                    onMoreClick    = { activeSongMoreOptions = it }
                )
            }

            // 推荐歌曲板块
            if (recommendedSongs.isNotEmpty()) {
                playlistRecommendItems(
                    recommendedSongs         = recommendedSongs,
                    currentTrackId           = currentTrackId,
                    onRefreshRecommendations = onRefreshRecommendations,
                    onPlaySong               = onPlaySong,
                    onArtistClick            = onArtistClick,
                    onAddRecommendSong       = onAddRecommendSong
                )
            }
        }

        // ── 2. 固定 Overlay ───────────────────────────────────────────────
        PlaylistTopBar(
            title           = playlist.name,
            progress        = progress,
            overlayHeight   = overlayHeight,
            statusBarHeight = statusBarHeight,
            dominantColor   = dominantColor,
            onBack          = onBack
        )

        // 折叠后吸附播放按钮（每日推荐歌单隐藏）
        if (!isDailyRecommend) {
            PlaylistCollapsedPlayFab(
                progress      = progress,
                overlayHeight = overlayHeight,
                onPlayAll     = onPlayAll
            )
        }
    }

    if (collectSongId != null) {
        PlaylistCollectSheet(
            songId = collectSongId!!,
            collectState = collectState,
            onDismiss = { collectSongId = null },
            onSaveCollection = onSaveCollection,
            onSaveNewCollection = onSaveNewCollection
        )
    }

    if (activeSongMoreOptions != null) {
        val track = activeSongMoreOptions!!
        PlaylistSongOptionsSheet(
            track = track,
            isLiked = track.id in likedSongIds,
            isLoggedIn = isLoggedIn,
            onDismiss = { activeSongMoreOptions = null },
            onAddToPlayNext = onAddToPlayNext,
            onToggleLike = onToggleLike,
            onCollectClick = { songId ->
                collectSongId = songId
                onLikeClick(songId)
            },
            onArtistClick = onArtistClick,
            onAlbumClick = onAlbumClick,
            onRequireLogin = onRequireLogin
        )
    }
}
