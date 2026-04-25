package com.lin0721.linmusic.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.lin0721.linmusic.data.remote.api.PersonalizedPlaylist
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.SpotifyGreen
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.TextGray
import com.lin0721.linmusic.ui.player.MiniPlayer
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
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { LinBottomNavigation() },
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF1B3B2B), BackgroundDark), startY = 0f, endY = 1000f
            )
        ),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Main content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // 给 MiniPlayer 留空间
            ) {
                // 原有的 TopGreetingBar 和 WelcomeBanner 放置于顶部
                item { TopGreetingBar() }
                item { WelcomeBanner() }

                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        item { LoadingContent() }
                    }
                    is HomeUiState.Error -> {
                        item {
                            ErrorContent(
                                message = state.message,
                                onRetry = { viewModel.loadPersonalizedPlaylists() }
                            )
                        }
                    }
                    is HomeUiState.Success -> {
                        item { SectionTitle(title = "Recommend for You", subtitle = "See All") }
                        item {
                            AlbumCarousel(
                                playlists = state.data.playlists,
                                onClick = { playlist ->
                                    onPlaylistClick(playlist.id)
                                }
                            )
                        }
                    }
                }
            }

            // 底部悬浮的 MiniPlayer
            MiniPlayer(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onTogglePlay = { viewModel.togglePlayPause() },
                onClick = onOpenPlayer,
                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
            )
        }
    }
}

// ─────────────────────── Loading ───────────────────────

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = SpotifyGreen,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading personalized recommendations...",
                fontSize = 14.sp,
                color = TextGray
            )
        }
    }
}

// ─────────────────────── Error ────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = TextGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Cannot load albums",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen
                )
            ) {
                Text(text = "Retry", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────── 新增的 UI 组件 ──────────────────────

@Composable
fun TopGreetingBar() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://picsum.photos/seed/avatar/100",
            contentDescription = "Profile",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Good Morning,", fontSize = 12.sp, color = TextGray)
            Text("lin", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        IconButton(onClick = { }) { Icon(Icons.Default.Notifications, "Notification", tint = Color.White) }
    }
}

@Composable
fun WelcomeBanner() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Welcome to", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Row {
            Text(text = "LinMusic ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SpotifyGreen)
            Text(text = "World", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(subtitle, fontSize = 12.sp, color = TextGray)
    }
}

@Composable
fun AlbumCarousel(playlists: List<PersonalizedPlaylist>, onClick: (PersonalizedPlaylist) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(playlists) { playlist ->
            Column(modifier = Modifier.width(140.dp).clickable { onClick(playlist) }) {
                AsyncImage(
                    model = "${playlist.picUrl}?param=300y300",
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(140.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LinBottomNavigation() {
    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
        val items = listOf(
            Triple("Home", Icons.Default.Home, true),
            Triple("Search", Icons.Default.Search, false),
            Triple("Library", Icons.Default.List, false),
            Triple("Profile", Icons.Default.Person, false)
        )
        items.forEach { (label, icon, selected) ->
            NavigationBarItem(
                selected = selected,
                onClick = { },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White, unselectedIconColor = TextGray,
                    selectedTextColor = Color.White, unselectedTextColor = TextGray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// Removed local MiniPlayer in favor of ui.player.MiniPlayer
