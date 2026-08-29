package com.lin0721.linmusic.feature.player.domain

// 歌曲详情/百科信息领域模型
data class SongWikiData(
    val style: String = "",
    val album: String = "",
    val language: String = "",
    val publishTime: String = "",
    val bpm: String = "",
    val creators: String = "",
    val creatorRoles: List<SongWikiCreatorRole> = emptyList(),
    val entertainment: String = "",
    val background: String = "",
    val awards: String = ""
)

// 制作人员单个角色（如"作词"）及其对应的艺人名单
data class SongWikiCreatorRole(
    val roleName: String,
    val artistNames: List<String>
)
