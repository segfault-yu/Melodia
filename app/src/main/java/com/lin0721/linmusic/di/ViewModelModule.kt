package com.lin0721.linmusic.di

import com.lin0721.linmusic.ui.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin ViewModel 层依赖注入模块
 *
 * 使用 [viewModelOf] 委托自动解析 HomeViewModel 的构造参数（MusicRepository）。
 */
val viewModelModule = module {

    viewModelOf(::HomeViewModel)

}
