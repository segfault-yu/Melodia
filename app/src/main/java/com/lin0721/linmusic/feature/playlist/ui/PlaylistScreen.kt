package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.MelodiaDragHandle
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.comment.ui.CommentsBottomSheet
import org.koin.androidx.compose.koinViewModel

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
    val isPlaying by viewModel.playerManager.isPlaying.collectAsStateWithLifecycle()
    val likedSongIds by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val collectState by viewModel.collectState.collectAsStateWithLifecycle()
    val userProfile  by viewModel.userProfile.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val historyRecommendState by viewModel.historyRecommendState.collectAsStateWithLifecycle()

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
                    isPlaying      = isPlaying,
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

private data class PlaylistMenuItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit
)
