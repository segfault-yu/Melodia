package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.api.Track
import com.lin0721.linmusic.feature.player.domain.LyricLine
import com.lin0721.linmusic.feature.player.domain.SongWikiData
import kotlinx.coroutines.flow.Flow

// 播放数据仓储（player 业务域，同时服务于 PlayerViewModel/PlayerManager/FloatingLyricService）
interface PlayerRepository {

    // 获取歌曲播放链接
    fun getSongUrl(songId: Long): Flow<Result<String>>

    // 获取歌曲歌词（已解析 LRC 格式）
    fun getLyrics(songId: Long): Flow<Result<List<LyricLine>>>

    fun getSongDetail(songId: Long): Flow<Result<Track>>

    fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>>

    fun getIntelligenceSongs(songId: Long, playlistId: Long): Flow<Result<List<Track>>>

    // 获取合并后的歌曲详情与百科信息
    fun getSongWiki(songId: Long): Flow<Result<SongWikiData>>
}
