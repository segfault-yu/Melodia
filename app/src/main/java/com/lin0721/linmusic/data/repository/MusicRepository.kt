package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.core.api.PlaylistDetail
import kotlinx.coroutines.flow.Flow

// 音乐数据层接口
interface MusicRepository {

    // 创建歌单
    fun createPlaylist(name: String, privacy: Int = 0): Flow<Result<PlaylistDetail>>
}
