package com.lin0721.linmusic.feature.player.ui

import androidx.compose.runtime.Composable
import com.lin0721.linmusic.core.comment.ui.CommentsBottomSheet
import com.lin0721.linmusic.core.comment.ui.CommentsState
import com.lin0721.linmusic.core.model.CommentItem
import com.lin0721.linmusic.core.player.PlayMode
import com.lin0721.linmusic.core.player.QueueItem

// 全屏播放器的四个底部弹层：播放队列、更多操作、睡眠定时、评论
@Composable
fun FullPlayerSheets(
    songState: PlayerSongDetailState,
    showQueueSheet: Boolean,
    showMoreOptionsSheet: Boolean,
    showTimerSheet: Boolean,
    showCommentsSheet: Boolean,
    queue: List<QueueItem>,
    currentQueueIndex: Int,
    playMode: PlayMode,
    playContext: String?,
    isPlaying: Boolean,
    title: String,
    artist: String,
    coverUrl: String,
    sleepTimerRemaining: Long,
    activeQuality: String,
    commentsState: CommentsState,
    onPlayAtIndex: (Int) -> Unit,
    onRemoveAtIndex: (Int) -> Unit,
    onMoveQueueItem: (from: Int, to: Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onClearQueue: () -> Unit,
    onDisableRoaming: () -> Unit,
    onQueueDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onAlbumClick: () -> Unit,
    onArtistClick: () -> Unit,
    onShowTimerClick: () -> Unit,
    onQualitySelected: (String) -> Unit,
    onStartSimilarRoaming: () -> Unit,
    onInsertSimilarSongs: () -> Unit,
    onMoreOptionsDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onTimerDismiss: () -> Unit,
    onLikeComment: (CommentItem) -> Unit,
    onRetryComments: () -> Unit,
    onCommentsDismiss: () -> Unit
) {
    if (showQueueSheet) {
        PlayQueueSheet(
            queue = queue,
            currentIndex = currentQueueIndex,
            playMode = playMode,
            playContext = playContext,
            isPlaying = isPlaying,
            onPlayAtIndex = onPlayAtIndex,
            onRemoveAtIndex = onRemoveAtIndex,
            onMoveItem = onMoveQueueItem,
            onToggleShuffle = onToggleShuffle,
            onClearQueue = onClearQueue,
            onDisableRoaming = onDisableRoaming,
            onDismiss = onQueueDismiss
        )
    }

    if (showMoreOptionsSheet) {
        val albumName = songState.songDetail?.al?.name ?: songState.songWiki?.album ?: "未知专辑"
        SongMoreOptionsSheet(
            title = title,
            artist = artist,
            coverUrl = coverUrl,
            albumName = albumName,
            isLiked = songState.isLiked,
            sleepTimerRemaining = sleepTimerRemaining,
            currentQuality = activeQuality,
            onToggleLike = onToggleLike,
            onAlbumClick = onAlbumClick,
            onArtistClick = onArtistClick,
            onShowTimerClick = onShowTimerClick,
            onQualitySelected = onQualitySelected,
            onStartSimilarRoaming = onStartSimilarRoaming,
            onInsertSimilarSongs = onInsertSimilarSongs,
            onDismiss = onMoreOptionsDismiss
        )
    }

    if (showTimerSheet) {
        SleepTimerSheet(
            sleepTimerRemaining = sleepTimerRemaining,
            onSetTimer = onSetTimer,
            onDismiss = onTimerDismiss
        )
    }

    if (showCommentsSheet) {
        CommentsBottomSheet(
            commentsState = commentsState,
            onLikeComment = onLikeComment,
            onDismiss = onCommentsDismiss,
            onRetry = onRetryComments
        )
    }
}
