package com.lin0721.linmusic.di

import com.lin0721.linmusic.ui.settings.SettingsViewModel
import com.lin0721.linmusic.ui.create.CreateViewModel
import com.lin0721.linmusic.feature.home.ui.HomeViewModel
import com.lin0721.linmusic.feature.library.ui.LibraryViewModel
import com.lin0721.linmusic.ui.player.PlayerViewModel
import com.lin0721.linmusic.ui.playlist.PlaylistViewModel
import com.lin0721.linmusic.ui.artist.ArtistViewModel
import com.lin0721.linmusic.feature.search.ui.SearchViewModel
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
    viewModelOf(::ArtistViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::CreateViewModel)
    viewModelOf(::PlayerViewModel)
    viewModelOf(::SettingsViewModel)

}
