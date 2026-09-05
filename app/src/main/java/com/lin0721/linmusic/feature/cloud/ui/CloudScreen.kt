package com.lin0721.linmusic.feature.cloud.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.LocalBottomOverlayInset
import com.lin0721.linmusic.core.ui.components.ErrorState
import com.lin0721.linmusic.core.ui.components.PlaylistCollectSheet
import com.lin0721.linmusic.core.ui.components.SearchResultRowSkeleton
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.interaction.pressScale
import com.lin0721.linmusic.core.ui.theme.MelodiaPress
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.feature.cloud.upload.UploadStatus
import org.koin.androidx.compose.koinViewModel

private const val SKELETON_ROW_COUNT = 8
// LazyColumn 第 0 项是容量海报头图，歌曲行的 list index 相应整体 +1
private const val HERO_ITEM_OFFSET = 1

@Composable
fun CloudScreen(
    viewModel: CloudViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val collectState by viewModel.collectState.collectAsStateWithLifecycle()
    val likedSongIds by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val uploadTasks by viewModel.uploadTasks.collectAsStateWithLifecycle()
    var showUploadSheet by remember { mutableStateOf(false) }

    val hasActiveUploads = uploadTasks.any { it.status != UploadStatus.SUCCESS && it.status != UploadStatus.FAILED }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.startUpload(uris)
            showUploadSheet = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { ToastManager.showToast(it) }
    }

    SecondaryScreenScaffold(title = "我的云盘", onBack = onBack) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = uiState) {
                CloudUiState.Loading -> {
                    Column(modifier = Modifier.padding(top = MelodiaSpacing.sm)) {
                        repeat(SKELETON_ROW_COUNT) { SearchResultRowSkeleton() }
                    }
                }

                is CloudUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(message = state.message, onRetry = { viewModel.load() })
                    }
                }

                is CloudUiState.Success -> {
                    val listState = rememberLazyListState()
                    val shouldLoadMore by remember(state.hasMore, state.isLoadingMore) {
                        derivedStateOf {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible >= state.songs.size - 3 + HERO_ITEM_OFFSET && state.hasMore && !state.isLoadingMore
                        }
                    }
                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) viewModel.loadMore()
                    }

                    LazyColumn(
                        state = listState,
                        // 行内组件（CloudSongRow/CloudStorageHeroBlock）自带水平内边距，这里只留垂直间距
                        contentPadding = PaddingValues(
                            top = MelodiaSpacing.sm,
                            bottom = LocalBottomOverlayInset.current + 16.dp
                        )
                    ) {
                        item(key = "hero") {
                            CloudStorageHeroBlock(
                                quota = state.quota,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.sm)
                            )
                        }

                        items(state.songs, key = { it.songId }) { song ->
                            CloudSongRow(
                                song = song,
                                onClick = { viewModel.playSong(song) },
                                onOptionsClick = { viewModel.openOptions(song) }
                            )
                        }

                        if (state.isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(MelodiaSpacing.md),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 有上传在跑时点它回看进度，没有时点它选文件
            val fabInteraction = remember { MutableInteractionSource() }
            FloatingActionButton(
                onClick = {
                    if (hasActiveUploads) {
                        showUploadSheet = true
                    } else {
                        filePickerLauncher.launch(arrayOf("audio/*"))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                interactionSource = fabInteraction,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = MelodiaSpacing.md,
                        bottom = LocalBottomOverlayInset.current + MelodiaSpacing.md
                    )
                    .pressScale(MelodiaPress.Transport, fabInteraction)
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                if (hasActiveUploads) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = "上传到云盘",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    if (showUploadSheet && uploadTasks.isNotEmpty()) {
        CloudUploadProgressSheet(
            tasks = uploadTasks,
            onRetry = { taskId -> viewModel.retryUpload(taskId) },
            onDismiss = { showUploadSheet = false }
        )
    }

    when (val current = overlay) {
        CloudOverlay.Hidden -> Unit

        is CloudOverlay.Options -> {
            CloudSongOptionsSheet(
                song = current.song,
                isLiked = current.song.songId in likedSongIds,
                onDismiss = { viewModel.dismissOverlay() },
                onToggleLikeClick = { viewModel.toggleLike() },
                onAddToPlaylistClick = { viewModel.openAddToPlaylist() },
                onMatchClick = { viewModel.openMatch() },
                onDeleteClick = { viewModel.requestDelete() }
            )
        }

        is CloudOverlay.AddToPlaylist -> {
            PlaylistCollectSheet(
                songId = current.song.songId,
                collectState = collectState,
                onDismiss = { viewModel.dismissOverlay() },
                onSaveCollection = { songId, items -> viewModel.saveAddToPlaylist(songId, items) },
                onSaveNewCollection = { name, songId -> viewModel.createPlaylistAndAddSong(name, songId) }
            )
        }

        is CloudOverlay.DeleteConfirm -> {
            CloudDeleteConfirmDialog(
                songName = current.song.name,
                isDeleting = current.isDeleting,
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissOverlay() }
            )
        }

        is CloudOverlay.Match -> {
            CloudMatchSheet(
                overlay = current,
                onQueryChange = { viewModel.updateMatchQuery(it) },
                onSelect = { viewModel.selectMatchCandidate(it) },
                onCancelConfirm = { viewModel.cancelMatchConfirm() },
                onConfirm = { viewModel.confirmMatch() },
                onDismiss = { viewModel.dismissOverlay() }
            )
        }
    }
}
