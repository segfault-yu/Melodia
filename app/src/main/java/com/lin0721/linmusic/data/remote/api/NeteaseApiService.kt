package com.lin0721.linmusic.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 网易云音乐 Retrofit 接口定义
 *
 * 所有 POST 请求的 JSON body 会被 [CryptoInterceptor] 自动拦截加密，
 * 调用方只需传入明文参数即可。
 */
interface NeteaseApiService {

    // ======================= 登录 =======================

    /**
     * 手机号登录
     *
     * @param body 包含手机号、密码 MD5、国家码等参数
     */
    @POST("/weapi/login/cellphone")
    suspend fun loginByPhone(@Body body: LoginByPhoneRequest): NeteaseResponse<LoginData>

    /**
     * 邮箱登录
     *
     * @param body 包含邮箱、密码 MD5 等参数
     */
    @POST("/weapi/login")
    suspend fun loginByEmail(@Body body: LoginByEmailRequest): NeteaseResponse<LoginData>

    // ===================== 推荐歌单 =====================

    /**
     * 获取每日推荐歌单
     *
     * 注意：需要登录后的 Cookie 才能调用
     */
    @POST("/weapi/v1/discovery/recommend/resource")
    suspend fun getDailyRecommendPlaylists(
        @Body body: EmptyBody = EmptyBody()
    ): NeteaseResponse<RecommendPlaylistData>

    // =================== 个性化推荐 ===================

    /**
     * 获取个性化推荐歌单（公开接口，无需登录）
     *
     * 用于在未登录状态下展示首页推荐歌单列表。
     */
    @POST("/weapi/personalized")
    suspend fun getPersonalizedPlaylists(
        @Body body: EmptyBody = EmptyBody()
    ): PersonalizedResponse
}

// ======================== 请求体 ========================

/**
 * 手机号登录请求
 */
@Serializable
data class LoginByPhoneRequest(
    /** 手机号 */
    val phone: String,
    /** 密码的 MD5 值 */
    val password: String,
    /** 国家区号，默认 86 (中国大陆) */
    val countrycode: String = "86",
    /** 是否记住登录 */
    val rememberLogin: Boolean = true,
)

/**
 * 邮箱登录请求
 */
@Serializable
data class LoginByEmailRequest(
    /** 邮箱地址 */
    val username: String,
    /** 密码的 MD5 值 */
    val password: String,
    /** 是否记住登录 */
    val rememberLogin: Boolean = true,
)

/**
 * 空请求体，用于不需要参数的 POST 接口
 */
@Serializable
class EmptyBody

// ======================= 响应体 =======================

/**
 * 网易云 API 通用响应包装
 *
 * 所有接口都会返回 [code] 和可选的 [msg]，
 * 具体业务数据由泛型 [T] 内联展平。
 *
 * 注意：网易云的响应结构比较扁平，业务字段直接和 code 平级，
 * 因此这里不做二层嵌套，而是让具体的 Data 类自行包含业务字段。
 */
@Serializable
data class NeteaseResponse<T>(
    val code: Int = 0,
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

