package com.lin0721.linmusic.ui.home

import android.widget.Toast
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
    onOpenPlayer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val hazeState = remember { HazeState() }
    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drawerWidth = 310.dp
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    val drawerState = remember {
        AnchoredDraggableState<HomeSidebarState>(
            initialValue = HomeSidebarState.Closed,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(drawerWidthPx) {
        drawerState.updateAnchors(
            DraggableAnchors {
                HomeSidebarState.Closed at 0f
                HomeSidebarState.Open at drawerWidthPx
            }
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.playerManager.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.playerManager.duration.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val onAvatarClick: () -> Unit = {
        if (userProfile != null) {
            scope.launch {
                if (drawerState.currentValue == HomeSidebarState.Closed) {
                    drawerState.animateTo(HomeSidebarState.Open)
                } else {
                    drawerState.animateTo(HomeSidebarState.Closed)
                }
            }
        } else {
            showLoginSheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = drawerState,
                orientation = Orientation.Horizontal,
                enabled = userProfile != null
            )
            .background(BackgroundBlack)
    ) {
        // 1. 侧边栏层 (位于底部或同步移动)
        userProfile?.let { profile ->
            Box(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val offset = drawerState.offset
                        val progress = if (offset.isNaN()) 0f else (offset / drawerWidthPx).coerceIn(0f, 1f)
                        
                        translationX = (if (offset.isNaN()) 0f else offset) - drawerWidthPx
                        
                        // 侧边栏本身的渐变
                        alpha = 0.5f + (0.5f * progress)
                    }
            ) {
                ProfileSidebar(
                    userProfile = profile,
                    onLogout = {
                        viewModel.logout()
                        scope.launch { drawerState.animateTo(HomeSidebarState.Closed) }
                    },
                    onDismiss = {
                        scope.launch { drawerState.animateTo(HomeSidebarState.Closed) }
                    }
                )
            }
        }

        // 2. 主内容层 (跟随偏移，带高级视差与缩放效果)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = drawerState.offset
                    val progress = if (offset.isNaN()) 0f else (offset / drawerWidthPx).coerceIn(0f, 1f)
                    
                    translationX = if (offset.isNaN()) 0f else offset
                    
                    // 进阶视觉效果：圆角过渡
                    clip = true
                    shape = RoundedCornerShape((progress * 32).dp)
                    
                    // 动态阴影
                    shadowElevation = (progress * 30f)
                }
                .background(BackgroundDark)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().haze(hazeState),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                item { TopGreetingBar(userProfile = userProfile, onLoginClick = onAvatarClick) }
                item { WelcomeBanner() }
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

                        // 2. 热门歌手
                        if (state.data.topArtists.isNotEmpty()) {
                            item { SectionHeader(title = "关注的歌手") }
                            item {
                                ArtistCarousel(
                                    artists = state.data.topArtists,
                                    onClick = { /* artist click */ }
                                )
                            }
                        }

                        // 3. 私人雷达 (推荐歌单的反转展示作为示例)
                        item { SectionHeader(title = "你的私人雷达", showAction = false) }
                        item {
                            RecommendationCarousel(
                                playlists = state.data.recommendPlaylists.reversed(),
                                onClick = { onPlaylistClick(it.id) }
                            )
                        }

                        // 4. 最近播放
                        if (state.data.recentPlaylists.isNotEmpty()) {
                            item { SectionHeader(title = "最近播放") }
                            item {
                                RecentPlaylistCarousel(
                                    items = state.data.recentPlaylists,
                                    onClick = { item -> onPlaylistClick(item.data.id) }
                                )
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
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                BottomFloatingIsland(
                    hazeState = hazeState,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onOpenPlayer = onOpenPlayer
                )
            }

            // 3. 点击遮罩层 (当侧边栏打开时，主内容变暗且点击可关闭)
            val currentOffset = drawerState.offset
            if (!currentOffset.isNaN() && currentOffset > 0f) {
                val progress = currentOffset / drawerWidthPx
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * progress))
                        .clickable(
                            enabled = drawerState.currentValue == HomeSidebarState.Open,
                            onClick = { scope.launch { drawerState.animateTo(HomeSidebarState.Closed) } }
                        )
                )
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
                onQrLogin = {
                    Toast.makeText(context, "二维码登录正在开发中", Toast.LENGTH_SHORT).show()
                    showLoginSheet = false
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
    }
}

enum class HomeSidebarState {
    Closed, Open
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
fun TopGreetingBar(userProfile: UserProfile?, onLoginClick: () -> Unit) {
    val greeting = remember { getGreetingText() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (userProfile == null) Modifier.clickable { onLoginClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 24.dp),
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
                Text(text = "点击登录获取专属推荐", fontSize = 12.sp, color = Color.LightGray)
            }
        }
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Notifications, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun WelcomeBanner() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("欢迎来到", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Row {
            Text("云村 ", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = NeteaseRed)
            Text("的音乐世界", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
fun FilterPills() {
    val filters = listOf("全部", "音乐", "播客", "有声书", "直播")
    var selectedIndex by remember { mutableStateOf(0) }
    LazyRow(modifier = Modifier.padding(top = 24.dp), contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        items(playlists) { playlist ->
            Column(modifier = Modifier.width(160.dp).clickable { onClick(playlist) }) {
                AsyncImage(model = "${playlist.picUrl}?param=400y400", contentDescription = null, modifier = Modifier.size(160.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
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
        items(items) { item ->
            val playlist = item.data
            Column(modifier = Modifier.width(120.dp).clickable { onClick(item) }) {
                AsyncImage(model = "${playlist.picUrl}?param=300y300", contentDescription = null, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = playlist.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "歌单 · ${playlist.creator?.nickname ?: "网易云音乐"}", color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    songs.forEachIndexed { index, song ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(index * 60L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    slideInVertically(
                                        animationSpec = tween(300, easing = EaseOutCubic),
                                        initialOffsetY = { it / 2 }
                                    )
                        ) {
                            DailySongRow(
                                index = index + 1,
                                song = song,
                                isLast = index == songs.lastIndex,
                                onClick = { onPlaySong(index) }
                            )
                        }
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
fun ArtistCarousel(artists: List<Artist>, onClick: (Artist) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        items(artists) { artist ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick(artist) }) {
                AsyncImage(model = "${artist.picUrl}?param=200y200", contentDescription = null, modifier = Modifier.size(90.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = artist.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun BottomFloatingIsland(
    hazeState: HazeState,
    currentTrack: androidx.media3.common.MediaItem?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlay: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().hazeChild(state = hazeState, shape = RoundedCornerShape(32.dp), style = HazeStyle(tint = Color.Black.copy(alpha = 0.4f), blurRadius = 24.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))) {
        Column(modifier = Modifier.padding(12.dp)) {
            AnimatedVisibility(visible = currentTrack != null) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onOpenPlayer() }.padding(horizontal = 4.dp)) {
                        AsyncImage(model = currentTrack?.mediaMetadata?.artworkUri, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = currentTrack?.mediaMetadata?.title?.toString() ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = currentTrack?.mediaMetadata?.artist?.toString() ?: "未知艺术家", color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onTogglePlay) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.1f))) {
                        val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                        Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(NeteaseRed))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                StaticNavItem("主页", Icons.Default.Home, true)
                StaticNavItem("搜索", Icons.Default.Search, false)
                StaticNavItem("音乐库", Icons.Default.LibraryMusic, false)
                StaticNavItem("创建", Icons.Default.AddBox, false)
            }
        }
    }
}

@Composable
fun StaticNavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }.padding(4.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
        Text(text = label, color = if (isSelected) Color.White else Color.Gray, fontSize = 10.sp)
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
