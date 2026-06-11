package com.lin0721.linmusic.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object AudioCacheManager {
    private var cache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context, maxSize: Long): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "audio_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(maxSize)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }

    @Synchronized
    fun recreateCache(context: Context, newMaxSize: Long) {
        try {
            cache?.release()
            cache = null
            getCache(context, newMaxSize)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun clearCache(context: Context) {
        try {
            cache?.release()
            cache = null
            val cacheDir = File(context.cacheDir, "audio_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
