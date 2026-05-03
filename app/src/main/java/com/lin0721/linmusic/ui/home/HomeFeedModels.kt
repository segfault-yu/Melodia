package com.lin0721.linmusic.ui.home

/**
 * 首页动态 Feed 数据模型
 */
sealed class HomeSection {
    // 方形封面横向滑动列表 (歌单/专辑/排行榜/新歌)
    data class SectionCarousel(val title: String, val items: List<CardItem>) : HomeSection()
    
    // 圆形头像横向滑动列表 (艺人/播客)
    data class SectionArtist(val title: String, val items: List<CardItem>) : HomeSection()
}

/**
 * 基础卡片项模型
 */
data class CardItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String,
    val isSong: Boolean = false,
    val type: CardType = CardType.PLAYLIST
)

enum class CardType {
    PLAYLIST, ALBUM, ARTIST, RADIO
}
