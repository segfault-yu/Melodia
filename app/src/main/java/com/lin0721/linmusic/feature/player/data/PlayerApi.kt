package com.lin0721.linmusic.feature.player.data

import com.lin0721.linmusic.core.model.Track
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// 播放相关（播放链接/歌词/歌曲详情/百科/智能推荐）的网易云 Retrofit 接口定义。
interface PlayerApi {

    // 获取歌曲播放链接
    @POST("/eapi/song/enhance/player/url/v1")
    suspend fun getSongUrl(
        @Body body: SongUrlRequest
    ): SongUrlResponse

    @POST("/eapi/song/lyric/v1")
    suspend fun getLyrics(
        @Body body: LyricRequest
    ): LyricResponse

    @POST("/eapi/v3/song/detail")
    suspend fun getSongDetail(
        @Body body: SongDetailRequest
    ): SongDetailResponse

    @POST("/eapi/v1/discovery/simiSong")
    suspend fun getSimiSongs(
        @Body body: SimiSongRequest
    ): SimiSongResponse

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

    // ================== 智能推荐歌曲 ==================
    @POST("/eapi/playmode/intelligence/list")
    suspend fun getIntelligenceSongs(
        @Body body: IntelligenceSongsRequest
    ): IntelligenceSongsResponse
}

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
    // VIP歌曲或者无版权时收费标识，freeTrialInfo 不为空表示可能只能试听
    val freeTrialInfo: FreeTrialInfo? = null,
)

@Serializable
data class FreeTrialInfo(
    val start: Long = 0,
    val end: Long = 0,
)

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

// ======================= 相似歌曲 DTO =======================

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

// ======================= 智能推荐歌曲 DTO =======================

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
