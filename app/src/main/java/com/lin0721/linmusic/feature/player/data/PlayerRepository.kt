package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.feature.player.domain.SongWikiData
import kotlinx.coroutines.flow.Flow

// 播放器详情页数据仓储（player 业务域，仅服务于 PlayerViewModel）
interface PlayerRepository {

    fun getSongDetail(songId: Long): Flow<Result<Track>>

    // 获取合并后的歌曲详情与百科信息
    fun getSongWiki(songId: Long): Flow<Result<SongWikiData>>
}
