package com.lin0721.linmusic.feature.playlist.domain

import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.network.ResourceProvider
import com.lin0721.linmusic.core.network.toUserMessage
import com.lin0721.linmusic.core.songlike.SongLikeRepository
import com.lin0721.linmusic.core.ui.components.PlaylistCollectItem
import com.lin0721.linmusic.core.ui.components.PlaylistCollectState
import com.lin0721.linmusic.core.userplaylist.UserPlaylistRepository
import com.lin0721.linmusic.feature.playlist.data.PlaylistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update

// “收藏到歌单”弹窗的共享状态与操作，由 artist/playlist 等域各自持有一个实例
class SongCollectDelegate(
    private val userPlaylistRepository: UserPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val songLikeRepository: SongLikeRepository,
    private val createPlaylistAndAddSongUseCase: CreatePlaylistAndAddSongUseCase,
    private val userPreferences: UserPreferences,
    private val resourceProvider: ResourceProvider
) {

    private val _state = MutableStateFlow(PlaylistCollectState())
    val state: StateFlow<PlaylistCollectState> = _state.asStateFlow()

    // 拉取当前用户的歌单并逐个判定是否已包含该歌曲；“我喜欢的音乐”直接查红心集合，避免多余请求
    suspend fun prepare(songId: Long, likedSongIds: Set<Long>, onToast: suspend (String) -> Unit) {
        val profile = userPreferences.userProfile.first() ?: return
        _state.update { it.copy(songId = songId, isLoading = true, collectItems = emptyList()) }

        val result = userPlaylistRepository.getUserPlaylists(profile.uid).firstOrNull() ?: return
        result.onSuccess { playlists ->
            val items = coroutineScope {
                playlists.filter { it.userId == profile.uid }.map { playlist ->
                    async {
                        val contains = if (isLikedPlaylist(playlist.name, playlist.id, profile.uid)) {
                            likedSongIds.contains(songId)
                        } else {
                            val detail = playlistRepository.getPlaylistDetail(playlist.id).firstOrNull()?.getOrNull()
                            detail?.tracks?.any { it.id == songId } ?: false
                        }
                        PlaylistCollectItem(
                            playlistId = playlist.id,
                            playlistName = playlist.name,
                            coverUrl = playlist.coverImgUrl,
                            isInitiallyContains = contains,
                            isContains = contains
                        )
                    }
                }.awaitAll()
            }
            _state.update { it.copy(collectItems = items, isLoading = false) }
        }.onFailure {
            _state.update { it.copy(isLoading = false) }
            onToast(it.toUserMessage(resourceProvider))
        }
    }

    // 提交勾选变更；红心歌单走红心接口并在内存回写红心集合，其余走歌单增删
    suspend fun save(
        songId: Long,
        items: List<PlaylistCollectItem>,
        likedSongIds: Set<Long>,
        onToast: suspend (String) -> Unit,
        onLikedChanged: (Set<Long>) -> Unit
    ) {
        val profile = userPreferences.userProfile.first()
        var liked = likedSongIds

        items.forEach { item ->
            if (item.isContains == item.isInitiallyContains) return@forEach

            if (profile != null && isLikedPlaylist(item.playlistName, item.playlistId, profile.uid)) {
                songLikeRepository.likeSong(songId, item.isContains).firstOrNull()
                    ?.onSuccess {
                        liked = if (item.isContains) liked + songId else liked - songId
                        onLikedChanged(liked)
                    }
                    ?.onFailure { onToast(it.toUserMessage(resourceProvider)) }
            } else {
                val op = if (item.isContains) "add" else "del"
                playlistRepository.manipulatePlaylistTracks(op, item.playlistId, songId).firstOrNull()
                    ?.onFailure { onToast(it.toUserMessage(resourceProvider)) }
            }
        }
        onToast("歌单收藏更新成功")
    }

    // 新建歌单并把歌曲加入，成功后重新拉取弹窗列表以带出新歌单
    suspend fun createAndAdd(
        name: String,
        songId: Long,
        likedSongIds: Set<Long>,
        onToast: suspend (String) -> Unit
    ) {
        createPlaylistAndAddSongUseCase(name, songId).firstOrNull()
            ?.onSuccess {
                onToast("创建并加入歌单成功")
                prepare(songId, likedSongIds, onToast)
            }
            ?.onFailure { onToast(it.toUserMessage(resourceProvider)) }
    }

    // 网易云的“我喜欢的音乐”歌单 ID 等于用户 uid，部分场景只能靠歌单名判断
    private fun isLikedPlaylist(name: String, playlistId: Long, uid: Long): Boolean =
        name.contains("喜欢的音乐") || playlistId == uid
}
