package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.RecommendPlaylistData
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据层接口
 */
interface MusicRepository {

    /**
     * 获取每日推荐歌单
     */
    fun getDailyRecommendPlaylists(): Flow<Result<RecommendPlaylistData>>

}
