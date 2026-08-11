package com.lin0721.linmusic.feature.library.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.coroutines.flow.Flow

// 音乐库数据仓储（library 业务域）
interface LibraryRepository {

    // 获取用户歌单
    fun getUserPlaylists(uid: Long, limit: Int = 1000): Flow<Result<List<UserPlaylist>>>

    // 获取听歌排行
    fun getUserRecord(uid: Long, type: Int): Flow<Result<List<Track>>>

    // 获取收藏专辑
    fun getCollectedAlbums(limit: Int = 1000): Flow<Result<List<AlbumSubItem>>>

    // 获取各分类收藏数
    fun getUserSubcount(): Flow<Result<UserSubcountResponse>>
}
