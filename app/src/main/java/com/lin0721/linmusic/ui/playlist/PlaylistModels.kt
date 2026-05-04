package com.lin0721.linmusic.ui.playlist

// 歌单详情业务模型
data class PlaylistDetailData(
    val id: Long,
    val name: String,
    val coverImgUrl: String,
    val description: String?,
    val playCount: Long,
    val trackCount: Int,
    val tracks: List<TrackData>
)

// 歌曲业务模型
data class TrackData(
    val id: Long,
    val name: String,
    val artist: String,
    val coverUrl: String,
    val albumName: String
)
