package com.lin0721.linmusic.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// 网易云音乐 Retrofit 接口定义。所有 POST 请求会被 [CryptoInterceptor] 自动加密。
interface NeteaseApiService {

    // ===================== 推荐歌单 =====================

    // 获取每日推荐歌单 (需要登录后的 Cookie)
    @POST("/eapi/v1/discovery/recommend/resource")
    suspend fun getDailyRecommendPlaylists(
        @Body body: EmptyBody = EmptyBody()
    ): NeteaseResponse<RecommendPlaylistData>

    // =================== 个性化推荐 ===================

    // 获取个性化推荐歌单（公开接口，无需登录）
    @POST("/eapi/personalized/playlist")
    suspend fun getPersonalizedPlaylists(
        @Body body: PersonalizedRequest = PersonalizedRequest()
    ): PersonalizedResponse

    // ================== 歌单详情 ==================
    @POST("/eapi/v6/playlist/detail")
    suspend fun getPlaylistDetail(
        @Body body: PlaylistDetailRequest
    ): PlaylistDetailResponse

    // ======================= 歌曲信息 =======================

    // 获取歌曲播放链接
    @POST("/eapi/song/enhance/player/url/v1")
    suspend fun getSongUrl(
        @Body body: SongUrlRequest
    ): SongUrlResponse

    // ======================= 歌手相关 =======================

    // 获取已关注歌手 (需登录)
    @POST("/eapi/artist/sublist")
    suspend fun getArtistSublist(
        @Body body: ArtistSublistRequest = ArtistSublistRequest()
    ): ArtistSublistResponse

    // 获取热门歌手
    @POST("/eapi/artist/top")
    suspend fun getTopArtists(
        @Body body: TopArtistsRequest = TopArtistsRequest()
    ): TopArtistsResponse

    // ======================= 用户信息 =======================

    // 获取当前登录账号信息
    @POST("/eapi/nuser/account/get")
    suspend fun getAccountInfo(
        @Body body: EmptyBody = EmptyBody()
    ): AccountInfoResponse

    // 获取首页动态内容 (支持分页)
    @POST("/eapi/homepage/block/page")
    suspend fun getHomepageBlocks(
        @Body body: HomepageBlockRequest
    ): HomepageBlockResponse

    // ================== 搜索 ==================
    // 获取默认搜索词
    @POST("/eapi/search/defaultkeyword/get")
    suspend fun getSearchDefaultKeyword(
        @Body body: EmptyBody = EmptyBody()
    ): SearchDefaultResponse

    // 热搜详情
    @POST("/eapi/hotsearchlist/get")
    suspend fun getHotSearchDetail(
        @Body body: EmptyBody = EmptyBody()
    ): HotSearchDetailResponse

    // 云搜索（综合搜索接口）
    @POST("/eapi/cloudsearch/pc")
    suspend fun cloudSearch(
        @Body body: CloudSearchRequest
    ): CloudSearchResponse

    // ================== 精品歌单标签 ==================

    // 获取精品歌单标签列表
    @POST("/eapi/playlist/highquality/tags")
    suspend fun getHighQualityTags(
        @Body body: EmptyBody = EmptyBody()
    ): HighQualityTagsResponse

    // 获取精品歌单列表（用于获取标签封面图）
    @POST("/eapi/playlist/highquality/list")
    suspend fun getHighQualityPlaylists(
        @Body body: HighQualityPlaylistRequest
    ): HighQualityPlaylistResponse

    // ================== 最近播放 ==================

    // 获取最近播放歌单
    @POST("/eapi/play-record/playlist/list")
    suspend fun getRecentPlaylists(
        @Body body: RecentPlaylistRequest = RecentPlaylistRequest()
    ): RecentPlaylistResponse

    // ================== 每日推荐歌曲 ==================

    // 获取每日推荐歌曲（需登录，每天 06:00 更新）
    @POST("/eapi/v3/discovery/recommend/songs")
    suspend fun getDailyRecommendSongs(
        @Body body: EmptyBody = EmptyBody()
    ): DailyRecommendSongsResponse

    // ================== 历史日推记录 ==================

    // 获取可用的历史日推日期列表（黑胶 VIP 功能）
    @POST("/weapi/discovery/recommend/songs/history/recent")
    suspend fun getHistoryRecommendDates(
        @Body body: EmptyBody = EmptyBody()
    ): HistoryDatesResponse

