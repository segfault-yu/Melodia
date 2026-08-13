package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// 播放器详情页（歌曲详情/百科/创作者）的网易云 Retrofit 接口定义。
interface PlayerApi {

    @POST("/eapi/v3/song/detail")
    suspend fun getSongDetail(
        @Body body: SongDetailRequest
    ): SongDetailResponse

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
}

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
