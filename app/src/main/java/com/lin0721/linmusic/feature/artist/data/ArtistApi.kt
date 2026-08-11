package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// 歌手/艺人相关的网易云 Retrofit 接口定义。
interface ArtistApi {

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

    @POST("/eapi/discovery/simiArtist")
    suspend fun getSimiArtists(
        @Body body: SimiArtistRequest
    ): SimiArtistResponse

    @POST("/eapi/artist/head/info/get")
    suspend fun getArtistDetail(
        @Body body: ArtistDetailRequest
    ): ArtistDetailResponse

    // ================== 艺人动态信息（粉丝数等） ==================
    @POST("/eapi/artist/detail/dynamic")
    suspend fun getArtistDetailDynamic(
        @Body body: ArtistFollowCountRequest
    ): ArtistFollowCountResponse

    // 获取歌手粉丝数量
    @POST("/weapi/artist/follow/count/get")
    suspend fun getArtistFollowCount(
        @Body body: ArtistFollowCountRequest
    ): ArtistFollowCountGetResponse

    @POST("/weapi/artist/albums/{id}")
    suspend fun getArtistAlbums(
        @Path("id") id: Long,
        @Body body: ArtistAlbumRequest = ArtistAlbumRequest()
    ): ArtistAlbumResponse

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
}

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
    // 请求是否成功
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