    // 获取指定日期的历史日推详情
    @POST("/weapi/discovery/recommend/songs/history/detail")
    suspend fun getHistoryRecommendDetail(
        @Body body: HistoryDetailRequest
    ): HistoryDetailResponse

    // ================== 排行榜 ==================

    // 获取排行榜详情（含各榜单前三首歌曲）
    @POST("/eapi/toplist/detail")
    suspend fun getToplistDetail(
        @Body body: EmptyBody = EmptyBody()
    ): ToplistDetailResponse

    // ================== 音乐库 (Library) ==================

    // 获取用户歌单
    @POST("/eapi/user/playlist")
    suspend fun getUserPlaylists(
        @Body body: UserPlaylistRequest
    ): UserPlaylistResponse

    // 获取收藏专辑
    @POST("/eapi/album/sublist")
    suspend fun getAlbumSublist(
        @Body body: AlbumSublistRequest = AlbumSublistRequest()
    ): AlbumSublistResponse

    // 获取用户各分类收藏数
    @POST("/eapi/user/subcount")
    suspend fun getUserSubcount(
        @Body body: EmptyBody = EmptyBody()
    ): UserSubcountResponse

    // 创建歌单
    @POST("/eapi/playlist/create")
    suspend fun createPlaylist(
        @Body body: PlaylistCreateRequest
    ): PlaylistCreateResponse

    // ================== 歌词 ==================

    @POST("/eapi/song/lyric/v1")
    suspend fun getLyrics(
        @Body body: LyricRequest
    ): LyricResponse

    // ================== 歌曲详情 ==================

    @POST("/eapi/v3/song/detail")
    suspend fun getSongDetail(
        @Body body: SongDetailRequest
    ): SongDetailResponse

    // ================== 相似歌手 ==================

    @POST("/eapi/discovery/simiArtist")
    suspend fun getSimiArtists(
        @Body body: SimiArtistRequest
    ): SimiArtistResponse

    // ================== 相似歌曲 ==================

    @POST("/eapi/v1/discovery/simiSong")
    suspend fun getSimiSongs(
        @Body body: SimiSongRequest
    ): SimiSongResponse

    // ================== 艺人详情 ==================

    @POST("/eapi/artist/head/info/get")
    suspend fun getArtistDetail(
        @Body body: ArtistDetailRequest
    ): ArtistDetailResponse

    // ================== 艺人动态信息（粉丝数等） ==================
    @POST("/eapi/artist/detail/dynamic")
    suspend fun getArtistDetailDynamic(
        @Body body: ArtistFollowCountRequest
    ): ArtistFollowCountResponse

    // 使用 WEAPI 接口获取歌手粉丝数量（规避 EAPI 混合 PC Cookie 下被风控返回 0 粉丝的问题）
    @POST("/weapi/artist/follow/count/get")
    suspend fun getArtistFollowCount(
        @Body body: ArtistFollowCountRequest
    ): ArtistFollowCountGetResponse

    // ================== 艺人专辑 ==================

    @POST("/weapi/artist/albums/{id}")
    suspend fun getArtistAlbums(
        @Path("id") id: Long,
        @Body body: ArtistAlbumRequest = ArtistAlbumRequest()
    ): ArtistAlbumResponse

    // ================== 专辑详情 ==================

    // 网易云 WEAPI 专辑详情接口，专辑 ID 需放入 URL 路径中
    @POST("/weapi/v1/album/{id}")
    suspend fun getAlbumDetail(
        @Path("id") id: Long,
        @Body body: EmptyBody = EmptyBody()
    ): AlbumDetailResponse

    // ================== 艺人热门歌曲 ==================

    @POST("/weapi/artist/top/song")
    suspend fun getArtistTopSongs(
        @Body body: ArtistTopSongsRequest
    ): ArtistTopSongsResponse

    // ================== 收藏与取消收藏歌手 ==================

    @POST("/weapi/artist/{op}")
    suspend fun subscribeArtist(
        @Path("op") op: String, // sub 表示关注, unsub 表示取消关注
        @Body body: ArtistSubscriptionRequest
    ): ArtistSubscriptionResponse

    // ================== 红心/喜欢 ==================

