package com.lin0721.linmusic.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// 网易云音乐 Retrofit 接口定义。所有 POST 请求会被 [CryptoInterceptor] 自动加密。
interface NeteaseApiService {

    // ======================= 登录 (二维码) =======================

    // 二维码登录：第一步 - 获取 Key
    @POST("/weapi/login/qr/key")
    suspend fun getQrKey(@Body body: EmptyBody = EmptyBody()): NeteaseResponse<QrKeyData>

    // 二维码登录：第二步 - 获取二维码
    @POST("/weapi/login/qr/create")
    suspend fun createQr(@Body body: QrCreateRequest): NeteaseResponse<QrCreateData>

    // 二维码登录：第三步 - 检查二维码状态 (800:过期, 801:等待, 802:待确认, 803:成功)
    @POST("/weapi/login/qr/check")
    suspend fun checkQr(@Body body: QrCheckRequest): NeteaseResponse<QrCheckData>

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

    /**
     * 获取歌曲播放链接
     */
    @POST("/eapi/song/enhance/player/url/v1")
    suspend fun getSongUrl(
        @Body body: SongUrlRequest
    ): SongUrlResponse

    // ======================= 热门歌手 =======================

    /**
     * 获取热门歌手
     */
    @POST("/eapi/v1/artist/top")
    suspend fun getTopArtists(
        @Body body: TopArtistsRequest = TopArtistsRequest()
    ): TopArtistsResponse

    // ======================= 用户信息 =======================

    /**
     * 获取当前登录账号信息
     */
    @POST("/eapi/nuser/account/get")
    suspend fun getAccountInfo(
        @Body body: EmptyBody = EmptyBody()
    ): AccountInfoResponse

    /**
     * 获取首页动态内容 (支持分页)
     */
    @POST("/eapi/homepage/block/page")
    suspend fun getHomepageBlocks(
        @Body body: HomepageBlockRequest
    ): HomepageBlockResponse

    // ================== 最近播放 ==================

    /**
     * 获取最近播放歌单
     */
    @POST("/eapi/play-record/playlist/list")
    suspend fun getRecentPlaylists(
        @Body body: RecentPlaylistRequest = RecentPlaylistRequest()
    ): RecentPlaylistResponse

    // ================== 私人 FM ==================

    /**
     * 获取私人 FM 歌曲
     */
    @POST("/eapi/v1/radio/get")
    suspend fun getPersonalFm(
        @Body body: EmptyBody = EmptyBody()
    ): PersonalFmResponse

    /**
     * 歌曲打心 (Like/Unlike)
     */
    @POST("/eapi/song/like")
    suspend fun likeSong(
        @Body body: LikeSongRequest
    ): NeteaseResponse<Unit>

    /**
     * 垃圾桶 (移除私人 FM 歌曲)
     */
    @POST("/eapi/v1/radio/trash")
    suspend fun trashFmSong(
        @Body body: TrashFmRequest
    ): NeteaseResponse<Unit>
}

// 首页动态内容请求体
@Serializable
data class HomepageBlockRequest(
    val cursor: String? = null,
    val refresh: Boolean = false
)

/**
 * 二维码 Key 响应数据
 */
@Serializable
data class QrKeyData(
    val unikey: String = ""
)

/**
 * 创建二维码请求
 */
@Serializable
data class QrCreateRequest(
    val key: String,
    val qr64: Boolean = true
)

/**
 * 二维码创建响应数据
 */
@Serializable
data class QrCreateData(
    val qrurl: String = "",
    val qrimg: String = ""
)

/**
 * 检查二维码状态请求
 */
@Serializable
data class QrCheckRequest(
    val key: String
)

/**
 * 二维码状态响应数据
 */
@Serializable
data class QrCheckData(
    /** 状态码 */
    val code: Int = 0,
    /** 状态消息 */
    val message: String = "",
    /** 成功时的 Cookie (MUSIC_U) */
    val cookie: String = ""
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
    val encodeType: String = "flac",
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
    val limit: Int = 30
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
    val ar: List<Artist> = emptyList(),
    val al: Album = Album(),
    val fee: Int = 0 
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
data class TrashFmRequest(
    val songId: Long,
    val alg: String = "rt",
    val time: Int = 25
)

