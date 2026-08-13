package com.lin0721.linmusic.di

import com.lin0721.linmusic.core.auth.AuthRepository
import com.lin0721.linmusic.core.auth.AuthRepositoryImpl
import com.lin0721.linmusic.feature.create.data.CreateRepository
import com.lin0721.linmusic.feature.create.data.CreateRepositoryImpl
import com.lin0721.linmusic.feature.home.data.HomeRepository
import com.lin0721.linmusic.feature.home.data.HomeRepositoryImpl
import com.lin0721.linmusic.feature.artist.data.ArtistRepository
import com.lin0721.linmusic.feature.artist.data.ArtistRepositoryImpl
import com.lin0721.linmusic.core.comment.data.CommentRepository
import com.lin0721.linmusic.core.comment.data.CommentRepositoryImpl
import com.lin0721.linmusic.core.songlike.SongLikeRepository
import com.lin0721.linmusic.core.songlike.SongLikeRepositoryImpl
import com.lin0721.linmusic.feature.library.data.LibraryRepository
import com.lin0721.linmusic.feature.library.data.LibraryRepositoryImpl
import com.lin0721.linmusic.feature.player.data.PlayerRepository
import com.lin0721.linmusic.feature.player.data.PlayerRepositoryImpl
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository
import com.lin0721.linmusic.feature.playlist.domain.CreatePlaylistAndAddSongUseCase
import com.lin0721.linmusic.feature.settings.data.SettingsRepository
import com.lin0721.linmusic.feature.settings.data.SettingsRepositoryImpl
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepositoryImpl
import com.lin0721.linmusic.feature.search.data.SearchRepository
import com.lin0721.linmusic.feature.search.data.SearchRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin 数据仓库层依赖注入模块
 */
val repositoryModule = module {

    // 登录态与账号信息（core/auth，跨业务域共享）
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }

    // 首页数据仓储（feature/home）
    singleOf(::HomeRepositoryImpl) { bind<HomeRepository>() }

    // 搜索数据仓储（feature/search）
    singleOf(::SearchRepositoryImpl) { bind<SearchRepository>() }

    // 音乐库数据仓储（feature/library）
    singleOf(::LibraryRepositoryImpl) { bind<LibraryRepository>() }

    // 歌手数据仓储（feature/artist）
    singleOf(::ArtistRepositoryImpl) { bind<ArtistRepository>() }

    // 评论数据仓储（core/comment 共享能力）
    singleOf(::CommentRepositoryImpl) { bind<CommentRepository>() }

    // 歌曲红心数据仓储（core/songlike 共享能力）
    singleOf(::SongLikeRepositoryImpl) { bind<SongLikeRepository>() }

    // 歌单/专辑数据仓储（feature/playlist）
    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }

    // 播放数据仓储（feature/player）
    singleOf(::PlayerRepositoryImpl) { bind<PlayerRepository>() }

    // 个人信息/等级/签到数据仓储（feature/settings）
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }

    // 新建歌单数据仓储（feature/create）
    singleOf(::CreateRepositoryImpl) { bind<CreateRepository>() }

    // 新建歌单并加入当前歌曲（跨 artist/library/playlist 域共用）
    singleOf(::CreatePlaylistAndAddSongUseCase)

}
