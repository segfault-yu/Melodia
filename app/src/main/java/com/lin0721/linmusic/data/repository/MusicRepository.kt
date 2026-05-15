package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.PersonalizedData
import com.lin0721.linmusic.data.remote.api.Artist
import com.lin0721.linmusic.data.remote.api.DailySong
import com.lin0721.linmusic.ui.home.HomeFeedPage
import kotlinx.coroutines.flow.Flow

// 音乐数据层接口
interface MusicRepository {

    // 获取首页动态内容 (支持分页)
    fun getHomepageBlocks(refresh: Boolean = true, cursor: String? = null): Flow<Result<HomeFeedPage>>

    // 获取个性化推荐歌单（公开接口，无需登录）
    fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>>

    // 获取歌单详情
    fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>>

    // 获取歌曲播放链接
    fun getSongUrl(songId: Long): Flow<Result<String>>

    // 获取热门歌手
    fun getTopArtists(): Flow<Result<List<Artist>>>

    // 获取当前登录账号信息
    fun getAccountInfo(): Flow<Result<com.lin0721.linmusic.data.remote.api.AccountInfoResponse>>

    // 获取最近播放歌单
    fun getRecentPlaylists(): Flow<Result<List<com.lin0721.linmusic.data.remote.api.RecentPlayItem>>>

    // ================== 每日推荐 ==================

    // 获取每日推荐歌曲（需登录）
    fun getDailyRecommendSongs(): Flow<Result<List<DailySong>>>

    // 获取历史日推可用日期列表（黑胶 VIP）
    fun getHistoryRecommendDates(): Flow<Result<List<String>>>

    // 获取指定日期的历史日推详情
    fun getHistoryRecommendDetail(date: String): Flow<Result<List<DailySong>>>
}
