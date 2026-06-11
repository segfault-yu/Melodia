package com.lin0721.linmusic.player

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lin0721.linmusic.data.local.SettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MelodiaPlaybackService : MediaSessionService() {

    private val playerManager: PlayerManager by inject()
    private val settingsPreferences: SettingsPreferences by inject()
    private var player: Player? = null
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        // 允许跨协议重定向（如 HTTPS 到 HTTP）
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val defaultDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // 动态代理数据源，每次请求新数据源时获取最新配置并创建对应的源
        val dynamicDataSourceFactory = androidx.media3.datasource.DataSource.Factory {
            val isCacheEnabled = runBlocking {
                settingsPreferences.streamCacheEnabled.first()
            }
            if (isCacheEnabled) {
                val maxSize = runBlocking {
                    settingsPreferences.audioCacheMaxSize.first()
                }
                val cache = AudioCacheManager.getCache(this@MelodiaPlaybackService, maxSize)
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource()
            } else {
                defaultDataSourceFactory.createDataSource()
            }
        }

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dynamicDataSourceFactory))
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


