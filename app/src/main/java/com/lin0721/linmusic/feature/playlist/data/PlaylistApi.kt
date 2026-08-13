package com.lin0721.linmusic.feature.playlist.data

import com.lin0721.linmusic.core.model.EmptyBody
import com.lin0721.linmusic.core.model.PlaylistCreator
import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// 歌单/专辑详情相关的网易云 Retrofit 接口定义。
interface PlaylistApi {

    @POST("/eapi/v6/playlist/detail")
    suspend fun getPlaylistDetail(
        @Body body: PlaylistDetailRequest
    ): PlaylistDetailResponse

    // 网易云 WEAPI 专辑详情接口，专辑 ID 需放入 URL 路径中
    @POST("/weapi/v1/album/{id}")
    suspend fun getAlbumDetail(
        @Path("id") id: Long,
        @Body body: EmptyBody = EmptyBody()
    ): AlbumDetailResponse

    // ================== 歌单歌曲操作 (添加/删除) ==================
    @POST("/eapi/playlist/manipulate/tracks")
    suspend fun manipulatePlaylistTracks(
        @Body body: PlaylistTracksManipulateRequest
    ): PlaylistTracksManipulateResponse

    // ================== 歌单收藏操作 (收藏/取消收藏) ==================
    @POST("/eapi/playlist/{op}")
    suspend fun subscribePlaylist(
        @Path("op") op: String,
        @Body body: PlaylistSubscribeRequest
    ): PlaylistSubscribeResponse
}

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
    val subscribed: Boolean = false,
    val creator: PlaylistCreator? = null,
    val tracks: List<Track> = emptyList()
)

// ======================= 歌单歌曲操作 DTO =======================

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

// ======================= 收藏与取消收藏歌单 DTO =======================

@Serializable
data class PlaylistSubscribeRequest(
    val id: Long
)

@Serializable
data class PlaylistSubscribeResponse(
    val code: Int = 0,
    val message: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// ======================= 专辑详情 DTO =======================

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
