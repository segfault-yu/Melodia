package com.lin0721.linmusic.feature.player.domain

// 歌曲详情/百科信息领域模型
data class SongWikiData(
    val style: String = "",
    val album: String = "",
    val language: String = "",
    val publishTime: String = "",
    val bpm: String = "",
    val creators: String = "",
    val entertainment: String = "",
    val background: String = "",
    val awards: String = ""
)
