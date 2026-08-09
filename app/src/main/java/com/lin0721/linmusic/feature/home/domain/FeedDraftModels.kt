package com.lin0721.linmusic.feature.home.domain

// 调研确认：ActionType/FeedItem/FeedSection 全项目零调用者（含数据层），疑似首页 Feed 流的早期草稿模型，
// 与实际使用的 HomeFeedPage/HomeSection/CardItem（见 HomeFeedModels.kt）并存。按规划保留不删，留待后续确认取舍。

/**
 * 交互类型
 */
enum class ActionType {
    PLAY_SONG,      // 播放歌曲
    OPEN_PLAYLIST,  // 打开歌单
    OPEN_ARTIST,    // 打开歌手页
    OPEN_ALBUM      // 打开专辑页
}

/**
 * 基础卡片数据
 */
data class FeedItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String,
    val actionType: ActionType
)

/**
 * 首页 Feed 区域密封类
 */
sealed class FeedSection {
    abstract val title: String

    /** 标准横向方形轮播 */
    data class Carousel(
        override val title: String,
        val items: List<FeedItem>
    ) : FeedSection()

    /** 横向圆形歌手轮播 */
    data class ArtistCarousel(
        override val title: String,
        val items: List<FeedItem>
    ) : FeedSection()

    /** 纵向单曲列表 (3-5首) */
    data class VerticalTrackList(
        override val title: String,
        val items: List<FeedItem>
    ) : FeedSection()

    /** 巨型带播放按钮的推荐海报 */
    data class FeaturedLargeCard(
        override val title: String,
        val item: FeedItem
    ) : FeedSection()

    /** 两列大色块网格 */
    data class TwoColumnGrid(
        override val title: String,
        val items: List<FeedItem>
    ) : FeedSection()
}





