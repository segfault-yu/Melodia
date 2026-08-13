package com.lin0721.linmusic.core.player.domain

// 单个字符/单词的耗时元数据
data class WordInfo(
    val text: String,
    val startOffsetMs: Long,  // 相对于整行歌词起始时间的偏移毫秒数
    val durationMs: Long      // 该字/词的持续发音毫秒数
)

// 歌词行领域模型
data class LyricLine(
    val timeMs: Long,
    val durationMs: Long = 0, // 新增：整行歌词的持续发音时间
    val text: String,
    val translation: String? = null,
    val words: List<WordInfo> = emptyList() // 如果是普通LRC则此列表为空；YRC则填入单字列表
)
