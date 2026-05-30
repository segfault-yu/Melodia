package com.lin0721.linmusic.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Track
import com.lin0721.linmusic.data.repository.ArtistInfo
import com.lin0721.linmusic.ui.components.LoginBottomSheet
import com.lin0721.linmusic.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

private val TOP_BAR_HEIGHT = 56.dp
private val COVER_MAX_SIZE = 280.dp
private val COVER_MIN_SIZE = 0.dp // 完全折叠收缩到 0

@Composable
fun ArtistScreen(
    artistId: Long,
    viewModel: ArtistViewModel = koinViewModel(),
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val likedSongIds by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val collectState by viewModel.collectState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { com.lin0721.linmusic.ui.components.ToastManager.showToast(it) }
    }
    LaunchedEffect(artistId) {
        viewModel.loadArtistData(artistId)
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeteaseRed)
                }
            }
            is ArtistUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = TextGray, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadArtistData(artistId) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)
                        ) {
                            Text("重试", color = Color.White)
                        }
                        TextButton(onClick = onBack) { Text("返回", color = TextGray) }
                    }
                }
            }
            is ArtistUiState.Success -> {
                ArtistContent(
                    artist = state.artist,
                    isFollowed = state.isFollowed,
                    fansCount = state.fansCount,
                    topSongs = state.topSongs,
                    albums = state.albums,
                    similarArtists = state.similarArtists,
                    likedSongIds = likedSongIds,
                    collectState = collectState,
                    isLoggedIn = userProfile != null,
                    onBack = onBack,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    onFollowClick = { viewModel.toggleFollow(artistId) },
                    onPlaySong = { track ->
                        viewModel.playSongInList(track, state.topSongs)
                    },
                    onPlayAll = {
                        state.topSongs.firstOrNull()?.let { first ->
                            viewModel.playSongInList(first, state.topSongs)
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
                    }
                )
            }
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
                    // 登录成功，通知 viewModel 同步用户信息并更新数据
                    viewModel.handleLoginSuccess(cookies)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistContent(
    artist: ArtistDetailInfo,
    isFollowed: Boolean,
    fansCount: Long,
    topSongs: List<Track>,
    albums: List<ArtistAlbum>,
    similarArtists: List<ArtistInfo>,
    likedSongIds: Set<Long>,
    collectState: PlaylistCollectState,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onFollowClick: () -> Unit,
    onPlaySong: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onLikeClick: (Long) -> Unit,
    onSaveCollection: (Long, List<PlaylistCollectItem>) -> Unit,
    onSaveNewCollection: (String, Long) -> Unit,
    onRequireLogin: () -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var isExpanded by remember { mutableStateOf(false) }

    var dominantColor by remember { mutableStateOf(Color(0xFF2C2C2C)) }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val overlayHeight = TOP_BAR_HEIGHT + statusBarHeight

    // 背景折叠临界点
    val collapseThresholdPx = with(density) { 320.dp.toPx() }
    val progress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseThresholdPx).coerceIn(0f, 1f)
        }
    }

    val overlayBgAlpha = progress
    val isCollapsed = progress >= 0.8f
    val bgScale = 1f + (1f - progress) * 0.1f // 微微视差拉伸

    var collectSongId by remember { mutableStateOf<Long?>(null) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showAllAlbumsSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. 全出血大图背景 (固定在顶部，随滚动折叠淡出)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .graphicsLayer {
                    translationY = -progress * collapseThresholdPx * 0.5f // 视差平移
                    scaleX = bgScale
                    scaleY = bgScale
                    alpha = 1f - progress * 0.7f // 渐隐
                }
        ) {
            val bgUrl = artist.cover.ifEmpty { artist.avatar }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (bgUrl.isNotEmpty()) "$bgUrl?param=640y640" else "")
                    .allowHardware(false)
                    .crossfade(true)
                    .build(),
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    dominantColor = extractDominantColor(state.result.drawable)
                },
                modifier = Modifier.fillMaxSize()
            )

            // 底部蒙层渐变到 BackgroundDark
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.5f),
                                BackgroundDark
                            )
                        )
                    )
            )
        }

        // 2. 滚动视口列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 180.dp)
        ) {
            // Item 0: 顶占位透明高，用于显示出大图 Header
            item(key = "header_placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        // Artist Name
                        Text(
                            text = artist.name,
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 48.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(6.dp))

                        // Fans/Monthly Listeners
                        Text(
                            text = "每月有 ${formatFansCount(fansCount)} 名听众",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Item 1: 操作控制行 (关注, 随机播放等)
            item(key = "action_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 圆角头像小缩略图
                    AsyncImage(
                        model = "${artist.avatar}?param=100y100",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )

                    Spacer(Modifier.width(12.dp))

                    // 关注状态卡片
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                border = if (isFollowed) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                else BorderStroke(0.dp, Color.Transparent),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(if (isFollowed) Color.Transparent else NeteaseRed)
                            .clickable(onClick = onFollowClick)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isFollowed) "已关注" else "关注",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, "More", tint = TextGray, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 随机播放
                    IconButton(onClick = onPlayAll) {
                        Icon(Icons.Default.Shuffle, "Shuffle", tint = NeteaseRed, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 红色圆圈大播放键
                    FloatingActionButton(
                        onClick = onPlayAll,
                        containerColor = NeteaseRed,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play All", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Item 2: “热门”标题
            item(key = "hot_tracks_title") {
                Text(
                    text = "热门歌曲",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            // 热门歌曲列表
            if (topSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(BackgroundDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无歌曲", color = TextGray, fontSize = 13.sp)
                    }
                }
            } else {
                val displaySongs = if (isExpanded) topSongs else topSongs.take(5)
                itemsIndexed(displaySongs, key = { _, track -> track.id }) { index, track ->
                    ArtistSongRow(
                        index = index + 1,
                        track = track,
                        likedSongIds = likedSongIds,
                        onClick = { onPlaySong(track) },
                        onLikeClick = {
                            if (!isLoggedIn) {
                                onRequireLogin()
                            } else {
                                collectSongId = track.id
                                onLikeClick(track.id)
                            }
                        }
                    )
                }
                if (topSongs.size > 5) {
                    item(key = "toggle_expand_songs") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDark)
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isExpanded) "收起" else "查看更多",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 专辑横向轮播区域
            if (albums.isNotEmpty()) {
                item(key = "albums_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark)
                            .padding(vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "全部专辑",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier
                                    .clickable { showAllAlbumsSheet = true }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "显示全部",
                                    color = NeteaseRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "查看全部",
                                    tint = NeteaseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albums.take(10), key = { it.id }) { album ->
                                AlbumCard(album = album, onClick = { onAlbumClick(album.id) })
                            }
                        }
                    }
                }
            }

            // 歌手简介卡片
            if (artist.briefDesc.isNotBlank()) {
                item(key = "bio_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "关于歌手",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .clickable { showBioDialog = true }
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = artist.briefDesc.trim(),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "查看全部介绍",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 相似歌手推荐
            if (similarArtists.isNotEmpty()) {
                item(key = "similar_artists_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark)
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "粉丝也喜欢",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(similarArtists, key = { it.id }) { artistInfo ->
                                SimilarArtistCard(artist = artistInfo, onClick = { onArtistClick(artistInfo.id) })
                            }
                        }
                    }
                }
            }
        }

        // 3. 悬浮的 TopBar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(overlayHeight)
                .background(dominantColor.copy(alpha = overlayBgAlpha))
                .padding(top = statusBarHeight)
                .zIndex(8f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (isCollapsed) {
                Text(
                    text = artist.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 60.dp)
                )
            }
        }

        // 折叠吸附播放按钮
        if (isCollapsed) {
            FloatingActionButton(
                onClick = onPlayAll,
                containerColor = NeteaseRed,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = overlayHeight - 24.dp)
                    .size(48.dp)
                    .zIndex(10f)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        // 4. 详情 Bottom Sheet (批量收藏)
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
                        val ctx = LocalContext.current

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
                                itemsIndexed(localItems) { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                localItems[idx] = item.copy(isContains = !item.isContains)
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
                                                localItems[idx] = item.copy(isContains = checked)
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

        // 6. 全部专辑 Bottom Sheet
        if (showAllAlbumsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAllAlbumsSheet = false },
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
                        text = "全部专辑",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albums, key = { it.id }) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAllAlbumsSheet = false
                                        onAlbumClick(album.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = "${album.picUrl}?param=150y150",
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val year = remember(album.publishTime) {
                                        try {
                                            val sdf = java.text.SimpleDateFormat("yyyy", Locale.getDefault())
                                            sdf.format(java.util.Date(album.publishTime))
                                        } catch (e: Exception) {
                                            "专辑"
                                        }
                                    }
                                    Text(
                                        text = "$year • ${album.size}首歌曲",
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "查看详情",
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. 歌手简介 Dialog
        if (showBioDialog) {
            AlertDialog(
                onDismissRequest = { showBioDialog = false },
                title = { Text(text = "关于 ${artist.name}", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 300.dp)) {
                        LazyColumn {
                            item {
                                Text(
                                    text = artist.briefDesc.trim(),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showBioDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeteaseRed)
                    ) {
                        Text("关闭", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 歌手歌曲 Row 单曲排版
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun ArtistSongRow(
    index: Int,
    track: Track,
    likedSongIds: Set<Long>,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(BackgroundDark)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 歌曲序号
        Text(
            text = index.toString(),
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )

        // 封面图
        AsyncImage(
            model = "${track.al.picUrl}?param=100y100",
            contentDescription = track.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.width(16.dp))

        // 标题
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // VIP 歌曲高显微标
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
        }

        Spacer(Modifier.width(8.dp))

        val isLiked = track.id in likedSongIds
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "喜欢/收藏歌曲",
                tint = if (isLiked) NeteaseRed else TextGray,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, "More", tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 歌手专辑 Card
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlbumCard(
    album: ArtistAlbum,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = "${album.picUrl}?param=200y200",
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .shadow(4.dp, RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = album.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        // 提取发布年份
        val year = remember(album.publishTime) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy", Locale.getDefault())
                sdf.format(java.util.Date(album.publishTime))
            } catch (e: Exception) {
                "专辑"
            }
        }
        Text(
            text = "$year • 专辑",
            color = TextGray,
            fontSize = 11.sp
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 相似艺人 Card (圆形头像 Row 浮动)
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun SimilarArtistCard(
    artist: ArtistInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "${artist.avatarUrl}?param=150y150",
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = artist.name,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// 辅助与本地数据转换工具
// ────────────────────────────────────────────────────────────────────────────

/**
 * 听众数量格式化转换 (千分位/万字)
 */
private fun formatFansCount(count: Long): String {
    return if (count >= 10000) {
        val num = count / 10000.0
        String.format(Locale.getDefault(), "%.1f万", num)
    } else {
        NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}

// 移除未使用的虚拟播放量生成工具
