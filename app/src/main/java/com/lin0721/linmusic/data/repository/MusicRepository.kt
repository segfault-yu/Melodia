package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.PersonalizedData
import com.lin0721.linmusic.data.remote.api.Artist
import com.lin0721.linmusic.data.remote.api.DailySong
import com.lin0721.linmusic.ui.home.HomeFeedPage
import kotlinx.coroutines.flow.Flow

// 排行榜领域模型（UI 层直接使用，与 DTO 解耦）
data class ToplistInfo(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val updateDesc: String,
    // 前三首格式："歌名 - 歌手"
    val topSongs: List<String>
)

// 歌手领域模型
data class ArtistInfo(
    val id: Long,
    val name: String,
    val avatarUrl: String
)

// 搜索结果领域模型
data class SearchSongsResult(
    val songs: List<com.lin0721.linmusic.data.remote.api.SearchSong>,
    val totalCount: Int,
    val hasMore: Boolean
)

// 热搜领域模型
data class HotSearch(
    val keyword: String,
    val score: Int,
    val description: String = "",
    val iconUrl: String? = null
)

// 歌单标签领域模型
data class PlaylistTag(
    val name: String,
    val coverUrl: String = ""
)

// 音乐数据层接口
interface MusicRepository {

    // ================== 发现/搜索 ==================
    // 获取默认搜索词
    fun getDefaultSearchKeyword(): Flow<Result<String>>

    // 云搜索歌曲
    fun searchSongs(keyword: String, offset: Int = 0, limit: Int = 30): Flow<Result<SearchSongsResult>>

    // 获取热搜榜
    fun getHotSearches(): Flow<Result<List<HotSearch>>>

    // 获取精品歌单标签（含封面图）
    fun getPlaylistTags(): Flow<Result<List<PlaylistTag>>>

    // 获取发现页（Search Screen）原始区块结构
    fun getDiscoveryBlocks(refresh: Boolean = true, cursor: String? = null): Flow<Result<List<com.lin0721.linmusic.data.remote.api.HomepageBlock>>>

    // ================== 首页内容 ==================
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

    // 获取最爱的歌手（优先已关注，兜底热门）
    fun getFavoriteArtists(): Flow<Result<List<ArtistInfo>>>

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

    // 获取排行榜详情（DTO 映射至领域模型）
    fun getToplistDetail(): Flow<Result<List<ToplistInfo>>>

    // ================== 音乐库 (Library) ==================
    // 获取用户歌单
    fun getUserPlaylists(uid: Long, limit: Int = 1000): Flow<Result<List<com.lin0721.linmusic.data.remote.api.UserPlaylist>>>

    // 获取收藏专辑
    fun getCollectedAlbums(limit: Int = 1000): Flow<Result<List<com.lin0721.linmusic.data.remote.api.AlbumSubItem>>>

    // 获取各分类收藏数
    fun getUserSubcount(): Flow<Result<com.lin0721.linmusic.data.remote.api.UserSubcountResponse>>

    // 创建歌单
    fun createPlaylist(name: String, privacy: Int = 0): Flow<Result<PlaylistDetail>>
}
