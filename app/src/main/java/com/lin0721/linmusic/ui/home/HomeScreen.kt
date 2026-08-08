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
import com.lin0721.linmusic.ui.components.FilterChipsRow
import com.lin0721.linmusic.ui.components.LoginBottomSheet
import com.lin0721.linmusic.ui.components.ProfileSidebar
import com.lin0721.linmusic.ui.components.SongRow
import com.lin0721.linmusic.ui.components.SongRowData
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
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                        item {
                            ForYouModule(
                                dailySongs = state.data.dailySongs,
                                toplists = state.data.toplistItems,
                                recommendPlaylists = state.data.recommendPlaylists,
                                onDailyRecommendClick = { onPlaylistClick(-1L) },
                                onHotlistClick = { onPlaylistClick(it) },
                                onIntelligenceClick = { viewModel.startIntelligenceMode() },
                                onRadarClick = { onPlaylistClick(it) },
                                onRoamingClick = { viewModel.startRoaming() }
                            )
                        }

                        // 推荐歌单
                        item { SectionHeader(title = "推荐歌单", showAction = false) }
                        item {
                            RecommendationCarousel(
                                playlists = state.data.recommendPlaylists,
                                onClick = { onPlaylistClick(it.id) }
                            )
                        }

                        // 最近播放
                        if (state.data.recentPlaylists.isNotEmpty()) {
                            item {
                                var recentViewIsGrid by remember { mutableStateOf(false) }

                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = MelodiaSpacing.sm, top = 36.dp, bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(MelodiaSpacing.sm))
                                        Text(
                                            text = "最近播放",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { recentViewIsGrid = !recentViewIsGrid }) {
                                            Icon(
                                                imageVector = if (recentViewIsGrid) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                                                contentDescription = "切换视图",
                                                tint = MaterialTheme.colorScheme.onSurface
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

                        // 排行榜
                        if (state.data.toplistItems.isNotEmpty()) {
                            item { SectionHeader(title = "排行榜", showAction = false) }
                            item { ToplistCarousel(toplists = state.data.toplistItems) }
                        }

                        // 你最爱的艺人
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
            .padding(start = 20.dp, end = 20.dp, top = MelodiaSpacing.lg, bottom = 0.dp),
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
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (userProfile != null) {
                Text(text = "$greeting，", fontSize = 12.sp, color = Color.LightGray)
                Text(text = userProfile.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(text = "未登录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }
    }
}



@Composable
fun FilterPills() {
    var selectedIndex by remember { mutableStateOf(0) }
    FilterChipsRow(
        items = listOf("全部", "音乐", "播客"),
        selectedIndex = selectedIndex,
        onSelected = { selectedIndex = it }
    )
}

@Composable
fun SectionHeader(title: String, showAction: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (showAction) { Text(text = "显示全部", color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable
fun RecommendationCarousel(playlists: List<PersonalizedPlaylist>, onClick: (PersonalizedPlaylist) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)) {
        items(playlists, key = { it.id }) { playlist ->
            Column(modifier = Modifier.width(150.dp).clickable { onClick(playlist) }) {
                val context = LocalContext.current
                val imageRequest = remember(playlist.picUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data("${playlist.picUrl}?param=200y200")
                        .crossfade(true)
                        .build()
                }
                AsyncImage(model = imageRequest, contentDescription = null, modifier = Modifier.size(150.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = playlist.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun RecentPlaylistCarousel(items: List<com.lin0721.linmusic.data.remote.api.RecentPlayItem>, onClick: (com.lin0721.linmusic.data.remote.api.RecentPlayItem) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)) {
        items(items, key = { it.data.id }) { item ->
            val playlist = item.data
            Column(modifier = Modifier.width(150.dp).clickable { onClick(item) }) {
                val context = LocalContext.current
                val imageRequest = remember(playlist.picUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data("${playlist.picUrl}?param=300y300")
                        .crossfade(true)
                        .build()
                }
                AsyncImage(model = imageRequest, contentDescription = null, modifier = Modifier.size(150.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = playlist.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(MelodiaSpacing.xxs))
                Text(text = "歌单 · ${playlist.creator?.nickname ?: "网易云音乐"}", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = playlist.name,
                            color = MaterialTheme.colorScheme.onSurface,
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
        horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)
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
            .size(300, 300)
            .crossfade(true)
            .build()
    }
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1F2E))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF1A1F2E))
                        )
                    )
            )
            if (item.updateDesc.isNotBlank()) {
                Text(
                    text = item.updateDesc,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(MelodiaSpacing.sm)
                        .background(
                            Color.Black.copy(alpha = 0.45f),
                            MaterialTheme.shapes.extraSmall
                        )
                        .padding(horizontal = 6.dp, vertical = MelodiaSpacing.xxs)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun FavoriteArtistsSection(artists: List<com.lin0721.linmusic.data.repository.ArtistInfo>) {
    Column {
        SectionHeader(title = "你最爱的艺人", showAction = false)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)
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
        val context = LocalContext.current
        val imageRequest = remember(artist.avatarUrl) {
            coil.request.ImageRequest.Builder(context)
                .data("${artist.avatarUrl}?param=200y200")
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = artist.name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(MelodiaSpacing.sm))
        Text(
            text = artist.name,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(MelodiaSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("哎呀，获取数据失败了哦！", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Text(message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = MelodiaSpacing.sm))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("重试") }
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
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = MelodiaSpacing.md),
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
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "历史每日推荐",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
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
                        Text("需要 VIP 会员", color = Color.Gray.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 顶部的日期 LazyRow
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = MelodiaSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dates) { date ->
                            val isSelected = date == selectedDate
                            val parts = date.split("-")
                            val monthDay = if (parts.size == 3) "${parts[1]}/${parts[2]}" else date
                            val year = parts.firstOrNull() ?: ""

                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.03f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { onDateSelected(date) }
                                    .padding(vertical = MelodiaSpacing.sm, horizontal = MelodiaSpacing.md),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = monthDay,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(MelodiaSpacing.xxs))
                                    Text(
                                        text = year,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // 下方的歌曲列表
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        if (songsLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = MelodiaSpacing.sm)
                            ) {
                                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                    SongRow(
                                        data = SongRowData(
                                            id = song.id,
                                            title = song.name,
                                            artist = song.ar.joinToString(" / ") { it.name },
                                            coverUrl = song.al.picUrl
                                        ),
                                        compact = true,
                                        onClick = { onPlaySong(index) }
                                    )
                                    if (index < songs.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 68.dp, end = MelodiaSpacing.md),
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

            Spacer(modifier = Modifier.height(MelodiaSpacing.xl))
        }
    }
}

@Composable
fun ForYouModule(
    dailySongs: List<DailySong>,
    toplists: List<ToplistInfo>,
    recommendPlaylists: List<PersonalizedPlaylist>,
    onDailyRecommendClick: () -> Unit,
    onHotlistClick: (Long) -> Unit,
    onIntelligenceClick: () -> Unit,
    onRadarClick: (Long) -> Unit,
    onRoamingClick: () -> Unit
) {
    val hotlist = remember(toplists) {
        toplists.firstOrNull { it.name.contains("热") } ?: toplists.firstOrNull()
    }

    val radarPlaylist = remember(recommendPlaylists) {
        recommendPlaylists.firstOrNull { it.name.contains("雷达") } ?: recommendPlaylists.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "为你推荐", showAction = false)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)
        ) {
            item {
                val coverUrl = dailySongs.firstOrNull()?.al?.picUrl
                ForYouCard(
                    title = "每日推荐",
                    subtitle = "专属歌曲每日更新",
                    coverUrl = coverUrl,
                    fallbackGradientColors = listOf(Color(0xFFE53935), Color(0xFFE35D5B)),
                    onClick = onDailyRecommendClick
                )
            }

            item {
                ForYouCard(
                    title = "热歌榜",
                    subtitle = "最热音乐随时听",
                    coverUrl = hotlist?.coverUrl,
                    fallbackGradientColors = listOf(Color(0xFFFFB300), Color(0xFFFBC02D)),
                    onClick = { hotlist?.let { onHotlistClick(it.id) } }
                )
            }

            item {
                val coverUrl = recommendPlaylists.getOrNull(2)?.picUrl
                ForYouCard(
                    title = "心动模式",
                    subtitle = "开启智能红心电台",
                    coverUrl = coverUrl,
                    fallbackGradientColors = listOf(Color(0xFF8E24AA), Color(0xFFAB47BC)),
                    onClick = onIntelligenceClick
                )
            }

            item {
                ForYouCard(
                    title = "私人雷达",
                    subtitle = "根据喜好精准定制",
                    coverUrl = radarPlaylist?.picUrl,
                    fallbackGradientColors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5)),
                    onClick = { radarPlaylist?.let { onRadarClick(it.id) } }
                )
            }

            item {
                val coverUrl = recommendPlaylists.getOrNull(1)?.picUrl
                ForYouCard(
                    title = "音乐漫游",
                    subtitle = "无限探索相似歌曲",
                    coverUrl = coverUrl,
                    fallbackGradientColors = listOf(Color(0xFF43A047), Color(0xFF66BB6A)),
                    onClick = onRoamingClick
                )
            }
        }
    }
}

@Composable
private fun ForYouCard(
    title: String,
    subtitle: String,
    coverUrl: String?,
    fallbackGradientColors: List<Color>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // 固化清晰封面的加载请求
    val coverRequest = remember(coverUrl) {
        if (!coverUrl.isNullOrBlank()) {
            coil.request.ImageRequest.Builder(context)
                .data("$coverUrl?param=300y300")
                .crossfade(true)
                .build()
        } else {
            null
        }
    }
    // 固化低清虚化背景的加载请求 (拉取 30x30 小图)
    val blurBgRequest = remember(coverUrl) {
        if (!coverUrl.isNullOrBlank()) {
            coil.request.ImageRequest.Builder(context)
                .data("$coverUrl?param=30y30")
                .crossfade(true)
                .build()
        } else {
            null
        }
    }
    val fallbackIcon = when (title) {
        "每日推荐" -> Icons.Rounded.DateRange
        "热歌榜" -> Icons.Rounded.Star
        "心动模式" -> Icons.Rounded.Favorite
        "私人雷达" -> Icons.Rounded.Search
        "音乐漫游" -> Icons.Rounded.Shuffle
        else -> Icons.Rounded.MusicNote
    }

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        // 底层模糊背景
        if (blurBgRequest != null) {
            AsyncImage(
                model = blurBgRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
                    .graphicsLayer(alpha = 0.35f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = fallbackGradientColors))
            )
        }

        // 上方 150dp 封面区
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(150.dp)
        ) {
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = fallbackGradientColors))
                )
            }

            if (coverUrl.isNullOrBlank()) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 底部副标题区
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
