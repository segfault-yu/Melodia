package com.lin0721.linmusic

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lin0721.linmusic.di.networkModule
import com.lin0721.linmusic.di.repositoryModule
import com.lin0721.linmusic.di.viewModelModule
import com.lin0721.linmusic.di.playerModule
import com.lin0721.linmusic.di.localModule

import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LinMusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 提前在后台线程初始化 Coil，避免首次渲染时在主线程触发 DiskLruCache.initialize() 造成卡顿
        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.15) // 占堆内存 15%
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02) // 占磁盘 2%，约 50MB
                        .build()
                }
                .crossfade(true)   // 全局启用淡入，避免图片突变造成视觉跳变
                .build()
        }

        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@LinMusicApplication)
            modules(networkModule, repositoryModule, viewModelModule, playerModule, localModule)
        }
    }
}
