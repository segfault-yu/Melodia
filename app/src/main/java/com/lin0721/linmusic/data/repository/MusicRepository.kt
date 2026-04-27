package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.PersonalizedData
import com.lin0721.linmusic.data.remote.api.Artist
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据层接口
 */
interface MusicRepository {

    /**
     * 获取个性化推荐歌单（公开接口，无需登录）
     */
    fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>>

    /**
     * 获取歌单详情
     */
    fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>>

    /**
     * 获取歌曲播放链接
     */
    fun getSongUrl(songId: Long): Flow<Result<String>>

    /**
     * 获取热门歌手
     */
    fun getTopArtists(): Flow<Result<List<Artist>>>
}
