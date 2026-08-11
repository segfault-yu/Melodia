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
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.Dialog
import com.lin0721.linmusic.core.ui.components.CreatePlaylistDialog
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.MelodiaDragHandle
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.feature.comment.ui.CommentsBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.lin0721.linmusic.core.ui.theme.extractDominantColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.feature.playlist.data.PlaylistDetail
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

// TopBar 操作区高度（不含状态栏）
private val TOP_BAR_HEIGHT = 56.dp
// 封面动画范围
private val COVER_MAX_SIZE = 260.dp
private val COVER_MIN_SIZE = 36.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    isAlbum: Boolean = false,
    viewModel: PlaylistViewModel = koinViewModel(),
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val likedSongIds by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val collectState by viewModel.collectState.collectAsStateWithLifecycle()
    val userProfile  by viewModel.userProfile.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val historyRecommendState by viewModel.historyRecommendState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showMoreMenuSheet by remember { mutableStateOf(false) }
    var selectedHistoryDate by remember { mutableStateOf("今天") }

    LaunchedEffect(historyRecommendState.selectedDate) {
        selectedHistoryDate = historyRecommendState.selectedDate ?: "今天"
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { com.lin0721.linmusic.core.ui.components.ToastManager.showToast(it) }
    }
    LaunchedEffect(playlistId, isAlbum) {
        viewModel.loadPlaylist(playlistId, isAlbum)
        if (playlistId == -1L) {
            viewModel.loadHistoryDates()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is PlaylistUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            is PlaylistUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(MelodiaSpacing.sm))
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(MelodiaSpacing.md))
                        Button(onClick = { viewModel.loadPlaylist(playlistId, isAlbum) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text("重试", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onBack) { Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            is PlaylistUiState.Success ->
                PlaylistContent(
                    playlist       = state.playlist,
                    currentTrackId = currentTrack?.mediaId,
                    likedSongIds   = likedSongIds,
                    collectState   = collectState,
                    isLoggedIn     = userProfile != null,
                    recommendedSongs = state.recommendedSongs,
                    onBack         = onBack,
                    onArtistClick  = onArtistClick,
                    onAlbumClick   = onAlbumClick,
                    onToggleLike   = viewModel::toggleLikeSong,
                    onPlaySong     = { track ->
                        viewModel.playSongInList(track, state.playlist.tracks)
                    },
                    onAddToPlayNext = { track ->
                        viewModel.addTrackToPlayNext(track)
                    },
                    onPlayAll = {
                        state.playlist.tracks.firstOrNull()?.let { first ->
                            viewModel.playSongInList(first, state.playlist.tracks)
                        }
                    },
                    onShufflePlay = {
                        val tracks = state.playlist.tracks
                        if (tracks.isNotEmpty()) {
                            val shuffled = tracks.shuffled()
                            viewModel.playSongInList(shuffled.first(), shuffled)
                        }
                    },
                    onLikeClick = { songId ->
                        viewModel.prepareCollectDialog(songId)
                    },
                    onSaveCollection = { songId, items ->
                        viewModel.savePlaylistCollection(songId, items)
                    },
                    onSaveNewCollection = { name, songId ->
                        viewModel.createPlaylistAndAddSong(name, songId)
                    },
                    onRequireLogin = {
                        showLoginSheet = true
                    },
                    onRefreshRecommendations = {
                        viewModel.refreshRecommendations()
                    },
                    onAddRecommendSong = { track ->
                        viewModel.addRecommendSongToPlaylist(state.playlist.id, track)
                    },
                    isSubscribed = state.isSubscribed,
                    onSubscribeClick = {
                        if (userProfile == null) {
                            showLoginSheet = true
                        } else {
                            viewModel.toggleSubscribePlaylist()
                        }
                    },
                    onCommentsClick = {
                        showCommentsSheet = true
                        viewModel.loadPlaylistComments(playlistId)
                    },
                    onMoreClick = {
                        showMoreMenuSheet = true
                    },
                    onHistoryClick = {},
                    historyDates = historyRecommendState.dates,
                    historySongsLoading = historyRecommendState.songsLoading,
                    showHistoryDatePicker = true,
                    selectedHistoryDate = selectedHistoryDate,
                    onSelectedHistoryDateChange = { selectedHistoryDate = it },
                    onLoadHistoryDetail = { viewModel.loadHistoryDetail(it) },
                    onLoadDailyRecommend = { viewModel.loadPlaylist(-1L) }
                )

        }

        if (showLoginSheet) {
            LoginBottomSheet(
                onDismiss = { showLoginSheet = false },
                onWebLogin = {
                    showLoginSheet = false
                    showWebViewLogin = true
                }
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

        if (showCommentsSheet) {
            CommentsBottomSheet(
                commentsState = commentsState,
                onLikeComment = viewModel::likeComment,
                onDismiss = { showCommentsSheet = false },
                onRetry = { viewModel.loadPlaylistComments(playlistId) }
            )
        }



        val successState = uiState as? PlaylistUiState.Success
        if (showMoreMenuSheet && successState != null) {
            val playlist = successState.playlist
            ModalBottomSheet(
                onDismissRequest = { showMoreMenuSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                dragHandle = { MelodiaDragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = MelodiaSpacing.lg, vertical = MelodiaSpacing.md)
                ) {
                    Text(
                        text = "歌单操作",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = MelodiaSpacing.md)
                    )
                    
                    val firstArtist = playlist.tracks.firstOrNull()?.ar?.firstOrNull()
                    val artistName = firstArtist?.name ?: "未知歌手"
                    
                    val menuItems = listOf(
                        PlaylistMenuItem(
                            icon = if (successState.isSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            title = if (successState.isSubscribed) "取消收藏歌单" else "收藏歌单",
                            subtitle = "收藏歌单到我的音乐库"
                        ) {
                            showMoreMenuSheet = false
                            if (userProfile == null) {
                                showLoginSheet = true
                            } else {
                                viewModel.toggleSubscribePlaylist()
                            }
                        },
                        PlaylistMenuItem(
                            icon = Icons.Default.Person,
                            title = "跳转至艺人",
                            subtitle = "查看歌手: $artistName"
                        ) {
                            showMoreMenuSheet = false
                            if (firstArtist != null) {
                                onArtistClick(firstArtist.id)
                            } else {
                                com.lin0721.linmusic.core.ui.components.ToastManager.showToast("未找到关联艺人信息")
                            }
                        },
                        PlaylistMenuItem(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            title = "加入播放队列",
                            subtitle = "添加 ${playlist.tracks.size} 首歌曲至播放队列"
                        ) {
                            showMoreMenuSheet = false
                            viewModel.addTracksToPlayNext(playlist.tracks)
                        },
                        PlaylistMenuItem(
                            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                            title = "添加到歌单",
                            subtitle = "将全部歌曲导入到其他歌单"
                        ) {
                            showMoreMenuSheet = false
                            com.lin0721.linmusic.core.ui.components.ToastManager.showToast("批量导入功能开发中，敬请期待")
                        }
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(menuItems, key = { it.title }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = item.onClick)
                                    .padding(vertical = MelodiaSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(MelodiaSpacing.md))
                                Column {
                                    Text(
                                        text = item.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.subtitle != null) {
                                        Text(
                                            text = item.subtitle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

// ────────────────────────────────────────────────────────────────────────────
// 主内容
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistContent(
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
        val songId = collectSongId!!
        ModalBottomSheet(
            onDismissRequest = { collectSongId = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            dragHandle = { MelodiaDragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MelodiaSpacing.lg)
                    .navigationBarsPadding()
                    .padding(bottom = MelodiaSpacing.lg)
            ) {
                Text(
                    text = "收藏到歌单",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = MelodiaSpacing.md)
                )

                if (collectState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val localItems = remember(collectState.collectItems) {
                        collectState.collectItems.map { it.copy() }.toMutableStateList()
                    }
                    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm)
                    ) {
                        item(key = "create_new_playlist_item") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showCreatePlaylistDialog = true
                                    }
                                    .padding(vertical = MelodiaSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "新建歌单",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "新建歌单",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (localItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = MelodiaSpacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无其他可用歌单", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        } else {
                            itemsIndexed(localItems, key = { _, item -> item.playlistId }) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            localItems[index] = item.copy(isContains = !item.isContains)
                                        }
                                        .padding(vertical = MelodiaSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = "${item.coverUrl}?param=100y100",
                                            contentDescription = item.playlistName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item.playlistName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Checkbox(
                                        checked = item.isContains,
                                        onCheckedChange = { checked ->
                                            localItems[index] = item.copy(isContains = checked)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MelodiaSpacing.lg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { collectSongId = null }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(MelodiaSpacing.sm))
                        Button(
                            onClick = {
                                onSaveCollection(songId, localItems)
                                collectSongId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("确定", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showCreatePlaylistDialog) {
                        CreatePlaylistDialog(
                            onDismiss = { showCreatePlaylistDialog = false },
                            onCreate = { name -> onSaveNewCollection(name, songId) }
                        )
                    }
                }
            }
        }
    }

    if (activeSongMoreOptions != null) {
        val track = activeSongMoreOptions!!
        ModalBottomSheet(
            onDismissRequest = { activeSongMoreOptions = null },
            containerColor = MaterialTheme.colorScheme.background,
            shape = BottomSheetShape,
            dragHandle = { MelodiaDragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = MelodiaSpacing.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "${track.al.picUrl}?param=150y150",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(MelodiaSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(MelodiaSpacing.xs))
                        Text(
                            text = track.ar.joinToString(" • ") { it.name },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = MelodiaSpacing.sm)
                )

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OptionRow(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        text = "下一首播放",
                        onClick = {
                            activeSongMoreOptions = null
                            onAddToPlayNext(track)
                        }
                    )

                    val isLiked = track.id in likedSongIds
                    val likeIcon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                    val likeIconTint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    val likeText = if (isLiked) "取消喜欢" else "喜欢"

                    OptionRow(
                        icon = likeIcon,
                        text = likeText,
                        iconTint = likeIconTint,
                        onClick = {
                            activeSongMoreOptions = null
                            if (!isLoggedIn) {
                                onRequireLogin()
                            } else {
                                onToggleLike(track.id, !isLiked)
                            }
                        }
                    )

                    OptionRow(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        text = "收藏到歌单",
                        onClick = {
                            activeSongMoreOptions = null
                            if (!isLoggedIn) {
                                onRequireLogin()
                            } else {
                                collectSongId = track.id
                                onLikeClick(track.id)
                            }
                        }
                    )

                    val artistsText = track.ar.joinToString(" • ") { it.name }
                    OptionRow(
                        icon = Icons.Default.Person,
                        text = "歌手: $artistsText",
                        onClick = {
                            activeSongMoreOptions = null
                            track.ar.firstOrNull()?.id?.let { artistId ->
                                onArtistClick(artistId)
                            }
                        }
                    )

                    OptionRow(
                        icon = Icons.Default.Album,
                        text = "专辑: ${track.al.name}",
                        onClick = {
                            activeSongMoreOptions = null
                            if (track.al.id > 0) {
                                onAlbumClick(track.al.id)
                            } else {
                                com.lin0721.linmusic.core.ui.components.ToastManager.showToast("暂无专辑信息")
                            }
                        }
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 搜索栏（LazyColumn Item 0）
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun SearchBarItem(query: String, onQueryChange: (String) -> Unit, topPadding: Dp, backgroundColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
        // 占据 overlay 的高度，防止下拉后搜索栏被返回键等遮挡
        Spacer(modifier = Modifier.height(topPadding))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MelodiaSpacing.md, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MelodiaSpacing.sm))
                androidx.compose.foundation.text.BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("在此页面上查找", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("排序", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 全出血 Hero（LazyColumn Item 1）
// 封面区包含 overlayHeight 的内边距，使封面在透明 overlay 下方自然显示
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun PlaylistHeaderItem(
    playlist: PlaylistDetail,
    coverSize: Dp,
    coverAlpha: Float,
    isCollapsed: Boolean,
    statusBarHeight: Dp,
    dominantColor: Color,
    onColorCalculated: (Color) -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onMoreClick: () -> Unit,
    onHistoryClick: () -> Unit = {},
    selectedHistoryDate: String = "今天"
) {
    val todayStr = remember {
        java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.getDefault()).format(java.util.Date())
    }
    val displayDateStr = remember(selectedHistoryDate) {
        if (selectedHistoryDate == "今天") {
            todayStr
        } else {
            val parts = selectedHistoryDate.split("-")
            if (parts.size == 3) {
                "${parts[0]}年${parts[1]}月${parts[2]}日"
            } else {
                selectedHistoryDate
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 使用从封面提取的主色调渐变到背景黑
            .background(Brush.verticalGradient(listOf(dominantColor, MaterialTheme.colorScheme.background)))
    ) {
        // 封面：与操作区的返回键水平对齐
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight + MelodiaSpacing.md, bottom = MelodiaSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val coverRequest = remember(playlist.coverImgUrl) {
                ImageRequest.Builder(context)
                    .data("${playlist.coverImgUrl}?param=400y400")
                    .allowHardware(false)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model              = coverRequest,
                contentDescription = playlist.name,
                contentScale       = ContentScale.Crop,
                onSuccess          = { state -> 
                    onColorCalculated(extractDominantColor(state.result.drawable))
                },
                modifier           = Modifier
                    .size(coverSize)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(10.dp), clip = false)
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (coverAlpha < 1f) Modifier.alpha(coverAlpha) else Modifier)
            )
        }

        // 歌单信息与操作行：折叠后隐藏
        if (!isCollapsed) {
            Column(modifier = Modifier.padding(horizontal = MelodiaSpacing.md)) {
                Text(playlist.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                    fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (playlist.id == -1L) {
                    Text(displayDateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else if (playlist.id == -2L) {
                    Text("网易云个人听歌记录统计", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (playlist.creator != null) {
                            AsyncImage(
                                model = "${playlist.creator.avatarUrl}?param=50y50",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(playlist.creator.nickname, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("为你打造", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                    if (!playlist.description.isNullOrBlank()) {
                        Spacer(Modifier.height(MelodiaSpacing.xs))
                        Text(playlist.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(MelodiaSpacing.xs))
                val playCountText = if (playlist.playCount > 0) " • 播放 ${formatPlayCount(playlist.playCount)} 次" else ""
                Text("${playlist.tracks.size} 首歌曲$playCountText", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (playlist.id != -1L && playlist.id != -2L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MelodiaSpacing.md, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onSubscribeClick) {
                            Icon(
                                imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = if (isSubscribed) "已收藏" else "收藏",
                                tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onCommentsClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Comment,
                                contentDescription = "评论",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onShufflePlay,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, "Shuffle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        FloatingActionButton(
                            onClick        = onPlayAll,
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape          = CircleShape,
                            modifier       = Modifier
                                .size(56.dp)
                                .shadow(6.dp, CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 推荐板块头部
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecommendationHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "推荐歌曲",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRefresh)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新推荐",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(MelodiaSpacing.xs))
            Text(
                text = "刷新",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


private data class PlaylistMenuItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit
)

@Composable
private fun OptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(MelodiaSpacing.md))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatPlayCount(count: Long): String {
    return when {
        count >= 100000000 -> "${(count / 10000000) / 10.0}亿"
        count >= 10000 -> "${(count / 1000) / 10.0}万"
        else -> count.toString()
    }
}

