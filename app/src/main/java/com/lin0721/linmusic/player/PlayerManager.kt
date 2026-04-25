package com.lin0721.linmusic.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PlayerManager(private val context: Context) : Player.Listener {

    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    suspend fun initController() {
        if (controller != null) return

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
                    continuation.resume(mediaController)
                },
                ContextCompat.getMainExecutor(context)
            )
            
            continuation.invokeOnCancellation {
                factory.cancel(true)
            }
        }
    }

    fun playAudio(url: String, title: String, artist: String, coverUrl: String) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(coverUrl))
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(url) // Use URL as a simple unique ID for now
            .setMediaMetadata(mediaMetadata)
            .build()

        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun resume() {
        controller?.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        _currentTrack.value = mediaItem
    }
}
