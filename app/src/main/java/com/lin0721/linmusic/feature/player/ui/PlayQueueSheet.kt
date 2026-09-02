package com.lin0721.linmusic.feature.player.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.ui.components.MelodiaIconButton
import com.lin0721.linmusic.core.ui.components.MelodiaTextButton
import com.lin0721.linmusic.core.ui.components.MelodiaButton
import com.lin0721.linmusic.core.player.PlayMode
import com.lin0721.linmusic.core.player.QueueItem
import com.lin0721.linmusic.core.ui.components.DraggableSongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.SurfaceDark
import com.lin0721.linmusic.core.ui.theme.SurfaceLight
import com.lin0721.linmusic.core.ui.theme.TextGray
import kotlinx.coroutines.launch
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQueueSheet(
    queue: List<QueueItem>,
    currentIndex: Int,
    playMode: PlayMode,
    playContext: String?,
    isPlaying: Boolean,
    onPlayAtIndex: (Int) -> Unit,
    onRemoveAtIndex: (Int) -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onClearQueue: () -> Unit,
    onDisableRoaming: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isRoaming = playContext == "similar_roaming"

    LaunchedEffect(currentIndex) {
        if (isRoaming) {
            listState.scrollToItem(0)
        } else if (currentIndex in queue.indices) {
            // 已播放占用的项数 = 1 (已播放头部) + currentIndex (已播放的歌曲数量)
            val scrollIndex = if (currentIndex > 0) currentIndex + 1 else 0
            listState.scrollToItem(scrollIndex)
        }
    }

    // 拖拽状态
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("清空播放队列", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("确定要清空播放队列吗？", color = TextGray, fontSize = 14.sp) },
            confirmButton = {
                MelodiaTextButton(
                    onClick = {
                        onClearQueue()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = NeteaseRed)
                ) {
                    Text("是的", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                MelodiaTextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消", color = Color.White)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(10.dp)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        shape = BottomSheetShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = MelodiaSpacing.xs)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(DragHandleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            QueueHeader(
                queueSize = queue.size,
                onClearClick = { showClearConfirmDialog = true },
                onDismiss = onDismiss
            )

            PlayModeInfoRow(
                playMode = playMode,
                playContext = playContext,
                onToggleShuffle = onToggleShuffle,
                onDisableRoaming = onDisableRoaming
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = MelodiaSpacing.md)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                // 1. "已播放" (仅在非漫游模式下显示)
                if (!isRoaming && currentIndex > 0) {
                    item(key = "header_played") {
                        SectionLabel("已播放")
                    }
                    itemsIndexed(
                        items = queue.subList(0, currentIndex),
                        key = { idx, item -> "played_${item.songId}_$idx" }
                    ) { idx, item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onRemoveAtIndex(idx)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { SwipeDeleteBackground() },
                            enableDismissFromStartToEnd = false
                        ) {
                            DraggableSongRow(
                                data = SongRowData(id = item.songId, title = item.title, artist = item.artist, coverUrl = item.coverUrl),
                                isCurrent = false,
                                isPlaying = false,
                                isPlayed = true,
                                isDragging = false,
                                dragOffsetY = 0f,
                                onClick = { onPlayAtIndex(idx) },
                                onDragStart = {},
                                onDrag = { _ -> },
                                onDragEnd = {}
                            )
                        }
                    }
                }

                // 2. "正在播放"
                if (currentIndex in queue.indices) {
                    item(key = "header_current") {
                        SectionLabel("正在播放")
                    }
                    item(key = "current_${queue[currentIndex].songId}") {
                        if (isRoaming) {
                            // 漫游模式下禁用侧滑删除
                            DraggableSongRow(
                                data = SongRowData(id = queue[currentIndex].songId, title = queue[currentIndex].title, artist = queue[currentIndex].artist, coverUrl = queue[currentIndex].coverUrl),
                                isCurrent = true,
                                isPlaying = isPlaying,
                                isPlayed = false,
                                isDragging = false,
                                dragOffsetY = 0f,
                                onClick = { onPlayAtIndex(currentIndex) },
                                onDragStart = {},
                                onDrag = { _ -> },
                                onDragEnd = {}
                            )
                        } else {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        onRemoveAtIndex(currentIndex)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { SwipeDeleteBackground() },
                                enableDismissFromStartToEnd = false
                            ) {
                                DraggableSongRow(
                                    data = SongRowData(id = queue[currentIndex].songId, title = queue[currentIndex].title, artist = queue[currentIndex].artist, coverUrl = queue[currentIndex].coverUrl),
                                    isCurrent = true,
                                    isPlaying = isPlaying,
                                    isPlayed = false,
                                    isDragging = false,
                                    dragOffsetY = 0f,
                                    onClick = { onPlayAtIndex(currentIndex) },
                                    onDragStart = {},
                                    onDrag = { _ -> },
                                    onDragEnd = {}
                                )
                            }
                        }
                    }
                }

                // 3. "接下来播放" (仅在非漫游模式下显示)
                if (!isRoaming) {
                    val upcomingStart = currentIndex + 1
                    if (upcomingStart < queue.size) {
                        item(key = "header_upcoming") {
                            SectionLabel(
                                if (playMode == PlayMode.SHUFFLE) "× 随机播放来源：${playContext ?: ""}"
                                else "接下来播放"
                            )
                        }
                        itemsIndexed(
                            items = queue.subList(upcomingStart, queue.size),
                            key = { idx, item -> "upcoming_${item.songId}_$idx" }
                        ) { idx, item ->
                            val actualIndex = upcomingStart + idx
                            val isDragging = draggedIndex == actualIndex
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        onRemoveAtIndex(actualIndex)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { SwipeDeleteBackground() },
                                enableDismissFromStartToEnd = false
                            ) {
                                DraggableSongRow(
                                    data = SongRowData(id = item.songId, title = item.title, artist = item.artist, coverUrl = item.coverUrl),
                                    isCurrent = false,
                                    isPlaying = false,
                                    isPlayed = false,
                                    isDragging = isDragging,
                                    dragOffsetY = if (isDragging) dragOffset else 0f,
                                    onClick = { onPlayAtIndex(actualIndex) },
                                    onDragStart = {
                                        draggedIndex = actualIndex
                                        dragOffset = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { delta ->
                                        dragOffset += delta
                                        val itemHeight = 64f
                                        val threshold = itemHeight * 0.6f
                                        if (dragOffset > threshold && actualIndex < queue.size - 1) {
                                            onMoveItem(draggedIndex, draggedIndex + 1)
                                            draggedIndex += 1
                                            dragOffset -= itemHeight
                                        } else if (dragOffset < -threshold && actualIndex > upcomingStart) {
                                            onMoveItem(draggedIndex, draggedIndex - 1)
                                            draggedIndex -= 1
                                            dragOffset += itemHeight
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = -1
                                        dragOffset = 0f
                                    }
                                )
                            }
                        }
                    }
                }
            }

            BottomActionRow(
                playMode = playMode,
                playContext = playContext,
                onToggleShuffle = onToggleShuffle,
                onDisableRoaming = onDisableRoaming
            )
        }
    }
}

