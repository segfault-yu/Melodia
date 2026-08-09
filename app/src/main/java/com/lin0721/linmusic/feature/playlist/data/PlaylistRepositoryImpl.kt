package com.lin0721.linmusic.feature.playlist.data

import com.lin0721.linmusic.core.api.LikeSongListRequest
import com.lin0721.linmusic.core.api.LikeSongRequest
import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.api.PlaylistDetail
import com.lin0721.linmusic.core.api.PlaylistDetailRequest
import com.lin0721.linmusic.core.api.PlaylistSubscribeRequest
import com.lin0721.linmusic.core.api.PlaylistTracksManipulateRequest
import com.lin0721.linmusic.core.contentfilter.ContentFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class PlaylistRepositoryImpl(
    private val apiService: NeteaseApiService,
    private val contentFilter: ContentFilter
) : PlaylistRepository {

    override fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>> = flow {
        val response = apiService.getPlaylistDetail(PlaylistDetailRequest(id = id))
        if (response.isSuccess && response.playlist != null) {
            val filteredTracks = contentFilter.filterBlockedArtists(response.playlist.tracks) { it.ar.map { a -> a.id } }
            emit(Result.success(response.playlist.copy(tracks = filteredTracks)))
        } else {
            emit(Result.failure(Exception("Failed to load playlist detail: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>> = flow {
        // 专辑 ID 需作为 URL 路径参数传入，不使用 AlbumDetailRequest 请求体
        val response = apiService.getAlbumDetail(id = id)
        if (response.isSuccess) {
            val album = response.album
            val filteredTracks = contentFilter.filterBlockedArtists(response.songs) { it.ar.map { a -> a.id } }
            val detail = PlaylistDetail(
                id = album.id,
                name = album.name,
                coverImgUrl = album.picUrl,
                description = album.description,
                playCount = 0L,
                tracks = filteredTracks
            )
            emit(Result.success(detail))
        } else {
            emit(Result.failure(Exception("Failed to load album detail: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>> = flow {
        val op = if (subscribe) "subscribe" else "unsubscribe"
        val response = apiService.subscribePlaylist(
            op = op,
            body = PlaylistSubscribeRequest(id = playlistId)
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("操作失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun manipulatePlaylistTracks(op: String, playlistId: Long, trackId: Long): Flow<Result<Unit>> = flow {
        val response = apiService.manipulatePlaylistTracks(
            PlaylistTracksManipulateRequest(
                op = op,
                pid = playlistId,
                trackIds = "[\"$trackId\"]"
            )
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Failed to manipulate tracks: code ${response.code}, message ${response.message}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>> = flow {
        val response = apiService.getLikedSongIds(LikeSongListRequest(uid = uid))
        if (response.isSuccess) {
            emit(Result.success(response.ids))
        } else {
            emit(Result.failure(Exception("Failed to load liked songs: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>> = flow {
        val response = apiService.likeSong(LikeSongRequest(trackId = songId, like = like))
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("Failed to like song: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}