    @POST("/eapi/song/like")
    suspend fun likeSong(
        @Body body: LikeSongRequest
    ): LikeSongResponse

    @POST("/eapi/song/like/get")
    suspend fun getLikedSongIds(
        @Body body: LikeSongListRequest
    ): LikeSongListResponse

    // ================== 歌单歌曲操作 (添加/删除) ==================
    @POST("/eapi/playlist/manipulate/tracks")
    suspend fun manipulatePlaylistTracks(
        @Body body: PlaylistTracksManipulateRequest
    ): PlaylistTracksManipulateResponse

    // ================== 百科 (WeApi) ==================

    // 获取歌曲百科简要信息
    @POST("/weapi/song/play/about/block/page")
    suspend fun getSongWikiSummary(
        @Body body: SongWikiSummaryRequest
    ): SongWikiSummaryResponse

    // 获取歌曲创作者信息
    @POST("/weapi/song/creators")
    suspend fun getSongCreators(
        @Body body: SongCreatorsRequest
    ): SongCreatorsResponse

    // ================== 评论 ==================
    @POST("/eapi/v1/resource/comments/{threadId}")
    suspend fun getComments(
        @Path("threadId") threadId: String,
        @Body body: CommentsRequest
    ): CommentsResponse

    // ================== 个人信息、等级与签到（设置与隐私扩展） ==================

    // 获取用户等级信息
    @POST("/eapi/user/level")
    suspend fun getUserLevel(
        @Body body: EmptyBody = EmptyBody()
    ): UserLevelResponse

    // 获取 VIP 状态信息
    @POST("/eapi/vip/info")
    suspend fun getVipInfo(
        @Body body: EmptyBody = EmptyBody()
    ): VipInfoResponse

    // 获取账号绑定信息
    @POST("/eapi/user/binding")
    suspend fun getUserBindings(
        @Body body: UserBindingRequest
    ): UserBindingResponse

    // 修改用户个人资料
    @POST("/eapi/user/update")
    suspend fun updateUserProfile(
        @Body body: UserProfileUpdateRequest
    ): UserProfileUpdateResponse

    // 检查昵称可用性
    @POST("/eapi/nickname/check")
    suspend fun checkNickname(
        @Body body: NicknameCheckRequest
    ): NicknameCheckResponse

    // 每日签到
    @POST("/eapi/point/dailyTask")
    suspend fun dailySignin(
        @Body body: DailySigninRequest
    ): DailySigninResponse

    // 退出登录
    @POST("/eapi/logout")
    suspend fun logoutApi(
        @Body body: EmptyBody = EmptyBody()
    ): LogoutApiResponse

    // 更换头像 (支持 MultipartBody 上传)
    @retrofit2.http.Multipart
    @POST("/weapi/user/avatar/upload/v1")
    suspend fun uploadAvatar(
        @retrofit2.http.Part imgFile: okhttp3.MultipartBody.Part
    ): AvatarUploadResponse

    // ================== 智能推荐歌曲 ==================
    @POST("/eapi/playmode/intelligence/list")
    suspend fun getIntelligenceSongs(
        @Body body: IntelligenceSongsRequest
    ): IntelligenceSongsResponse
}

// 首页动态内容请求体
@Serializable
data class HomepageBlockRequest(
    val cursor: String? = null,
    val refresh: Boolean = false
)

