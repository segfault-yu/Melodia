package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.PersonalizedData
import kotlinx.coroutines.flow.Flow

/**
 * 音乐数据层接口
 */
interface MusicRepository {

    /**
     * 获取个性化推荐歌单（公开接口，无需登录）
     */
    fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>>

}
