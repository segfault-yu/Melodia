package com.lin0721.linmusic.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.koin.android.ext.android.inject

class MelodiaPlaybackService : MediaSessionService() {

    private val playerManager: PlayerManager by inject()
    private var player: Player? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        playerManager.saveState()
        mediaSession?.run {

            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
