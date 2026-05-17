package com.lin0721.linmusic.di

import com.lin0721.linmusic.ui.home.HomeViewModel
import com.lin0721.linmusic.ui.library.LibraryViewModel
import com.lin0721.linmusic.ui.playlist.PlaylistViewModel
import com.lin0721.linmusic.ui.search.SearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin ViewModel 层依赖注入模块
 *
 * 使用 [viewModelOf] 委托自动解析 ViewModel 的构造参数。
 */
val viewModelModule = module {

    viewModelOf(::HomeViewModel)
    viewModelOf(::PlaylistViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::LibraryViewModel)

}