// ======================= 搜索 DTO =======================
@Serializable
data class SearchDefaultResponse(
    val code: Int = 0,
    val data: SearchDefaultData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SearchDefaultData(
    val showKeyword: String = "",
    val realkeyword: String = "",
    val searchType: Int = 0,
    val action: Int = 0
)


// 空请求体，用于不需要参数的 POST 接口
@Serializable
class EmptyBody

// ======================= 响应体 =======================

// 网易云 API 通用响应包装。具体业务数据由泛型 [T] 内联展平。
@Serializable
data class NeteaseResponse<T>(
    val code: Int = 0,
    val data: T? = null,
    val msg: String? = null,
    val message: String? = null,
) {
    /** 请求是否成功 */
    val isSuccess: Boolean get() = code == 200
}

/**
 * 登录响应数据
 */
@Serializable
data class LoginData(
    /** 登录令牌 */
    val token: String? = null,
    /** 用户基本信息 */
    val profile: UserProfile? = null,
    /** Cookie 中的 MUSIC_U 值 (部分情况下接口会直接返回) */
    val cookie: String? = null,
)

@Serializable
data class UserProfile(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
    @SerialName("backgroundUrl")
    val backgroundUrl: String = "",
    val signature: String = "",
)

/**
 * 每日推荐歌单响应数据
 */
@Serializable
data class RecommendPlaylistData(
    /** 推荐歌单列表 */
    val recommend: List<RecommendPlaylist> = emptyList(),
)

@Serializable
data class RecommendPlaylist(
    val id: Long = 0,
    val name: String = "",
    /** 歌单封面 */
    val picUrl: String = "",
    /** 播放次数 */
    val playcount: Long = 0,
    /** 歌曲数量 */
    val trackCount: Int = 0,
    /** 创建者昵称 */
    @SerialName("creator")
    val creator: PlaylistCreator? = null,
)

@Serializable
data class PlaylistCreator(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
)

// ==================== 个性化推荐 ====================

@Serializable
data class PersonalizedRequest(
    val limit: Int = 30
)

/**
 * 个性化推荐接口的响应包装
 *
 * 该接口的响应结构为 { code: 200, result: [...] }，
 * 与其他接口的扁平结构不同，因此单独定义响应类型。
 */
@Serializable
data class PersonalizedResponse(
    val code: Int = 0,
    val result: List<PersonalizedPlaylist> = emptyList(),
) {
    val isSuccess: Boolean get() = code == 200
}

/**
 * 个性化推荐歌单数据（供 UI 层使用的简化包装）
 */
@Serializable
data class PersonalizedData(
    /** 推荐歌单列表 */
    val playlists: List<PersonalizedPlaylist> = emptyList(),
)

@Serializable
data class PersonalizedPlaylist(
    val id: Long = 0,
    val name: String = "",
    /** 歌单封面 */
    val picUrl: String = "",
)

// ======================= 歌曲信息 =======================

@Serializable
data class SongUrlRequest(
    val ids: String,
    val level: String = "standard",
    val encodeType: String = "flac"
)

@Serializable
data class SongUrlResponse(
    val code: Int = 0,
    val data: List<SongUrlItem> = emptyList(),
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongUrlItem(
    val id: Long = 0,
    val url: String? = null,
    val br: Long = 0,
    val size: Long = 0,
    val md5: String? = null,
    val type: String? = null,
    /**
     * VIP歌曲或者无版权时收费标识
     * freeTrialInfo 不为空表示可能只能试听
     */
    val freeTrialInfo: FreeTrialInfo? = null,
)

@Serializable
data class FreeTrialInfo(
    val start: Long = 0,
    val end: Long = 0,
)

// ======================= 热门歌手 =======================

@Serializable
data class TopArtistsRequest(
    val offset: Int = 0,
    val limit: Int = 30,
    val total: Boolean = true
)

@Serializable
data class TopArtistsResponse(
    val code: Int = 0,
    val artists: List<Artist> = emptyList(),
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class Artist(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val img1v1Url: String = "",
)

@Serializable
data class ArtistSublistRequest(
    val limit: Int = 25,
    val offset: Int = 0,
    val total: Boolean = true
)

@Serializable
data class ArtistSublistResponse(
    val code: Int = 0,
    // 实际返回结构：{"data":[...], "code":200}，data 字段直接就是歌手数组
    val data: List<Artist> = emptyList()
)

// ======================= 歌单详情 =======================

@Serializable
data class PlaylistDetailRequest(
    val id: Long,
    val n: Int = 100000,
    val s: Int = 8
)

@Serializable
data class PlaylistDetailResponse(
    val code: Int = 0,
    val playlist: PlaylistDetail? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class PlaylistDetail(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String = "",
    val description: String? = null,
    val playCount: Long = 0,
    val tracks: List<Track> = emptyList()
)

@Serializable
data class Track(
    val id: Long = 0,
    val name: String = "",
    // 同时兼容 "ar" (常规接口) 和 "artists" (相似歌曲接口)
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @JsonNames("ar", "artists")
    val ar: List<Artist> = emptyList(),
    // 同时兼容 "al" (常规接口) 和 "album" (相似歌曲接口)
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @JsonNames("al", "album")
    val al: Album = Album(),
    val fee: Int = 0,
    val publishTime: Long = 0, // 歌曲发行时间戳，部分接口在歌曲详情中包含
    val dt: Long = 0
)

@Serializable
data class Album(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = ""
)

// ======================= 用户账户信息 =======================

@Serializable
data class AccountInfoResponse(
    val code: Int = 0,
    val account: Account? = null,
    val profile: UserProfile? = null
)

@Serializable
data class Account(
    val id: Long = 0,
    val userName: String = "",
    val type: Int = 0,
    val status: Int = 0,
)

// ======================= 最近播放 =======================

@Serializable
data class RecentPlaylistRequest(
    val limit: Int = 100
)

@Serializable
data class RecentPlaylistResponse(
    val code: Int = 0,
    val data: RecentPlaylistData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class RecentPlaylistData(
    val list: List<RecentPlayItem> = emptyList()
)

@Serializable
data class RecentPlayItem(
    val data: RecentPlaylistInfo
)

@Serializable
data class RecentPlaylistInfo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverImgUrl")
    val picUrl: String = "",
    val creator: PlaylistCreator? = null
)

// ======================= 私人 FM 模型 =======================

@Serializable
data class PersonalFmResponse(
    val code: Int = 0,
    val data: List<Track> = emptyList()
)

@Serializable
data class LikeSongRequest(
    val trackId: Long,
    val like: Boolean = true
)

@Serializable
data class LikeSongResponse(
    val code: Int = 0,
    val playlistId: Long = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class LikeSongListRequest(
    val uid: Long
)

@Serializable
data class LikeSongListResponse(
    val code: Int = 0,
    val ids: List<Long> = emptyList(),
    val checkPoint: Long = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class TrashFmRequest(
    val songId: Long,
    val alg: String = "rt",
    val time: Int = 25
)

// ======================= 每日推荐歌曲模型 =======================

@Serializable
data class DailyRecommendSongsResponse(
    val code: Int = 0,
    val data: DailyRecommendData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class DailyRecommendData(
    val dailySongs: List<DailySong> = emptyList()
)

@Serializable
data class DailySong(
    val id: Long = 0,
    val name: String = "",
    val ar: List<Artist> = emptyList(),
    val al: Album = Album(),
    val fee: Int = 0,
    // 推荐理由（例如："根据你喜欢的 xxx 推荐"）
    val reason: String? = null
)
// ======================= 历史日推模型 =======================

@Serializable
data class HistoryDatesResponse(
    val code: Int = 0,
    val data: HistoryDatesData? = null
)

@Serializable
data class HistoryDatesData(
    val list: List<String> = emptyList()
)

@Serializable
data class HistoryDetailRequest(
    val date: String
)

@Serializable
data class HistoryDetailResponse(
    val code: Int = 0,
    val data: HistoryDetailData? = null
)

@Serializable
data class HistoryDetailData(
    val dailySongs: List<DailySong> = emptyList()
)

// ======================= 排行榜 DTO =======================

@Serializable
data class ToplistDetailResponse(
    val code: Int = 0,
    val list: List<ToplistDto> = emptyList()
)

@Serializable
data class ToplistDto(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String = "",
    val updateFrequency: String = "",
    // 部分榜单无歌曲预览数据
    val tracks: List<ToplistTrackDto>? = null
)

@Serializable
data class ToplistTrackDto(
    val first: String = "",  // 歌名
    val second: String = "" // 歌手
)

// ======================= 云搜索 DTO =======================

@Serializable
data class CloudSearchRequest(
    val s: String,
    val type: Int = 1,      // 1=单曲 10=专辑 100=歌手 1000=歌单
    val limit: Int = 30,
    val offset: Int = 0
)

@Serializable
data class CloudSearchResponse(
    val code: Int = 0,
    val result: SearchResult? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SearchResult(
    val songs: List<SearchSong>? = null,
    val songCount: Int = 0
)

@Serializable
data class SearchSong(
    val id: Long = 0,
    val name: String = "",
    val ar: List<Artist> = emptyList(),
    val al: Album = Album(),
    val fee: Int = 0,
    val dt: Long = 0     // 歌曲时长 ms
)

// ======================= 热搜 DTO =======================

@Serializable
data class HotSearchDetailResponse(
    val code: Int = 0,
    val data: List<HotSearchItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class HotSearchItem(
    val searchWord: String = "",
    val score: Int = 0,
    val content: String = "",
    val iconUrl: String? = null
)

// ======================= 精品歌单 DTO =======================

@Serializable
data class HighQualityTagsResponse(
    val code: Int = 0,
    val tags: List<HighQualityTag> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class HighQualityTag(
    val id: Long = 0,
    val name: String = "",
    val category: Int = -1,
    val hot: Boolean = false
)

@Serializable
data class HighQualityPlaylistRequest(
    val cat: String = "全部",
    val limit: Int = 50,
    val lasttime: Long = 0,
    val total: Boolean = true
)

@Serializable
data class HighQualityPlaylistResponse(
    val code: Int = 0,
    val playlists: List<HighQualityPlaylist> = emptyList(),
    val more: Boolean = false,
    val lasttime: Long = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class HighQualityPlaylist(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String = "",
    val tags: List<String> = emptyList(),
    val playCount: Long = 0
)

// ======================= 音乐库 (Library) DTOs =======================

@Serializable
data class UserPlaylistRequest(
    val uid: Long,
    val limit: Int = 1000,
    val offset: Int = 0
)

@Serializable
data class UserPlaylistResponse(
    val code: Int = 0,
    val playlist: List<UserPlaylist> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class UserPlaylist(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String = "",
    val playCount: Long = 0,
    val trackCount: Int = 0,
    val userId: Long = 0,
    val creator: PlaylistCreator? = null,
    val updateTime: Long = 0
)

@Serializable
data class AlbumSublistRequest(
    val limit: Int = 1000,
    val offset: Int = 0,
    val total: Boolean = true
)

@Serializable
data class AlbumSublistResponse(
    val code: Int = 0,
    val data: List<AlbumSubItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class AlbumSubItem(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val artists: List<Artist> = emptyList(),
    val size: Int = 0,
    val subTime: Long = 0
)

@Serializable
data class UserSubcountResponse(
    val code: Int = 0,
    val artistCount: Int = 0,
    val playlistCount: Int = 0,
    val mvCount: Int = 0,
    val createPlaylistCount: Int = 0,
    val subPlaylistCount: Int = 0,
    val albumCount: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class PlaylistCreateRequest(
    val name: String,
    val privacy: Int = 0,
    val type: String = "NORMAL"
)

@Serializable
data class PlaylistCreateResponse(
    val code: Int = 0,
    val id: Long = 0,
    val playlist: PlaylistDetail? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= 歌词 DTO =======================

@Serializable
data class LyricRequest(
    val id: Long,
    val cp: Boolean = false,
    val tv: Int = -1,
    val lv: Int = -1,
    val rv: Int = -1,
    val kv: Int = -1,
    val yv: Int = 99, // 默认设为 99 以请求 YRC 歌词
    val ytv: Int = -1,
    val yrv: Int = -1
)

@Serializable
data class LyricResponse(
    val code: Int = 0,
    val lrc: LyricContent? = null,
    val tlyric: LyricContent? = null,
    val ytlrc: LyricContent? = null,    // 新增：YRC 对应的翻译
    val romalrc: LyricContent? = null,
    val yrc: LyricContent? = null, // 新增：存放逐字歌词的原始文本
    val nolyric: Boolean = false, // 标识是否有歌词，若为 true 则通常无歌词
    val uncollected: Boolean = false // 标识歌曲是否未收录歌词
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class LyricContent(
    val version: Int = 0,
    val lyric: String? = null
)

// ======================= 歌曲详情 DTO =======================

@Serializable
data class SongDetailRequest(
    val c: String
)

@Serializable
data class SongDetailResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= 相似歌手 DTO =======================

@Serializable
data class SimiArtistRequest(
    val artistid: Long
)

@Serializable
data class SimiArtistResponse(
    val code: Int = 0,
    val artists: List<Artist> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= 艺人详情 DTO =======================

@Serializable
data class ArtistDetailRequest(val id: Long)

@Serializable
data class ArtistDetailResponse(
    val code: Int = 0,
    val data: ArtistDetailData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class ArtistDetailData(
    val artist: ArtistDetailInfo? = null
)

@Serializable
data class ArtistDetailInfo(
    val id: Long = 0,
    val name: String = "",
    val cover: String = "",
    val avatar: String = "",
    val briefDesc: String = "",
    val albumSize: Int = 0,
    val musicSize: Int = 0,
    val identifyTag: List<String>? = null,
    val trans: String? = null, // 翻译名称
    val alias: List<String>? = null // 别名列表
)

// ======================= 艺人粉丝数量 DTO =======================

@Serializable
data class ArtistFollowCountRequest(val id: Long)

@Serializable
data class ArtistFollowCountResponse(
    val code: Int = 0,
    val message: String? = null,
    val fansCount: Long = 0,
    val isFollow: Boolean = false,
    val followCount: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}
// 歌手关注与粉丝数详细数据
@Serializable
data class ArtistFollowCountData(
    @SerialName("fans") val fans: Long? = null,             // 粉丝数
    @SerialName("fansCnt") val fansCnt: Long? = null,       // 粉丝计数 (EAPI返回)
    @SerialName("fansCount") val fansCount: Long? = null,   // 粉丝总数
    @SerialName("followCount") val followCount: Long? = null // 关注数
)

// 歌手关注数获取响应体 ( concrete 实体类，避免泛型反序列化问题 )
@Serializable
data class ArtistFollowCountGetResponse(
    @SerialName("code") val code: Int = 0,              // 响应状态码
    @SerialName("message") val message: String? = null, // 响应消息
    @SerialName("data") val data: ArtistFollowCountData? = null // 核心业务数据
) {
    /** 请求是否成功 */
    val isSuccess: Boolean get() = code == 200
}

// ======================= 艺人专辑 DTO =======================

@Serializable
data class ArtistAlbumRequest(
    val limit: Int = 10,
    val offset: Int = 0,
    val total: Boolean = true
)

@Serializable
data class ArtistAlbumResponse(
    val code: Int = 0,
    val hotAlbums: List<ArtistAlbum> = emptyList(),
    val more: Boolean = false
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class ArtistAlbum(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val publishTime: Long = 0,
    val size: Int = 0
)

// ======================= 艺人热门单曲 DTO =======================

@Serializable
data class ArtistTopSongsRequest(val id: Long)

@Serializable
data class ArtistTopSongsResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= 收藏与取消收藏歌手 DTO =======================

@Serializable
data class ArtistSubscriptionRequest(
    val artistId: Long,
    val artistIds: String
)

@Serializable
data class ArtistSubscriptionResponse(
    val code: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= 专辑详情 DTO =======================

@Serializable
data class AlbumDetailRequest(val id: Long)

@Serializable
data class AlbumDetailResponse(
    val code: Int = 0,
    val album: AlbumInfo = AlbumInfo(),
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class AlbumInfo(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val description: String? = null
)

// ======================= 评论 DTO =======================

@Serializable
data class CommentsRequest(
    val threadId: String,
    val rid: String,
    val limit: Int = 20,
    val offset: Int = 0,
    val beforeTime: Long = 0
)

@Serializable
data class CommentsResponse(
    val code: Int = 0,
    val total: Int = 0,
    val more: Boolean = false,
    val comments: List<CommentItem> = emptyList(),
    val hotComments: List<CommentItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class CommentItem(
    val commentId: Long = 0,
    val user: CommentUser = CommentUser(),
    val content: String = "",
    val time: Long = 0,
    val timeStr: String? = null,
    val likedCount: Int = 0,
    val liked: Boolean = false,
    val beReplied: List<BeRepliedComment>? = null
)

@Serializable
data class CommentUser(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = ""
)

@Serializable
data class BeRepliedComment(
    val user: CommentUser? = null,
    val content: String? = null,
    val beRepliedCommentId: Long? = null
)

// ======================= 歌曲百科简要信息 DTO =======================

@Serializable
data class SongWikiSummaryRequest(
    val songId: Long
)

@Serializable
data class SongWikiSummaryResponse(
    val code: Int = 0,
    val data: SongWikiSummaryData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongWikiSummaryData(
    val blocks: List<SongWikiBlock> = emptyList()
)

@Serializable
data class SongWikiBlock(
    val code: String = "",
    val showType: String = "",
    val creatives: List<SongWikiCreative> = emptyList()
)

@Serializable
data class SongWikiCreative(
    val creativeType: String = "",
    val resources: List<SongWikiResource> = emptyList(),
    val uiElement: SongWikiUiElement? = null
)

@Serializable
data class SongWikiResource(
    val uiElement: SongWikiUiElement? = null
)

@Serializable
data class SongWikiUiElement(
    val mainTitle: SongWikiMainTitle? = null,
    val textLinks: List<SongWikiTextLink> = emptyList(),
    val descriptions: List<SongWikiDescription> = emptyList() // 百科描述列表，用于提取歌曲背景、所获奖项等
)

@Serializable
data class SongWikiMainTitle(
    val title: String = ""
)

@Serializable
data class SongWikiTextLink(
    val text: String = ""
)

@Serializable
data class SongWikiDescription(
    val description: String = "" // 具体的描述内容文本
)



// ======================= 歌曲创作者 DTO =======================

@Serializable
data class SongCreatorsRequest(
    val songId: Long
)

@Serializable
data class SongCreatorsResponse(
    val code: Int = 0,
    val data: SongCreatorsData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SongCreatorsData(
    val songCreatorsRoleVos: List<SongCreatorRole> = emptyList()
)

@Serializable
data class SongCreatorRole(
    val roleName: String = "",
    val creatorMetaVOS: List<CreatorMeta> = emptyList()
)

@Serializable
data class CreatorMeta(
    val artistName: String = ""
)

// ======================= 设置和隐私 DTO =======================

@Serializable
data class UserLevelResponse(
    val code: Int = 0,
    val data: UserLevelData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class UserLevelData(
    val level: Int = 0,
    val nextPlayCount: Long = 0,
    val nextLoginCount: Int = 0,
    val nowPlayCount: Long = 0,
    val nowLoginCount: Int = 0,
    val progress: Double = 0.0
)

@Serializable
data class VipInfoResponse(
    val code: Int = 0,
    val data: VipInfoData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class VipInfoData(
    val redVipLevel: Int = 0,
    val vipType: Int = 0, // 0-非VIP, 10-音乐包, 11-黑胶VIP
    val expireTime: Long = 0,
    val isVip: Boolean = false
)

@Serializable
data class UserBindingRequest(
    val uid: Long
)

@Serializable
data class UserBindingResponse(
    val code: Int = 0,
    val bindings: List<UserBindingItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class UserBindingItem(
    val type: Int = 0, // 1-手机, 2-新浪微博, 5-腾讯微博, 10-微信, 20-QQ, 1000-网易邮箱
    val typeName: String = "",
    val userId: Long = 0,
    val expired: Boolean = false
)

@Serializable
data class UserProfileUpdateRequest(
    val nickname: String,
    val gender: Int,
    val birthday: Long,
    val province: Int,
    val city: Int,
    val signature: String
)

@Serializable
data class UserProfileUpdateResponse(
    val code: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class NicknameCheckRequest(
    val nickname: String
)

@Serializable
data class NicknameCheckResponse(
    val code: Int = 0,
    val duplicated: Boolean = false,
    val candidateNicknames: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class DailySigninRequest(
    val type: Int = 0
)

@Serializable
data class DailySigninResponse(
    val code: Int = 0,
    val point: Int = 0,
    val msg: String? = null
) {
    val isSuccess: Boolean get() = code == 200 || code == -2
}

@Serializable
data class LogoutApiResponse(
    val code: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class AvatarUploadResponse(
    val code: Int = 0,
    val url: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class PlaylistTracksManipulateRequest(
    val op: String, // "add" or "del"
    val pid: Long,
    val trackIds: String, // JSON Array String: "[123]"
    val imme: String = "true"
)

@Serializable
data class PlaylistTracksManipulateResponse(
    val code: Int = 0,
    val count: Int = 0,
    val status: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class SimiSongRequest(
    val songid: String
)

@Serializable
data class SimiSongResponse(
    val code: Int = 0,
    val songs: List<Track> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class IntelligenceSongsRequest(
    val songId: String,
    val playlistId: String,
    val type: String = "fromPlayOne",
    val startMusicId: String,
    val count: Int = 20
)

@Serializable
data class IntelligenceSongsResponse(
    val code: Int = 0,
    val data: List<IntelligenceItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class IntelligenceItem(
    val id: Long = 0,
    val recommended: Boolean = false,
    val songInfo: Track? = null
)
