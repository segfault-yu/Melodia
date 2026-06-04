package com.lin0721.linmusic.ui.home

import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.data.remote.api.PersonalizedPlaylist
import com.lin0721.linmusic.data.remote.api.Artist
import com.lin0721.linmusic.data.remote.api.DailySong
import com.lin0721.linmusic.data.repository.ToplistInfo
import com.lin0721.linmusic.ui.theme.*
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.ui.components.LoginBottomSheet
import com.lin0721.linmusic.ui.components.ProfileSidebar
import com.lin0721.linmusic.ui.components.WebViewLoginScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onPlaylistClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    onLoginScreenVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    // 监听网页登录界面可见性变化，并通知上层以隐藏悬浮底栏
    LaunchedEffect(showWebViewLogin) {
        onLoginScreenVisibilityChanged(showWebViewLogin)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            com.lin0721.linmusic.ui.components.ToastManager.showToast(message)
        }
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

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
            .background(BackgroundDark)
    ) {
        // 动态光效底层
            DynamicAmbientLight()

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

                when (val state = uiState) {
                    is HomeUiState.Loading -> { item { LoadingIndicator() } }
                    is HomeUiState.Error -> {
                        item {
                            ErrorContent(message = state.message, onRetry = { viewModel.loadHomeData() })
                        }
                    }
                    is HomeUiState.Success -> {
                        // 1. 推荐歌单
                        item { SectionHeader(title = "为你推荐", showAction = false) }
                        item {
                            RecommendationCarousel(
                                playlists = state.data.recommendPlaylists,
                                onClick = { onPlaylistClick(it.id) }
                            )
                        }

                        // 4. 最近播放
                        if (state.data.recentPlaylists.isNotEmpty()) {
                            item {
                                var recentViewIsGrid by remember { mutableStateOf(false) }

                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 8.dp, top = 36.dp, bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "最近播放",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { recentViewIsGrid = !recentViewIsGrid }) {
                                            Icon(
                                                imageVector = if (recentViewIsGrid) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                                                contentDescription = "切换视图",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    if (recentViewIsGrid) {
                                        RecentPlaylistGrid(
                                            items = state.data.recentPlaylists,
                                            onClick = { item -> onPlaylistClick(item.data.id) }
                                        )
                                    } else {
                                        RecentPlaylistCarousel(
                                            items = state.data.recentPlaylists,
                                            onClick = { item -> onPlaylistClick(item.data.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. 每日推荐
                        if (state.data.dailySongs.isNotEmpty()) {
                            item { SectionHeader(title = "今日推荐", showAction = false) }
                            item {
                                var showHistorySheet by remember { mutableStateOf(false) }
                                val historyDates by viewModel.historyDates.collectAsStateWithLifecycle()
                                val historyDatesLoading by viewModel.historyDatesLoading.collectAsStateWithLifecycle()
                                val historySongs by viewModel.historySongs.collectAsStateWithLifecycle()
                                val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                                val historySongsLoading by viewModel.historySongsLoading.collectAsStateWithLifecycle()

                                DailyRecommendCard(
                                    songs = state.data.dailySongs,
                                    onPlayAll = { viewModel.playDailySong(0) },
                                    onPlaySong = { index -> viewModel.playDailySong(index) },
                                    onViewHistory = {
                                        showHistorySheet = true
                                        viewModel.loadHistoryDates()
                                    }
                                )

                                if (showHistorySheet) {
                                    HistoryRecommendSheet(
                                        dates = historyDates,
                                        datesLoading = historyDatesLoading,
                                        songs = historySongs,
                                        selectedDate = selectedDate,
                                        songsLoading = historySongsLoading,
                                        onDateSelected = { viewModel.loadHistoryDetail(it) },
                                        onPlaySong = { viewModel.playHistorySong(it) },
                                        onDismiss = { showHistorySheet = false }
                                    )
                                }
                            }
                        }

                        // 6. 排行榜
                        if (state.data.toplistItems.isNotEmpty()) {
                            item { SectionHeader(title = "排行榜", showAction = false) }
                            item { ToplistCarousel(toplists = state.data.toplistItems) }
                        }

                        // 7. 你最爱的艺人
                        if (state.data.favoriteArtists.isNotEmpty()) {
                            item { FavoriteArtistsSection(artists = state.data.favoriteArtists) }
                        }
                    }
                }
            }

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

private fun getGreetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }
}

@Composable
fun TopGreetingBar(
    userProfile: UserProfile?, 
    onLoginClick: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    val greeting = remember { getGreetingText() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (userProfile == null) Modifier.clickable { onLoginClick() } else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userProfile != null) {
            AsyncImage(
                model = "${userProfile.avatarUrl}?param=200y200",
                contentDescription = "用户头像",
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onLoginClick() },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (userProfile != null) {
                Text(text = "$greeting，", fontSize = 12.sp, color = Color.LightGray)
                Text(text = userProfile.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text(text = "未登录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "点击登录", fontSize = 12.sp, color = Color.LightGray)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onSearchClick() }, 
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}



@Composable
fun FilterPills() {
    val filters = listOf("全部", "音乐", "播客")
    var selectedIndex by remember { mutableStateOf(0) }
    LazyRow(modifier = Modifier.padding(top = 8.dp), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filters.size) { index ->
            val isSelected = index == selectedIndex
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isSelected) NeteaseRed else Color.White.copy(alpha = 0.1f)).clickable { selectedIndex = index }.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(text = filters[index], color = if (isSelected) Color.White else Color.LightGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, showAction: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (showAction) { Text(text = "显示全部", color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable
fun RecommendationCarousel(playlists: List<PersonalizedPlaylist>, onClick: (PersonalizedPlaylist) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            Column(modifier = Modifier.width(160.dp).clickable { onClick(playlist) }) {
                AsyncImage(model = "${playlist.picUrl}?param=200y200", contentDescription = null, modifier = Modifier.size(160.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = playlist.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "根据你的口味生成", color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun RecentPlaylistCarousel(items: List<com.lin0721.linmusic.data.remote.api.RecentPlayItem>, onClick: (com.lin0721.linmusic.data.remote.api.RecentPlayItem) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(items, key = { it.data.id }) { item ->
            val playlist = item.data
            Column(modifier = Modifier.width(120.dp).clickable { onClick(item) }) {
                AsyncImage(model = "${playlist.picUrl}?param=200y200", contentDescription = null, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = playlist.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "歌单 · ${playlist.creator?.nickname ?: "网易云音乐"}", color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun RecentPlaylistGrid(
    items: List<com.lin0721.linmusic.data.remote.api.RecentPlayItem>,
    onClick: (com.lin0721.linmusic.data.remote.api.RecentPlayItem) -> Unit
) {
    val gridItems = items.take(9)
    val rows = gridItems.chunked(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEach { item ->
                    val playlist = item.data
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(item) }
                    ) {
                        AsyncImage(
                            model = "${playlist.picUrl}?param=300y300",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ToplistCarousel(toplists: List<ToplistInfo>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items = toplists, key = { it.id }) { item ->
            ToplistCard(item = item)
        }
    }
}

@Composable
private fun ToplistCard(item: ToplistInfo) {
    val context = LocalContext.current
    val imageRequest = remember(item.coverUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(item.coverUrl)
            .size(360, 360)
            .crossfade(true)
            .build()
    }
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1F2E))
    ) {
        // 封面图 + 渐变蒙层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 底部渐变遥控可读性
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF1A1F2E))
                        )
                    )
            )
            // 更新频率徽章
            if (item.updateDesc.isNotBlank()) {
                Text(
                    text = item.updateDesc,
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.45f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        // 榜单名
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DailyRecommendCard(
    songs: List<DailySong>,
    onPlayAll: () -> Unit,
    onPlaySong: (Int) -> Unit,
    onViewHistory: () -> Unit = {}
) {
    if (songs.isEmpty()) return

    val today = remember {
        val cal = Calendar.getInstance()
        "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }
    val dayOfMonth = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B2E))
    ) {
        Column {
            // 卡片头部：渐变色标题区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFCC0000), Color(0xFF1A1F3A))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 日历图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayOfMonth,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "DAY",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "每日推荐",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = today,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    }
                    // 历史推荐按钮
                    IconButton(
                        onClick = { onViewHistory() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "历史推荐",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 播放全部按钮
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onPlayAll() },
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "播放全部",
                                tint = NeteaseRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // 歌曲列表（全部，可滚动）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(songs) { index, song ->
                        DailySongRow(
                            index = index + 1,
                            song = song,
                            isLast = index == songs.lastIndex,
                            onClick = { onPlaySong(index) }
                        )
                    }
                }
                // 底部渐出蒙层，提示可继续滚动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF161B2E))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun DailySongRow(
    index: Int,
    song: DailySong,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 序号
        Text(
            text = index.toString().padStart(2, '0'),
            color = if (index == 1) NeteaseRed else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
        // 专辑封面
        AsyncImage(
            model = "${song.al.picUrl}?param=120y120",
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artistStr = song.ar.joinToString(" / ") { it.name }
            val subtitle = if (!song.reason.isNullOrBlank()) song.reason else artistStr
            Text(
                text = subtitle,
                color = if (!song.reason.isNullOrBlank()) Color(0xFFFFAA44) else Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
    // 分隔线（最后一项不显示）
    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 60.dp, end = 20.dp),
            color = Color.White.copy(alpha = 0.06f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun FavoriteArtistsSection(artists: List<com.lin0721.linmusic.data.repository.ArtistInfo>) {
    Column {
        SectionHeader(title = "你最爱的艺人", showAction = false)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistCircleCard(artist)
            }
        }
    }
}

@Composable
fun ArtistCircleCard(artist: com.lin0721.linmusic.data.repository.ArtistInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp).clickable { /* artist click */ }
    ) {
        // 圆形头像
        AsyncImage(
            model = "${artist.avatarUrl}?param=200y200", 
            contentDescription = artist.name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape), // 裁剪为圆形
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 歌手名
        Text(
            text = artist.name,
            fontSize = 13.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeteaseRed) }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("哎呀，获取数据失败了哦！", color = Color.White, fontSize = 16.sp)
        Text(message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)) { Text("重试") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryRecommendSheet(
    dates: List<String>,
    datesLoading: Boolean,
    songs: List<DailySong>,
    selectedDate: String?,
    songsLoading: Boolean,
    onDateSelected: (String) -> Unit,
    onPlaySong: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12172A),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFCC0000), Color(0xFF880000))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "历史每日推荐",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "黑胶 VIP 专属功能",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.Gray)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            if (datesLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeteaseRed, modifier = Modifier.size(32.dp))
                }
            } else if (dates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无历史记录", color = Color.Gray, fontSize = 14.sp)
                        Text("需要黑胶 VIP 会员", color = Color.Gray.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    // 左侧日期列表
                    LazyColumn(
                        modifier = Modifier
                            .width(110.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF0D1120)),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(dates) { date ->
                            val isSelected = date == selectedDate
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDateSelected(date) }
                                    .background(
                                        if (isSelected) NeteaseRed.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 14.dp, horizontal = 12.dp)
                            ) {
                                // 日期显示：拆分为月/日两行
                                val parts = date.split("-")
                                val monthDay = if (parts.size == 3) "${parts[1]}/${parts[2]}" else date
                                val year = parts.firstOrNull() ?: ""
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = monthDay,
                                        color = if (isSelected) NeteaseRed else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = year,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(3.dp)
                                            .height(20.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(NeteaseRed)
                                    )
                                }
                            }
                        }
                    }

                    // 右侧歌曲列表
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (songsLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeteaseRed, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                itemsIndexed(songs) { index, song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPlaySong(index) }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = "${song.al.picUrl}?param=100y100",
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.name,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.ar.joinToString(" / ") { it.name },
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (index < songs.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 64.dp, end = 12.dp),
                                            color = Color.White.copy(alpha = 0.05f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DynamicAmbientLight() {
    // 采用全屏画布
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 顶部中央偏左的红色光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeteaseRed.copy(alpha = 0.20f),
                    NeteaseRed.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(width * 0.3f, height * 0.15f),
                radius = width * 1.3f
            ),
            center = Offset(width * 0.3f, height * 0.15f),
            radius = width * 1.3f
        )

        // 底部漫长柔化遮罩
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    BackgroundDark.copy(alpha = 0.2f),
                    BackgroundDark.copy(alpha = 0.6f),
                    BackgroundDark.copy(alpha = 0.9f),
                    BackgroundDark
                ),
                startY = height * 0.15f,
                endY = height * 0.8f
            )
        )
    }
}
