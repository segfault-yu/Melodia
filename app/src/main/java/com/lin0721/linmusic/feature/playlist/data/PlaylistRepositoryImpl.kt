package com.lin0721.linmusic.feature.playlist.data

import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow

class PlaylistRepositoryImpl(
    private val apiService: PlaylistApi,
    private val contentFilter: ContentFilter
) : PlaylistRepository {

    override fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>> = apiFlow(
        request = { apiService.getPlaylistDetail(PlaylistDetailRequest(id = id)) },
        isSuccess = { it.isSuccess && it.playlist != null },
        code = { it.code },
        transform = { response ->
            val filteredTracks = contentFilter.filterBlockedArtists(response.playlist!!.tracks) { it.ar.map { a -> a.id } }
            response.playlist.copy(tracks = filteredTracks)
        }
    )

    override fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>> = apiFlow(
        // 专辑 ID 需作为 URL 路径参数传入，不使用 AlbumDetailRequest 请求体
        request = { apiService.getAlbumDetail(id = id) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { response ->
            val album = response.album
            val filteredTracks = contentFilter.filterBlockedArtists(response.songs) { it.ar.map { a -> a.id } }
            PlaylistDetail(
                id = album.id,
                name = album.name,
                coverImgUrl = album.picUrl,
                description = album.description,
                playCount = 0L,
                tracks = filteredTracks
            )
        }
    )

    override fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>> = apiFlow(
        request = {
            apiService.subscribePlaylist(
                op = if (subscribe) "subscribe" else "unsubscribe",
                body = PlaylistSubscribeRequest(id = playlistId)
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { Unit }
    )

    override fun manipulatePlaylistTracks(op: String, playlistId: Long, trackId: Long): Flow<Result<Unit>> = apiFlow(
        request = {
            apiService.manipulatePlaylistTracks(
                PlaylistTracksManipulateRequest(
                    op = op,
                    pid = playlistId,
                    trackIds = "[\"$trackId\"]"
                )
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        msg = { it.message },
        transform = { Unit }
    )

    override fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>> = apiFlow(
        request = { apiService.getLikedSongIds(LikeSongListRequest(uid = uid)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.ids }
    )

    override fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>> = apiFlow(
        request = { apiService.likeSong(LikeSongRequest(trackId = songId, like = like)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { Unit }
    )
}
