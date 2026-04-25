package com.lin0721.linmusic.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.ui.player.MiniPlayer
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.SpotifyGreen
import com.lin0721.linmusic.ui.theme.SurfaceLight
import com.lin0721.linmusic.ui.theme.TextGray
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistScreen(
    playlistId: Long,
    viewModel: PlaylistViewModel = koinViewModel(),
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        when (val state = uiState) {
            is PlaylistUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
            is PlaylistUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Error loading playlist", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message, color = TextGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPlaylist(playlistId) }, colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)) {
                            Text("Retry", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Back") }
                    }
                }
            }
            is PlaylistUiState.Success -> {
                val playlist = state.playlist
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // 头部视觉区
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF282828), BackgroundDark)
                                    )
                                )
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.padding(top = 48.dp, start = 8.dp)) {
                                Icon(Icons.Rounded.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Column(
                                modifier = Modifier.fillMaxSize().padding(top = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = "${playlist.coverImgUrl}?param=300y300",
                                    contentDescription = null,
                                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // 歌单信息与操作区
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(playlist.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LinMusic | ${playlist.tracks.size} Songs | ${(playlist.playCount / 10000)}w Plays", color = TextGray, fontSize = 12.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Heart", tint = TextGray)
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextGray)
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextGray)
                                }
                                FloatingActionButton(
                                    onClick = { 
                                        if (playlist.tracks.isNotEmpty()) {
                                            val firstTrack = playlist.tracks.first()
                                            viewModel.playSong(firstTrack.id, firstTrack.name, firstTrack.ar.firstOrNull()?.name ?: "Unknown", firstTrack.al.picUrl)
                                        }
                                    },
                                    containerColor = SpotifyGreen,
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }

                    // 歌曲列表
                    items(playlist.tracks) { track ->
                        val isActive = currentTrack?.mediaId == track.id.toString()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) SurfaceLight else Color.Transparent)
                                .clickable {
                                    viewModel.playSong(
                                        songId = track.id,
                                        title = track.name,
                                        artist = track.ar.firstOrNull()?.name ?: "Unknown",
                                        coverUrl = track.al.picUrl
                                    )
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.name, color = if (isActive) SpotifyGreen else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(track.ar.joinToString { it.name }, color = TextGray, fontSize = 14.sp)
                            }
                            AsyncImage(
                                model = "${track.al.picUrl}?param=100y100",
                                contentDescription = track.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
        
        MiniPlayer(
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onTogglePlay = { viewModel.playerManager.togglePlayPause() },
            onClick = onOpenPlayer,
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
        )
    }
}
