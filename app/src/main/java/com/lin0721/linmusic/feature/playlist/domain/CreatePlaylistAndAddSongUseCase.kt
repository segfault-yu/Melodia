package com.lin0721.linmusic.feature.playlist.domain

import com.lin0721.linmusic.core.api.PlaylistDetail
import com.lin0721.linmusic.feature.create.data.CreateRepository
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 新建歌单并立即把指定歌曲加入其中，供 artist/library/playlist 等域共同复用
class CreatePlaylistAndAddSongUseCase(
    private val createRepository: CreateRepository,
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(name: String, songId: Long, privacy: Int = 0): Flow<Result<PlaylistDetail>> = flow {
        createRepository.createPlaylist(name, privacy).collect { createResult ->
            createResult.fold(
                onSuccess = { playlist ->
                    playlistRepository.manipulatePlaylistTracks("add", playlist.id, songId).collect { addResult ->
                        addResult.fold(
                            onSuccess = { emit(Result.success(playlist)) },
                            onFailure = { e -> emit(Result.failure(Exception("加入新建歌单失败: ${e.message}"))) }
                        )
                    }
                },
                onFailure = { e -> emit(Result.failure(Exception("创建歌单失败: ${e.message}"))) }
            )
        }
    }
}
