package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectSheet
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.model.PlaylistDetail

// TopBar 操作区高度（不含状态栏）
private val TOP_BAR_HEIGHT = 56.dp
// 封面动画范围
private val COVER_MAX_SIZE = 260.dp
private val COVER_MIN_SIZE = 36.dp

// ────────────────────────────────────────────────────────────────────────────
// 主内容
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
    var dominantColor by remember { mutableStateOf(Color(0xFF333333)) }

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
    val overlayBgAlpha    = progress
    // 封面透明度优化：前 40% 不透明，之后平滑淡出
    val coverAlpha        = 1f - ((progress - 0.4f) / 0.5f).coerceIn(0f, 1f)
    // 到达临界点后瞬间切换，不做渐隐
    val isCollapsed       = progress >= 0.8f

    var collectSongId by remember { mutableStateOf<Long?>(null) }
    var activeSongMoreOptions by remember { mutableStateOf<Track?>(null) }

    // 下拉阈值超过 40dp 触发弹出
    val snapThresholdPx = with(density) { 40.dp.toPx() }
    var pullAccumulator by remember { mutableFloatStateOf(0f) }
    // 标记是否通过主动拖拽打开，防止 fling 误入 item 0 后被误判为已打开
    var intentionallyOpened by remember { mutableStateOf(false) }

    // 搜索栏二态吸附
    val searchBarSnapConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isDailyRecommend) return Offset.Zero

                // 在 item 1 顶部边界
                if (available.y > 0 && listState.firstVisibleItemIndex == 1
                    && listState.firstVisibleItemScrollOffset == 0
                ) {
                    if (source == NestedScrollSource.UserInput) {
                        pullAccumulator += available.y
                        if (pullAccumulator >= snapThresholdPx) {
                            pullAccumulator = 0f
                            intentionallyOpened = true
                            // 阈值达到，放行让后续拖拽自然滚到 item 0
                            // gesture 持有 scroll mutex，此时不能调用 animateScrollToItem
                            return Offset.Zero
                        }
                    }
                    // 未达阈值的拖拽 或 fling：消耗全部，停在 item 1
                    return available
                }

                if (available.y < 0) {
                    pullAccumulator = 0f
                    // 向上滑动收起搜索栏时重置标记
                    if (listState.firstVisibleItemIndex >= 1) intentionallyOpened = false
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullAccumulator = 0f
                // 手指已抬起，gesture 结束，mutex 已释放，此处调用动画安全
                // 若已达阈值但 list 还没滚到 item 0，补充动画
                if (intentionallyOpened && listState.firstVisibleItemIndex != 0) {
                    listState.animateScrollToItem(0)
                    return available // 消耗 fling 速度，由动画接管
                }
                return Velocity.Zero
            }

            // 如果 fling 仍然越过了边界进入 item 0，强制 snap 回 item 1
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!isDailyRecommend && listState.firstVisibleItemIndex == 0 && !intentionallyOpened) {
                    listState.animateScrollToItem(1)
                }
                return Velocity.Zero
            }
        }
    }

    // 慢速拖拽后的吸附：手势结束时若搜索栏处于中间态，自动就近吸附
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && !isDailyRecommend
            && listState.firstVisibleItemIndex == 0
        ) {
            val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            val itemHeight = itemInfo?.size ?: return@LaunchedEffect
            if (listState.firstVisibleItemScrollOffset > itemHeight / 2) {
                intentionallyOpened = false
                listState.animateScrollToItem(1)
            } else {
                listState.animateScrollToItem(0)
            }
        }
    }

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
                    val allDates = remember(historyDates) {
                        listOf("今天") + historyDates
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        this@LazyRow.items(allDates, key = { it }) { date ->
                            val isSelected = date == selectedHistoryDate
                            val displayText = if (date == "今天") {
                                "今天"
                            } else {
                                val parts = date.split("-")
                                if (parts.size == 3) "${parts[1]}/${parts[2]}" else date
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                                    )
                                    .clickable {
                                        onSelectedHistoryDateChange(date)
                                        if (date == "今天") {
                                            onLoadDailyRecommend()
                                        } else {
                                            onLoadHistoryDetail(date)
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = MelodiaSpacing.sm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            if (playlist.id == -2L) {
                item(key = "user_record_filter") {
                    val allFilters = listOf("最近一周", "所有时间")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allFilters, key = { it }) { filter ->
                            val isSelected = filter == selectedHistoryDate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                                    )
                                    .clickable {
                                        onSelectedHistoryDateChange(filter)
                                        if (filter == "最近一周") {
                                            onLoadHistoryDetail("weekly")
                                        } else {
                                            onLoadHistoryDetail("all")
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = MelodiaSpacing.sm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
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
                // 歌曲（支持搜索过滤）
                val filtered = if (searchQuery.isBlank()) playlist.tracks
                               else playlist.tracks.filter {
                                   it.name.contains(searchQuery, true) ||
                                   it.ar.any { a -> a.name.contains(searchQuery, true) }
                               }
                items(filtered, key = { it.id }) { track ->
                    SongRow(
                        data = SongRowData(
                            id = track.id,
                            title = track.name,
                            artist = track.ar.joinToString(" • ") { it.name },
                            coverUrl = track.al.picUrl,
                            isVip = track.fee == 1
                        ),
                        isActive = currentTrackId == track.id.toString(),
                        onClick = { onPlaySong(track) },
                        onArtistClick = { track.ar.firstOrNull()?.id?.let(onArtistClick) },
                        trailingSlot = {
                            IconButton(
                                onClick = { activeSongMoreOptions = track },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(MelodiaSpacing.sm))
                        }
                    )
                }
            }

            // 推荐歌曲板块
            if (recommendedSongs.isNotEmpty()) {
                item(key = "recommendation_header") {
                    RecommendationHeader(
                        onRefresh = onRefreshRecommendations
                    )
                }
                items(recommendedSongs, key = { "rec_${it.id}" }) { track ->
                    SongRow(
                        data = SongRowData(
                            id = track.id,
                            title = track.name,
                            artist = track.ar.joinToString(" • ") { it.name },
                            coverUrl = track.al.picUrl,
                            isVip = track.fee == 1
                        ),
                        isActive = currentTrackId == track.id.toString(),
                        onClick = { onPlaySong(track) },
                        onArtistClick = { track.ar.firstOrNull()?.id?.let(onArtistClick) },
                        trailingSlot = {
                            IconButton(
                                onClick = { onAddRecommendSong(track) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加歌曲到歌单",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                        .padding(MelodiaSpacing.xxs)
                                )
                            }
                        }
                    )
                }
            }
        }

        // ── 2. 固定 Overlay ───────────────────────────────────────────────
        // 进入时背景 alpha=0（完全透明，视觉上不存在）
        // 滚动后 alpha 随 progress 增大直到完全不透明
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(overlayHeight)
                .background(dominantColor.copy(alpha = overlayBgAlpha))
                .padding(top = statusBarHeight) // 内容区域被挤到状态栏下方
                .zIndex(8f)
        ) {
            // 返回键
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = MelodiaSpacing.xs)
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
            }
            // 歌单名称：随着滚动淡入及向上微移
            val titleAlpha = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
            val titleOffsetY = lerp(8.dp, 0.dp, titleAlpha)
            Text(
                text       = playlist.name,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize   = 17.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 60.dp)
                    .offset(y = titleOffsetY)
                    .alpha(titleAlpha)
            )
        }

        // 折叠后吸附播放按钮（每日推荐歌单隐藏）
        val fabScale = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
        if (!isDailyRecommend && fabScale > 0f) {
            FloatingActionButton(
                onClick        = onPlayAll,
                containerColor = MaterialTheme.colorScheme.primary,
                shape          = CircleShape,
                modifier       = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = MelodiaSpacing.md)
                    .offset(y = overlayHeight - 28.dp)
                    .size(56.dp)
                    .zIndex(10f)
                    .graphicsLayer(
                        scaleX = fabScale,
                        scaleY = fabScale,
                        alpha = fabScale
                    )
                    .shadow(8.dp * fabScale, CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
            }
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
