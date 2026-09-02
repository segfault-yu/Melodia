package com.lin0721.linmusic.feature.recent.domain

import com.lin0721.linmusic.core.model.Track

// 最近播放的三类记录。playedAtText 在映射时定格，避免列表重组时反复换算时间
data class RecentSong(
    val track: Track,
    val playedAtText: String
)

data class RecentPlaylist(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val creatorName: String,
    val playedAtText: String
)

data class RecentAlbum(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val playedAtText: String
)
