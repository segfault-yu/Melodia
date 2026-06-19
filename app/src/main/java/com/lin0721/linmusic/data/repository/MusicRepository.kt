package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.PersonalizedData
import com.lin0721.linmusic.data.remote.api.ArtistAlbum
import com.lin0721.linmusic.data.remote.api.ArtistDetailInfo
import com.lin0721.linmusic.data.remote.api.Artist
import com.lin0721.linmusic.data.remote.api.DailySong
import com.lin0721.linmusic.data.remote.api.Track
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

// 单个字符/单词的耗时元数据
data class WordInfo(
    val text: String,
    val startOffsetMs: Long,  // 相对于整行歌词起始时间的偏移毫秒数
    val durationMs: Long      // 该字/词的持续发音毫秒数
)

// 歌词行领域模型
data class LyricLine(
    val timeMs: Long,
    val durationMs: Long = 0, // 新增：整行歌词的持续发音时间
    val text: String,
    val translation: String? = null,
    val words: List<WordInfo> = emptyList() // 如果是普通LRC则此列表为空；YRC则填入单字列表
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

    // 获取专辑详情，映射至统一领域模型 PlaylistDetail
    fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>>

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

    // 获取历史日推可用日期列表（VIP）
    fun getHistoryRecommendDates(): Flow<Result<List<String>>>

    // 获取指定日期的历史日推详情
    fun getHistoryRecommendDetail(date: String): Flow<Result<List<DailySong>>>

    // 获取排行榜详情（DTO 映射至领域模型）
    fun getToplistDetail(): Flow<Result<List<ToplistInfo>>>

    // ================== 音乐库 ==================
    // 获取用户歌单
    fun getUserPlaylists(uid: Long, limit: Int = 1000): Flow<Result<List<com.lin0721.linmusic.data.remote.api.UserPlaylist>>>

    // 获取听歌排行
    fun getUserRecord(uid: Long, type: Int): Flow<Result<List<Track>>>

    // 获取收藏专辑
    fun getCollectedAlbums(limit: Int = 1000): Flow<Result<List<com.lin0721.linmusic.data.remote.api.AlbumSubItem>>>

    // 获取各分类收藏数
    fun getUserSubcount(): Flow<Result<com.lin0721.linmusic.data.remote.api.UserSubcountResponse>>

    // 创建歌单
    fun createPlaylist(name: String, privacy: Int = 0): Flow<Result<PlaylistDetail>>

    // ================== 歌词 ==================

    // 获取歌曲歌词（已解析 LRC 格式）
    fun getLyrics(songId: Long): Flow<Result<List<LyricLine>>>

    // ================== 歌曲详情 ==================

    fun getSongDetail(songId: Long): Flow<Result<Track>>

    // ================== 相似歌手 ==================

    fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>>

    // ================== 相似歌曲 ==================

    fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>>

    // ================== 智能推荐 ==================

    fun getIntelligenceSongs(songId: Long, playlistId: Long): Flow<Result<List<Track>>>

    // ================== 艺人详情 ==================

    fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>>

    fun getArtistAlbums(artistId: Long, limit: Int = 10): Flow<Result<List<ArtistAlbum>>>

    // 获取艺人粉丝数（每月听众数）
    fun getArtistFansCount(artistId: Long): Flow<Result<Long>>

    // 获取艺人热门歌曲（50首）
    fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>>

    // 收藏/关注歌手
    fun subscribeArtist(artistId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // 检查是否已关注歌手
    fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>>

    // ================== 红心/喜欢 ==================

    fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>>

    fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>>

    // 歌单歌曲添加/删除操作
    fun manipulatePlaylistTracks(op: String, playlistId: Long, trackId: Long): Flow<Result<Unit>>

    // 收藏/取消收藏歌单
    fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>>


    // ================== 评论 ==================

    fun getComments(songId: Long, limit: Int = 20, offset: Int = 0): Flow<Result<com.lin0721.linmusic.data.remote.api.CommentsResponse>>

    // 获取通用资源的评论 (例如歌单 A_PL_0_ID)
    fun getComments(threadId: String, limit: Int = 20, offset: Int = 0): Flow<Result<com.lin0721.linmusic.data.remote.api.CommentsResponse>>

    // 评论点赞/取消点赞
    fun likeComment(threadId: String, commentId: Long, like: Boolean): Flow<Result<Unit>>


    // ================== 百科与乐谱详情 ==================
    // 获取合并后的歌曲详情与百科信息
    fun getSongWiki(songId: Long): Flow<Result<SongWikiData>>

    // ================== 设置和隐私扩展 ==================
    // 获取用户等级信息
    fun getUserLevel(): Flow<Result<com.lin0721.linmusic.data.remote.api.UserLevelData>>

    // 获取 VIP 状态信息
    fun getVipInfo(): Flow<Result<com.lin0721.linmusic.data.remote.api.VipInfoData>>

    // 获取账号绑定信息
    fun getUserBindings(uid: Long): Flow<Result<List<com.lin0721.linmusic.data.remote.api.UserBindingItem>>>

    // 修改用户个人资料
    fun updateUserProfile(nickname: String, gender: Int, birthday: Long, province: Int, city: Int, signature: String): Flow<Result<Unit>>

    // 检查昵称可用性
    fun checkNickname(nickname: String): Flow<Result<Boolean>>

    // 每日签到
    fun dailySignin(type: Int): Flow<Result<Int>> // 返回签到获得的积分数

    // 退出登录并清理网络会话
    fun logout(): Flow<Result<Unit>>

    // 上传并更换头像
    fun uploadAvatar(file: java.io.File): Flow<Result<String>>
}

// 歌曲详情/百科信息领域模型
data class SongWikiData(
    val style: String = "",
    val album: String = "", 
    val language: String = "",      
    val publishTime: String = "",   
    val bpm: String = "",           
    val creators: String = "",      
    val entertainment: String = "", 
    val background: String = "",    
    val awards: String = ""         
)
