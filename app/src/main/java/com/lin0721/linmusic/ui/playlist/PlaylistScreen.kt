package com.lin0721.linmusic.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.Dialog
import com.lin0721.linmusic.ui.components.LoginBottomSheet
import com.lin0721.linmusic.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.ui.playlist.PlaylistCollectState
import com.lin0721.linmusic.ui.playlist.PlaylistCollectItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.lin0721.linmusic.ui.theme.extractDominantColor
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
import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.SurfaceLight
import com.lin0721.linmusic.ui.theme.TextGray
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor

// TopBar 操作区高度（不含状态栏）
private val TOP_BAR_HEIGHT = 56.dp
// 封面动画范围
private val COVER_MAX_SIZE = 260.dp
private val COVER_MIN_SIZE = 36.dp

@Composable
fun PlaylistScreen(
    playlistId: Long,
    isAlbum: Boolean = false,
    viewModel: PlaylistViewModel = koinViewModel(),
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val likedSongIds by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val collectState by viewModel.collectState.collectAsStateWithLifecycle()
    val userProfile  by viewModel.userProfile.collectAsStateWithLifecycle()
    val recommendedSongs by viewModel.recommendedSongs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    android.util.Log.d("PlaylistScreen", "PlaylistScreen Composition: playlistId = $playlistId, isAlbum = $isAlbum")

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { com.lin0721.linmusic.ui.components.ToastManager.showToast(it) }
    }
    LaunchedEffect(playlistId, isAlbum) {
        android.util.Log.d("PlaylistScreen", "LaunchedEffect Triggered: loading playlistId = $playlistId, isAlbum = $isAlbum")
        viewModel.loadPlaylist(playlistId, isAlbum)
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        when (val state = uiState) {
            is PlaylistUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeteaseRed)
                }
            is PlaylistUiState.Error ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = TextGray, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPlaylist(playlistId, isAlbum) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)) {
                            Text("重试", color = Color.White)
                        }
                        TextButton(onClick = onBack) { Text("返回", color = TextGray) }
                    }
                }
            is PlaylistUiState.Success ->
                PlaylistContent(
                    playlist       = state.playlist,
                    currentTrackId = currentTrack?.mediaId,
                    likedSongIds   = likedSongIds,
                    collectState   = collectState,
                    isLoggedIn     = userProfile != null,
                    recommendedSongs = recommendedSongs,
                    onBack         = onBack,
                    onArtistClick  = onArtistClick,
                    onPlaySong     = { track ->
                        viewModel.playSongInList(track, state.playlist.tracks)
                    },
                    onPlayAll = {
                        state.playlist.tracks.firstOrNull()?.let { first ->
                            viewModel.playSongInList(first, state.playlist.tracks)
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
                    }
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
                            com.lin0721.linmusic.ui.components.ToastManager.showToast("登录成功，正在同步数据...")
                        }
                    )
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
    onPlaySong: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveCollection: (Long, List<PlaylistCollectItem>) -> Unit,
    onSaveNewCollection: (String, Long) -> Unit,
    onRequireLogin: () -> Unit,
    onRefreshRecommendations: () -> Unit,
    onAddRecommendSong: (Track) -> Unit
) {
    val density = LocalDensity.current

    // 初始显示 index=1（封面），搜索栏 index=0 藏于上方，下拉可见
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
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
            when {
                listState.firstVisibleItemIndex == 0 -> 0f
                listState.firstVisibleItemIndex == 1 ->
                    (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
                else -> 1f
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

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. 滚动内容 ───────────────────────────────────────────────────
        LazyColumn(
            state          = listState,
            contentPadding = PaddingValues(bottom = 180.dp)
        ) {
            // Item 0：搜索栏（下拉可见）
            item(key = "search") {
                SearchBarItem(
                    query           = searchQuery, 
                    onQueryChange   = { searchQuery = it },
                    topPadding      = overlayHeight,
                    backgroundColor = dominantColor
                )
            }

            // Item 1：全出血 Hero
            // 封面从内容顶部延伸，视觉上与透明 overlay 无缝衔接
            item(key = "header") {
                PlaylistHeaderItem(
                    playlist          = playlist,
                    coverSize         = coverSize,
                    coverAlpha        = coverAlpha,
                    isCollapsed       = isCollapsed,
                    statusBarHeight   = statusBarHeight,
                    dominantColor     = dominantColor,
                    onColorCalculated = { dominantColor = it },
                    onPlayAll         = onPlayAll
                )
            }

            // 歌曲（支持搜索过滤）
            val filtered = if (searchQuery.isBlank()) playlist.tracks
                           else playlist.tracks.filter {
                               it.name.contains(searchQuery, true) ||
                               it.ar.any { a -> a.name.contains(searchQuery, true) }
                           }
            items(filtered, key = { it.id }) { track ->
                SongRow(
                    track        = track,
                    isActive     = currentTrackId == track.id.toString(),
                    likedSongIds = likedSongIds,
                    onClick      = { onPlaySong(track) },
                    onLikeClick  = {
                        if (!isLoggedIn) {
                            onRequireLogin()
                        } else {
                            collectSongId = track.id
                            onLikeClick(track.id)
                        }
                    },
                    onArtistClick = onArtistClick
                )
            }

            // 推荐歌曲板块
            if (recommendedSongs.isNotEmpty()) {
                item(key = "recommendation_header") {
                    RecommendationHeader(
                        onRefresh = onRefreshRecommendations
                    )
                }
                items(recommendedSongs, key = { "rec_${it.id}" }) { track ->
                    RecommendSongRow(
                        track = track,
                        isActive = currentTrackId == track.id.toString(),
                        onPlaySong = { onPlaySong(track) },
                        onAddClick = { onAddRecommendSong(track) },
                        onArtistClick = onArtistClick
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
            // 返回键：始终可见（白色图标浮在封面/背景上）
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Back",
                    tint = Color.White, modifier = Modifier.size(32.dp))
            }
            // 歌单名称：折叠后瞬间显示
            if (isCollapsed) {
                Text(
                    text       = playlist.name,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 60.dp)
                )
            }
        }

        // 折叠后吸附播放按钮（半挂 overlay 底部边缘）
        if (isCollapsed) {
            FloatingActionButton(
                onClick        = onPlayAll,
                containerColor = NeteaseRed,
                shape          = CircleShape,
                modifier       = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = overlayHeight - 28.dp)
                    .size(56.dp)
                    .zIndex(10f)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }

    if (collectSongId != null) {
        val songId = collectSongId!!
        ModalBottomSheet(
            onDismissRequest = { collectSongId = null },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    Surface(
                        modifier = Modifier.width(40.dp).height(4.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    ) {}
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "收藏到歌单",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (collectState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeteaseRed)
                    }
                } else {
                    val localItems = remember(collectState.collectItems) {
                        collectState.collectItems.map { it.copy() }.toMutableStateList()
                    }
                    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
                    var newPlaylistNameInput by remember { mutableStateOf("") }
                    val context = LocalContext.current

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "create_new_playlist_item") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        newPlaylistNameInput = ""
                                        showCreatePlaylistDialog = true
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeteaseRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "新建歌单",
                                        tint = NeteaseRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "新建歌单",
                                    color = Color.White,
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
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无其他可用歌单", color = TextGray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            itemsIndexed(localItems) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            localItems[index] = item.copy(isContains = !item.isContains)
                                        }
                                        .padding(vertical = 8.dp),
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
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item.playlistName,
                                            color = Color.White,
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
                                        colors = CheckboxDefaults.colors(checkedColor = NeteaseRed)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { collectSongId = null }) {
                            Text("取消", color = TextGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSaveCollection(songId, localItems)
                                collectSongId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("确定", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showCreatePlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreatePlaylistDialog = false },
                            title = { Text("新建歌单并收藏", color = Color.White, fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("请输入新歌单的名称：", color = TextGray, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BackgroundDark)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (newPlaylistNameInput.isEmpty()) {
                                            Text("歌单名称", color = TextGray, fontSize = 14.sp)
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = newPlaylistNameInput,
                                            onValueChange = { newPlaylistNameInput = it },
                                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                            cursorBrush = SolidColor(NeteaseRed),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (newPlaylistNameInput.isNotBlank()) {
                                            onSaveNewCollection(newPlaylistNameInput, songId)
                                            showCreatePlaylistDialog = false
                                        } else {
                                            com.lin0721.linmusic.ui.components.ToastManager.showToast("名字不能为空哦！")
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NeteaseRed)
                                ) {
                                    Text("创建并收藏", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                    Text("取消", color = Color.White)
                                }
                            },
                            containerColor = SurfaceDark,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Search, null, tint = TextGray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = Color.White,
                        fontSize = 14.sp
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("在此页面上查找", color = TextGray, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("排序", color = TextGray, fontSize = 13.sp)
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
    onPlayAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 使用从封面提取的主色调渐变到背景黑，实现高度一体化的视觉效果
            .background(Brush.verticalGradient(listOf(dominantColor, BackgroundDark)))
    ) {
        // 封面：偏移状态栏的高度，加上一点 padding，使其与操作区的返回键水平对齐
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarHeight + 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("${playlist.coverImgUrl}?param=400y400")
                    .allowHardware(false) // 禁用硬件位图，否则后续提取像素时会抛出异常导致降级为默认颜色
                    .crossfade(true)
                    .build(),
                contentDescription = playlist.name,
                contentScale       = ContentScale.Crop,
                onSuccess          = { state -> 
                    onColorCalculated(extractDominantColor(state.result.drawable))
                },
                modifier           = Modifier
                    .size(coverSize)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp), clip = false)
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (coverAlpha < 1f) Modifier.alpha(coverAlpha) else Modifier)
            )
        }

        // 歌单信息与操作行：折叠后隐藏
        if (!isCollapsed) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = NeteaseRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("为你打造", color = TextGray, fontSize = 13.sp)
                }
                if (!playlist.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(playlist.description, color = TextGray, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text("${playlist.tracks.size} 首歌曲", color = TextGray, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceDark))
                    Icon(Icons.Default.Add, "Add", tint = TextGray, modifier = Modifier.size(24.dp))
                    Icon(Icons.Default.Download, "Download", tint = TextGray, modifier = Modifier.size(24.dp))
                    Icon(Icons.Default.MoreVert, "More", tint = TextGray, modifier = Modifier.size(24.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = NeteaseRed, modifier = Modifier.size(26.dp))
                    FloatingActionButton(
                        onClick        = onPlayAll,
                        containerColor = NeteaseRed,
                        shape          = CircleShape,
                        modifier       = Modifier
                            .size(56.dp)
                            .shadow(6.dp, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 基础歌曲行组件（供普通歌曲行和推荐歌曲行复用，消除多余布局代码）
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun BaseSongRow(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    onArtistClick: (Long) -> Unit,
    actionArea: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isActive) SurfaceLight else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model              = "${track.al.picUrl}?param=100y100",
            contentDescription = track.name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.name,
                    color = if (isActive) NeteaseRed else Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.fee == 1) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(NeteaseRed.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                            .border(BorderStroke(0.5.dp, NeteaseRed), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "VIP",
                            color = NeteaseRed,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.ar.joinToString(" • ") { it.name },
                color = TextGray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    track.ar.firstOrNull()?.id?.let { artistId ->
                        onArtistClick(artistId)
                    }
                }
            )
        }
        actionArea()
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 歌曲行
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun SongRow(
    track: Track, 
    isActive: Boolean, 
    likedSongIds: Set<Long>,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onArtistClick: (Long) -> Unit
) {
    BaseSongRow(
        track = track,
        isActive = isActive,
        onClick = onClick,
        onArtistClick = onArtistClick
    ) {
        val isLiked = track.id in likedSongIds
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(32.dp).padding(end = 4.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "收藏歌单操作",
                tint = if (isLiked) NeteaseRed else TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
        Icon(Icons.Default.MoreVert, "More", tint = TextGray, modifier = Modifier.size(20.dp))
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
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "推荐歌曲",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onRefresh)
                .background(SurfaceDark)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新推荐",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "刷新",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 推荐歌曲行
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecommendSongRow(
    track: Track,
    isActive: Boolean,
    onPlaySong: () -> Unit,
    onAddClick: () -> Unit,
    onArtistClick: (Long) -> Unit
) {
    BaseSongRow(
        track = track,
        isActive = isActive,
        onClick = onPlaySong,
        onArtistClick = onArtistClick
    ) {
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加歌曲到歌单",
                tint = Color.White,
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(2.dp)
            )
        }
    }
}
