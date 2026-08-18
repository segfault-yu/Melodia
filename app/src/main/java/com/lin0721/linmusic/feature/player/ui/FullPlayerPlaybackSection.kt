package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.lin0721.linmusic.core.player.PlayMode
import com.lin0721.linmusic.core.ui.theme.ColorPalette

// 播放器主区：封面、歌名歌手、单行歌词、进度条、播放控制、快捷操作
// 拆成多个 item 而非包一层，保证滚动回收与 firstVisibleItemIndex 判定不受影响
fun LazyListScope.fullPlayerPlaybackSection(
    songState: PlayerSongDetailState,
    colors: FullPlayerColors,
    coverUrl: String,
    title: String,
    artist: String,
    playContext: String?,
    currentLyricIndex: Int,
    isPlaying: Boolean,
    currentPositionProvider: () -> Long,
    duration: Long,
    playMode: PlayMode,
    coverScaleProvider: () -> Float,
    onClose: () -> Unit,
    onPaletteExtracted: (ColorPalette) -> Unit,
    onMoreClick: () -> Unit,
    onToggleLike: () -> Unit,
    onArtistClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onDisableRoaming: () -> Unit,
    onTimerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onInsertSimilarClick: () -> Unit
) {
    item(key = "cover") {
        FullPlayerCoverArt(
            coverUrl = coverUrl,
            title = title,
            playContext = playContext,
            onClose = onClose,
            onPaletteExtracted = onPaletteExtracted,
            onMoreClick = onMoreClick,
            modifier = Modifier.graphicsLayer {
                val scale = coverScaleProvider()
                scaleX = scale
                scaleY = scale
            }
        )
    }

    item(key = "song_info") {
        SongInfo(
            title = title,
            artist = artist,
            isLiked = songState.isLiked,
            onToggleLike = onToggleLike,
            onArtistClick = onArtistClick
        )
    }

    item(key = "mini_lyric") {
        MiniLyricLine(
            lyrics = songState.lyrics,
            currentLyricIndex = currentLyricIndex,
            lyricsHighlight = colors.lyricsHighlight,
            isPlaying = isPlaying
        )
    }

    item(key = "progress") {
        // 播放器尚未拿到时长时退回歌曲详情里的时长
        val displayDuration = if (duration > 0L) duration else (songState.songDetail?.dt ?: 0L)
        ProgressSection(
            currentPositionProvider = currentPositionProvider,
            duration = displayDuration,
            onSeek = onSeek
        )
    }

    item(key = "controls") {
        PlaybackControls(
            isPlaying = isPlaying,
            onTogglePlay = onTogglePlay,
            onPlayNext = onPlayNext,
            onPlayPrevious = onPlayPrevious,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            playMode = playMode,
            isRoaming = playContext == "similar_roaming",
            onDisableRoaming = onDisableRoaming
        )
    }

    item(key = "actions") {
        ActionButtons(
            onTimerClick = onTimerClick,
            onQueueClick = onQueueClick,
            onInsertSimilarClick = onInsertSimilarClick
        )
    }
}
