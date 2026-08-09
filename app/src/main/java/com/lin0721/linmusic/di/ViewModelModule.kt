package com.lin0721.linmusic.di

import com.lin0721.linmusic.feature.settings.ui.SettingsViewModel
import com.lin0721.linmusic.feature.create.ui.CreateViewModel
import com.lin0721.linmusic.feature.home.ui.HomeViewModel
import com.lin0721.linmusic.feature.library.ui.LibraryViewModel
import com.lin0721.linmusic.feature.player.ui.PlayerViewModel
import com.lin0721.linmusic.feature.playlist.ui.PlaylistViewModel
import com.lin0721.linmusic.feature.artist.ui.ArtistViewModel
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
