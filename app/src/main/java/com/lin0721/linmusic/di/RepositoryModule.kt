package com.lin0721.linmusic.di

import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.data.repository.MusicRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin 数据仓库层依赖注入模块
 */
val repositoryModule = module {

    // 自动通过构造函数注入 NeteaseApiService，并将 MusicRepositoryImpl 绑定到 MusicRepository 接口
    singleOf(::MusicRepositoryImpl) { bind<MusicRepository>() }

}
