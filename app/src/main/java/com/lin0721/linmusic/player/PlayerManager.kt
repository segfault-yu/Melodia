package com.lin0721.linmusic.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lin0721.linmusic.data.local.PlaybackPreferences
import com.lin0721.linmusic.data.local.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayerManager(
    private val context: Context,
    private val playbackPreferences: PlaybackPreferences
) : Player.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        // 每秒更新播放进度
        scope.launch {
            while (true) {
                if (_isPlaying.value) {
                    _currentPosition.value = controller?.currentPosition ?: 0L
                }
                delay(1000)
            }
        }
    }

    suspend fun initController() {
        if (controller != null) return

        // 1. 尝试从本地存储恢复状态（在连接服务前，先给 UI 一个占位）
        val lastState = playbackPreferences.playbackState.first()
        if (lastState.songId != -1L && _currentTrack.value == null) {
            val bundle = Bundle().apply { putLong("songId", lastState.songId) }
            val metadata = MediaMetadata.Builder()
                .setTitle(lastState.title)
                .setArtist(lastState.artist)
                .setArtworkUri(Uri.parse(lastState.coverUrl))
                .setExtras(bundle)
                .build()
            
            val mediaItem = MediaItem.Builder()
                .setMediaId(lastState.songId.toString())
                .setMediaMetadata(metadata)
                .build()
            
            _currentTrack.value = mediaItem
            _currentPosition.value = lastState.lastPositionMs
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, LinMusicPlaybackService::class.java)
        )

        controller = suspendCancellableCoroutine { continuation ->
            val factory = MediaController.Builder(context, sessionToken).buildAsync()
            factory.addListener(
                {
                    val mediaController = factory.get()
                    mediaController.addListener(this@PlayerManager)
                    
                    // 同步服务端的实时状态（如果服务已在运行）
                    if (mediaController.currentMediaItem != null) {
                        _currentTrack.value = mediaController.currentMediaItem
                        _isPlaying.value = mediaController.isPlaying
                        _currentPosition.value = mediaController.currentPosition
                        _duration.value = mediaController.duration
                    }
                    
                    continuation.resume(mediaController)
                },
                ContextCompat.getMainExecutor(context)
            )
            
            continuation.invokeOnCancellation {
                factory.cancel(true)
            }
        }
    }

    fun playAudio(songId: Long, url: String, title: String, artist: String, coverUrl: String, startPosition: Long = 0) {
        val bundle = Bundle().apply {
            putLong("songId", songId)
        }
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(coverUrl))
            .setExtras(bundle)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(songId.toString())
            .setMediaMetadata(mediaMetadata)
            .build()

        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (startPosition > 0) {
                seekTo(startPosition)
            }
            play()
        }
    }

    fun pause() {
        controller?.pause()
        saveState()
    }

    fun resume() {
        controller?.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun saveState() {
        val item = _currentTrack.value ?: return
        val songId = item.mediaMetadata.extras?.getLong("songId") ?: -1L
        if (songId == -1L) return

        scope.launch {
            playbackPreferences.savePlaybackState(
                PlaybackState(
                    songId = songId,
                    title = item.mediaMetadata.title?.toString() ?: "",
                    artist = item.mediaMetadata.artist?.toString() ?: "",
                    coverUrl = item.mediaMetadata.artworkUri?.toString() ?: "",
                    lastPositionMs = controller?.currentPosition ?: _currentPosition.value
                )
            )
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (!isPlaying) {
            saveState()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _currentTrack.value = mediaItem
        if (mediaItem != null) {
            _duration.value = controller?.duration ?: 0L
            saveState()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            _duration.value = controller?.duration ?: 0L
        }
    }
}
