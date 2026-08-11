package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import com.lin0721.linmusic.feature.artist.data.ArtistAlbum
import com.lin0721.linmusic.feature.artist.data.ArtistDetailInfo
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.artist.domain.ArtistInfo
import com.lin0721.linmusic.core.ui.components.CreatePlaylistDialog
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.MelodiaDragHandle
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.extractDominantColor
import com.lin0721.linmusic.feature.playlist.ui.PlaylistCollectState
import com.lin0721.linmusic.feature.playlist.ui.PlaylistCollectItem
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

private val TOP_BAR_HEIGHT = 56.dp

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
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { com.lin0721.linmusic.core.ui.components.ToastManager.showToast(it) }
    }
    LaunchedEffect(artistId) {
        viewModel.loadArtistData(artistId)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ArtistUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("加载失败", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(MelodiaSpacing.sm))
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(MelodiaSpacing.md))
                        Button(
                            onClick = { viewModel.loadArtistData(artistId) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("重试", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onBack) { Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            is ArtistUiState.Success -> {
                val blockedArtistIds by viewModel.blockedArtistIds.collectAsStateWithLifecycle()
                ArtistContent(
                    artist = state.artist,
                    isFollowed = state.isFollowed,
                    fansCount = state.fansCount,
                    topSongs = state.topSongs,
                    albums = state.albums,
                    similarArtists = state.similarArtists,
                    likedSongIds = likedSongIds,
                    blockedArtistIds = blockedArtistIds,
                    currentTrackId = currentTrack?.mediaId,
                    collectState = collectState,
                    isLoggedIn = userProfile != null,
                    onBack = onBack,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    onFollowClick = { viewModel.toggleFollow(artistId) },
                    onBlockClick = { viewModel.toggleBlockArtist(artistId) },
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
    blockedArtistIds: Set<Long>,
    currentTrackId: String?,
    collectState: PlaylistCollectState,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onFollowClick: () -> Unit,
    onBlockClick: () -> Unit,
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
    var showMoreMenuSheet by remember { mutableStateOf(false) }

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
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("音乐", "专辑", "MV", "关于艺人")

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. 大图背景
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
        }

        // 1b. dominantColor 独立底色层：压在封面图上方，高度延伸到操作栏底部，与 Tab 栏接在一起
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = -progress * collapseThresholdPx * 0.5f // 与大图同步视差平移
                    alpha = 1f - progress * 0.7f // 与大图同步渐隐
                }
        ) {
            // 纯色底色向下延伸,底边缘与底部的 tab_bar 接在一起
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(dominantColor)
            )
        }


        // 2. 滚动视口列表
        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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
                        .height(310.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // 歌手名称和粉丝数区域的渐变蒙层
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .padding(horizontal = MelodiaSpacing.md)
                            .padding(bottom = MelodiaSpacing.md, top = MelodiaSpacing.lg)
                    ) {
                        // Artist Name
                        Text(
                            text = artist.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 48.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(6.dp))

                        // 显示歌手的译名
                        val chineseName = (artist.trans?.takeIf { it.isNotBlank() }
                            ?: artist.alias?.firstOrNull { it.isNotBlank() })?.takeIf { it != artist.name }

                        if (!chineseName.isNullOrBlank()) {
                            Text(
                                text = chineseName,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }

            // Item 1: 操作控制行 (关注, 随机播放等)
            item(key = "action_bar") {
                // 操作栏背景线性渐变
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp) // 固定高度为 80.dp
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.60f), // 顶部：80% 不透明
                                    MaterialTheme.colorScheme.background.copy(alpha = 1.00f)  // 底部：90% 不透明
                                )
                            )
                        )
                        .padding(start = MelodiaSpacing.md, end = MelodiaSpacing.md, top = MelodiaSpacing.xs, bottom = MelodiaSpacing.xs), 
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 左侧控制列 (包含粉丝数与头像控制项)
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "共有 ${formatFansCount(fansCount)} 位听众",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(MelodiaSpacing.sm))
                        Row(
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
                                    .background(if (isFollowed) Color.Transparent else MaterialTheme.colorScheme.primary)
                                    .clickable(onClick = onFollowClick)
                                    .padding(horizontal = MelodiaSpacing.md, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isFollowed) "已关注" else "关注",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.width(MelodiaSpacing.md))

                            IconButton(
                                onClick = { showMoreMenuSheet = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // 右侧播放控制项
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Bottom)
                    ) {
                        // 随机播放
                        IconButton(
                            onClick = onPlayAll,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, "Shuffle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 红色圆圈大播放键
                        FloatingActionButton(
                            onClick = onPlayAll,
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play All", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Item 2: 分页 Tab 栏
            item(key = "tab_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = MelodiaSpacing.md, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedTab = index }
                                .padding(vertical = MelodiaSpacing.xs)
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // 指示器：选中时显示横线，未选中时透明
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(28.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }

            // 根据选中的 Tab 呈现对应内容
            when (selectedTab) {
                0 -> { // 音乐 Tab: 显示当前歌手的全部热门歌曲
                    if (topSongs.isEmpty()) {
                        item(key = "empty_songs") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    } else {
                        itemsIndexed(topSongs, key = { _, track -> track.id }) { index, track ->
                            val isActive = track.id.toString() == currentTrackId
                            SongRow(
                                data = SongRowData(
                                    id = track.id,
                                    title = track.name,
                                    artist = track.ar.joinToString(" • ") { it.name },
                                    coverUrl = track.al.picUrl,
                                    isVip = track.fee == 1
                                ),
                                isActive = isActive,
                                index = index + 1,
                                onClick = { onPlaySong(track) },
                                trailingSlot = {
                                    val isLiked = track.id in likedSongIds
                                    IconButton(
                                        onClick = {
                                            if (!isLoggedIn) {
                                                onRequireLogin()
                                            } else {
                                                collectSongId = track.id
                                                onLikeClick(track.id)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).padding(end = MelodiaSpacing.xs)
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "喜欢/收藏歌曲",
                                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                1 -> { // 专辑 Tab: 采用精致的行模式展示
                    if (albums.isEmpty()) {
                        item(key = "empty_albums") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(albums, key = { it.id }) { album ->
                            ArtistAlbumRow(album = album, onClick = { onAlbumClick(album.id) })
                        }
                    }
                }
                2 -> { // MV Tab: 占位设计
                    item(key = "empty_mv") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "暂无MV",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(MelodiaSpacing.md))
                            Text(
                                text = "暂无相关的 MV 视频",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(MelodiaSpacing.xs))
                            Text(
                                text = "艺人尚未上传或暂无播放授权",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                3 -> { // 关于艺人 Tab: 艺人简介 + 相似艺人推荐
                    item(key = "bio_content") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = MelodiaSpacing.md, vertical = 20.dp)
                        ) {
                            Text(
                                text = "艺人简介",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { showBioDialog = true }
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = artist.briefDesc.ifBlank { "暂无艺人简介信息" }.trim(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        maxLines = 8,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (artist.briefDesc.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "点击查看完整介绍",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (similarArtists.isNotEmpty()) {
                        item(key = "similar_artists_section") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = MelodiaSpacing.md)
                            ) {
                                Text(
                                    text = "相似艺人推荐",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(start = MelodiaSpacing.md, end = MelodiaSpacing.md, bottom = 12.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = MelodiaSpacing.md),
                                    horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)
                                ) {
                                    items(similarArtists, key = { it.id }) { artistInfo ->
                                        SimilarArtistCard(artist = artistInfo, onClick = { onArtistClick(artistInfo.id) })
                                    }
                                }
                            }
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
                    .padding(start = MelodiaSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (isCollapsed) {
                Text(
                    text = artist.name,
                    color = MaterialTheme.colorScheme.onSurface,
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

        // 4. 详情 Bottom Sheet (批量收藏)
        if (collectSongId != null) {
            val songId = collectSongId!!
            ModalBottomSheet(
                onDismissRequest = { collectSongId = null },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = BottomSheetShape,
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
                        val ctx = LocalContext.current

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
                                            .clip(RoundedCornerShape(6.dp))
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
                                itemsIndexed(localItems, key = { _, item -> item.playlistId }) { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                localItems[idx] = item.copy(isContains = !item.isContains)
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
                                                    .clip(RoundedCornerShape(6.dp))
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
                                                localItems[idx] = item.copy(isContains = checked)
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
                                shape = RoundedCornerShape(12.dp)
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

        // 5. 歌手简介 Dialog
        if (showBioDialog) {
            AlertDialog(
                onDismissRequest = { showBioDialog = false },
                title = { Text(text = "关于 ${artist.name}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 300.dp)) {
                        LazyColumn {
                            item {
                                Text(
                                    text = artist.briefDesc.trim(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("关闭", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
        }

        // 7. 更多操作 Bottom Sheet
        if (showMoreMenuSheet) {
            val isBlocked = blockedArtistIds.contains(artist.id)
            ModalBottomSheet(
                onDismissRequest = { showMoreMenuSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = BottomSheetShape,
                dragHandle = { MelodiaDragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = MelodiaSpacing.lg, vertical = MelodiaSpacing.md)
                ) {
                    Text(
                        text = "歌手操作",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = MelodiaSpacing.md)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreMenuSheet = false
                                onBlockClick()
                            }
                            .padding(vertical = MelodiaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (isBlocked) "取消屏蔽" else "屏蔽歌手",
                            tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(MelodiaSpacing.md))
                        Column {
                            Text(
                                text = if (isBlocked) "取消屏蔽该艺人所有歌曲" else "屏蔽该艺人所有歌曲",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBlocked) "取消屏蔽后，其歌曲将重新显示在推荐和搜索中" else "屏蔽后，其歌曲将不再在推荐和搜索中展示",
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

        Spacer(Modifier.height(MelodiaSpacing.sm))

        Text(
            text = artist.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MelodiaSpacing.xs)
        )
    }
}

@Composable
private fun ArtistAlbumRow(
    album: ArtistAlbum,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "${album.picUrl}?param=150y150",
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(MelodiaSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            val publishTimeStr = remember(album.publishTime) {
                if (album.publishTime > 0) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(album.publishTime))
                } else ""
            }
            val desc = buildString {
                if (publishTimeStr.isNotEmpty()) append(publishTimeStr)
                if (album.size > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("${album.size}首歌曲")
                }
            }
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "详情",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatFansCount(count: Long): String {
    return if (count >= 10000) {
        val num = count / 10000.0
        String.format(Locale.getDefault(), "%.1f万", num)
    } else {
        NumberFormat.getNumberInstance(Locale.US).format(count)
    }
}
