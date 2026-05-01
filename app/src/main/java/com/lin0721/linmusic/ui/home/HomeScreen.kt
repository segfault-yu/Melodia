package com.lin0721.linmusic.ui.home

import android.widget.Toast
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.data.remote.api.PersonalizedPlaylist
import com.lin0721.linmusic.data.remote.api.Artist
import com.lin0721.linmusic.ui.theme.*
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.ui.components.LoginBottomSheet
import com.lin0721.linmusic.ui.components.ProfileSidebar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onPlaylistClick: (Long) -> Unit = {},
    onOpenPlayer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val hazeState = remember { HazeState() }

    // 登录弹窗状态
    var showLoginSheet by remember { mutableStateOf(false) }

    // 侧边栏状态（2D挤压效果）
    var isSidebarOpen by remember { mutableStateOf(false) }

    // Toast 监听
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

    // 头像点击：已登录开侧边栏，未登录开登录弹窗
    val onAvatarClick: () -> Unit = {
        if (userProfile != null) {
            isSidebarOpen = !isSidebarOpen
        } else {
            showLoginSheet = true
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 侧边栏：2D挤压动画
        AnimatedVisibility(
            visible = isSidebarOpen && userProfile != null,
            enter = expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = shrinkHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut()
        ) {
            userProfile?.let { profile ->
                ProfileSidebar(
                    userProfile = profile,
                    onLogout = {
                        viewModel.logout()
                        isSidebarOpen = false
                    },
                    onDismiss = {
                        isSidebarOpen = false
                    }
                )
            }
        }

        // 主内容区域（占据剩余空间，会被挤压）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GradientStart, BackgroundDark, BackgroundBlack),
                        startY = 0f,
                        endY = 2000f
                    )
                )
        ) {
            // Haze 内容提供层
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                item { TopGreetingBar(userProfile = userProfile, onLoginClick = onAvatarClick) }
                item { WelcomeBanner() }
                item { FilterPills() }

                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        item { LoadingIndicator() }
                    }
                    is HomeUiState.Error -> {
                        item {
                            ErrorContent(
                                message = state.message,
                                onRetry = { viewModel.loadHomeData() }
                            )
                        }
                    }
                    is HomeUiState.Success -> {
                        item { SectionHeader(title = "为你推荐") }
                        item {
                            RecommendationCarousel(
                                playlists = state.data.recommendPlaylists,
                                onClick = { onPlaylistClick(it.id) }
                            )
                        }

                        if (state.data.topArtists.isNotEmpty()) {
                            item { SectionHeader(title = "关注的歌手") }
                            item {
                                ArtistCarousel(
                                    artists = state.data.topArtists,
                                    onClick = { /* artist click */ }
                                )
                            }
                        }

                        item { SectionHeader(title = "你的私人雷达") }
                        item {
                            RecommendationCarousel(
                                playlists = state.data.recommendPlaylists.reversed(),
                                onClick = { onPlaylistClick(it.id) }
                            )
                        }
                    }
                }
            }

            // 底部悬浮舱
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
        }
    }

    // 登录弹窗（放在最外层）
    if (showLoginSheet) {
        LoginBottomSheet(
            onDismiss = { showLoginSheet = false },
            onPhoneLogin = {
                viewModel.simulateLogin()
                showLoginSheet = false
            },
            onQrLogin = {
                viewModel.simulateLogin()
                showLoginSheet = false
            },
            onEmailLogin = {
                viewModel.simulateLogin()
                showLoginSheet = false
            }
        )
    }
}

/**
 * 动态问候语工具函数
 */
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
    onLoginClick: () -> Unit
) {
    val greeting = remember { getGreetingText() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // 未登录时整个区域可点击跳转登录
                if (userProfile == null) Modifier.clickable { onLoginClick() }
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像区域
        if (userProfile != null) {
            // 已登录：真实头像
            AsyncImage(
                model = "${userProfile.avatarUrl}?param=200y200",
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onLoginClick() },
                contentScale = ContentScale.Crop
            )
        } else {
            // 未登录：默认占位头像
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 文字区域
        Column(modifier = Modifier.weight(1f)) {
            if (userProfile != null) {
                Text(
                    text = "$greeting，",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Text(
                    text = userProfile.nickname,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "未登录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "点击登录获取专属推荐",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        // 通知铃铛
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .border(2.dp, Color(0xFF2a1215), CircleShape)
            )
        }
    }
}

@Composable
fun WelcomeBanner() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "欢迎来到",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp
        )
        Row {
            Text(
                text = "云村 ",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeteaseRed,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "的音乐世界",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun FilterPills() {
    val filters = listOf("全部", "音乐", "播客", "有声书", "直播")
    var selectedIndex by remember { mutableStateOf(0) }
    
    LazyRow(
        modifier = Modifier.padding(top = 24.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filters.size) { index ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) NeteaseRed else Color.White.copy(alpha = 0.1f))
                    .clickable { selectedIndex = index }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filters[index],
                    color = if (isSelected) Color.White else Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = "查看全部", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun RecommendationCarousel(playlists: List<PersonalizedPlaylist>, onClick: (PersonalizedPlaylist) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(playlists) { playlist ->
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .clickable { onClick(playlist) }
            ) {
                AsyncImage(
                    model = "${playlist.picUrl}?param=400y400",
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "根据你的口味生成",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ArtistCarousel(artists: List<Artist>, onClick: (Artist) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(artists) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onClick(artist) }
            ) {
                AsyncImage(
                    model = "${artist.picUrl}?param=200y200",
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = artist.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(32.dp),
                style = HazeStyle(
                    tint = Color.Black.copy(alpha = 0.4f),
                    blurRadius = 24.dp
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // --- 上半部分: MiniPlayer 与进度条（仅有歌曲时展示） ---
            AnimatedVisibility(
                visible = currentTrack != null,
                enter = expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300, delayMillis = 80)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onOpenPlayer() }
                            .padding(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        AsyncImage(
                            model = currentTrack?.mediaMetadata?.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack?.mediaMetadata?.title?.toString() ?: "",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* Cast */ }) {
                                Icon(Icons.Default.Cast, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onTogglePlay) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(NeteaseRed)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }


            // --- 下半部分: 四大导航 Icon（始终显示） ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { }
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp).padding(bottom = 2.dp)
        )
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NeteaseRed)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("哎呀，获取数据失败了哦！", color = Color.White, fontSize = 16.sp)
        Text(message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed), modifier = Modifier.padding(top = 16.dp)) {
            Text("重试")
        }
    }
}
