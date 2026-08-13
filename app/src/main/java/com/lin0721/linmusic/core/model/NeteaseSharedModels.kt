package com.lin0721.linmusic.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// 跨业务域共享的网易云基础数据模型，被 playlist/player/artist/library/home/search 等多个域的接口响应体引用。

// 空请求体，用于不需要参数的 POST 接口
@Serializable
class EmptyBody

@Serializable
data class Artist(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = "",
    val img1v1Url: String = "",
)

@Serializable
data class Album(
    val id: Long = 0,
    val name: String = "",
    val picUrl: String = ""
)

@Serializable
data class Track(
    val id: Long = 0,
    val name: String = "",
    // 同时兼容 "ar" (常规接口) 和 "artists" (相似歌曲接口)
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @JsonNames("ar", "artists")
    val ar: List<Artist> = emptyList(),
    // 同时兼容 "al" (常规接口) 和 "album" (相似歌曲接口)
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @JsonNames("al", "album")
    val al: Album = Album(),
    val fee: Int = 0,
    val publishTime: Long = 0, // 歌曲发行时间戳，部分接口在歌曲详情中包含
    val dt: Long = 0
)

@Serializable
data class PlaylistCreator(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
)

// 歌单/专辑统一详情模型，playlist 域产出，create 域新建歌单后同样返回该结构
@Serializable
data class PlaylistDetail(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String = "",
    val description: String? = null,
    val playCount: Long = 0,
    val subscribed: Boolean = false,
    val creator: PlaylistCreator? = null,
    val tracks: List<Track> = emptyList()
)

// 歌手领域模型，artist/player/home 等多域共用
data class ArtistInfo(
    val id: Long,
    val name: String,
    val avatarUrl: String
)
