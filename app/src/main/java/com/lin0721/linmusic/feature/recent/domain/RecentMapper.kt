package com.lin0721.linmusic.feature.recent.domain

import com.lin0721.linmusic.feature.recent.data.RecentAlbumItem
import com.lin0721.linmusic.feature.recent.data.RecentPlaylistItem
import com.lin0721.linmusic.feature.recent.data.RecentSongItem

// DTO → 领域模型。nowMs 由调用方传入同一批次的时间戳，保证同屏记录的相对时间基准一致
fun RecentSongItem.toDomain(nowMs: Long): RecentSong = RecentSong(
    track = data,
    playedAtText = formatPlayedAt(playTime, nowMs)
)

fun RecentPlaylistItem.toDomain(nowMs: Long): RecentPlaylist = RecentPlaylist(
    id = data.id,
    name = data.name,
    coverUrl = data.picUrl,
    creatorName = data.creator?.nickname.orEmpty(),
    playedAtText = formatPlayedAt(playTime, nowMs)
)

fun RecentAlbumItem.toDomain(nowMs: Long): RecentAlbum = RecentAlbum(
    id = data.id,
    name = data.name,
    coverUrl = data.picUrl,
    artistName = data.artist?.name.orEmpty(),
    playedAtText = formatPlayedAt(playTime, nowMs)
)