@Composable
private fun SwipeDeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeteaseRed)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun QueueHeader(
    queueSize: Int,
    onClearClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "队列",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (queueSize > 1) {
            MelodiaIconButton(
                onClick = onClearClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "清空队列",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            "共 $queueSize 首",
            color = TextGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = MelodiaSpacing.sm)
        )
        MelodiaIconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlayModeInfoRow(
    playMode: PlayMode,
    playContext: String?,
    onToggleShuffle: () -> Unit,
    onDisableRoaming: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = MelodiaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isRoaming = playContext == "similar_roaming"
        val (icon, label) = when {
            isRoaming -> Icons.Rounded.AllInclusive to "相似歌曲漫游"
            playMode == PlayMode.LIST_LOOP -> Icons.Default.Repeat to "列表循环"
            playMode == PlayMode.SINGLE_LOOP -> Icons.Default.RepeatOne to "单曲循环"
            else -> Icons.Default.Shuffle to "随机播放"
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .clickable {
                    if (isRoaming) {
                        onDisableRoaming()
                    } else {
                        onToggleShuffle()
                    }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = NeteaseRed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 12.sp)
        }
        if (!playContext.isNullOrBlank() && !isRoaming) {
            Spacer(Modifier.width(12.dp))
            Text(
                "来自：$playContext",
                color = TextGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextGray,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun BottomActionRow(
    playMode: PlayMode,
    playContext: String?,
    onToggleShuffle: () -> Unit,
    onDisableRoaming: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.md, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val isRoaming = playContext == "similar_roaming"
        MelodiaButton(
            onClick = {
                if (isRoaming) {
                    onDisableRoaming()
                } else {
                    onToggleShuffle()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRoaming || playMode == PlayMode.SHUFFLE) NeteaseRed else SurfaceDark
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Icon(
                imageVector = if (isRoaming) Icons.Rounded.AllInclusive else Icons.Default.Shuffle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isRoaming) "关闭漫游" else "随机播放", color = Color.White, fontSize = 13.sp)
        }
        MelodiaButton(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("定时器", color = Color.White, fontSize = 13.sp)
        }
    }
}
