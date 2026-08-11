package com.lin0721.linmusic.feature.playlist.data

import kotlinx.coroutines.flow.Flow

// 歌单/专辑/红心数据仓储（playlist 业务域）
interface PlaylistRepository {

    // 获取歌单详情
    fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>>

    // 获取专辑详情，映射至统一领域模型 PlaylistDetail
    fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>>

    // 收藏/取消收藏歌单
    fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // 歌单歌曲添加/删除操作
    fun manipulatePlaylistTracks(op: String, playlistId: Long, trackId: Long): Flow<Result<Unit>>

    fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>>

    fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>>
}
