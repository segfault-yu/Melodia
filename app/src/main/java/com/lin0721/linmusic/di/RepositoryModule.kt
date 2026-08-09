package com.lin0721.linmusic.di

import com.lin0721.linmusic.core.auth.AuthRepository
import com.lin0721.linmusic.core.auth.AuthRepositoryImpl
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.data.repository.MusicRepositoryImpl
import com.lin0721.linmusic.feature.home.data.HomeRepository
import com.lin0721.linmusic.feature.home.data.HomeRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin 数据仓库层依赖注入模块
 */
val repositoryModule = module {

    // 自动通过构造函数注入 NeteaseApiService，并将 MusicRepositoryImpl 绑定到 MusicRepository 接口
    singleOf(::MusicRepositoryImpl) { bind<MusicRepository>() }

    // 登录态与账号信息（core/auth，跨业务域共享）
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }

    // 首页数据仓储（feature/home）
    singleOf(::HomeRepositoryImpl) { bind<HomeRepository>() }

}
