package com.lin0721.linmusic.player

import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.koin.android.ext.android.inject

class MelodiaPlaybackService : MediaSessionService() {

    private val playerManager: PlayerManager by inject()
    private var player: Player? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        
        // 允许跨协议重定向（如 HTTPS 到 HTTP）
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            
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

