package com.lin0721.linmusic

import android.app.Application
import com.lin0721.linmusic.di.networkModule
import com.lin0721.linmusic.di.repositoryModule
import com.lin0721.linmusic.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class LinMusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Koin 日志（Debug 构建输出详细日志，Release 仅输出错误）
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)

            // 提供 Android Context
            androidContext(this@LinMusicApplication)

            // 加载模块
            modules(networkModule, repositoryModule, viewModelModule)
        }
    }
}
