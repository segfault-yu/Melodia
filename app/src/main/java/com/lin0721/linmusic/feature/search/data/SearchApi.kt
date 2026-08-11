package com.lin0721.linmusic.feature.search.data

import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.EmptyBody
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// 搜索相关的网易云 Retrofit 接口定义。
interface SearchApi {

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
}

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
