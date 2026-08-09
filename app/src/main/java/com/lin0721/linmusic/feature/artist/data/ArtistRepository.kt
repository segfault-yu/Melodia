package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.api.Artist
import com.lin0721.linmusic.core.api.ArtistAlbum
import com.lin0721.linmusic.core.api.ArtistDetailInfo
import com.lin0721.linmusic.core.api.Track
import com.lin0721.linmusic.feature.artist.domain.ArtistInfo
import kotlinx.coroutines.flow.Flow

// 歌手数据仓储（artist 业务域）
interface ArtistRepository {

    // 获取热门歌手 —— 当前无外部调用者（仅被 getFavoriteArtists 内部以 apiService 直连方式复用），保留待后续确认取舍
    fun getTopArtists(): Flow<Result<List<Artist>>>

    // 获取最爱的歌手（优先已关注，兜底热门）
    fun getFavoriteArtists(): Flow<Result<List<ArtistInfo>>>

    fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>>

    fun getArtistAlbums(artistId: Long, limit: Int = 10): Flow<Result<List<ArtistAlbum>>>

    // 获取艺人粉丝数（每月听众数）
    fun getArtistFansCount(artistId: Long): Flow<Result<Long>>

    // 获取艺人热门歌曲（50首）
    fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>>

    // 收藏/关注歌手
    fun subscribeArtist(artistId: Long, subscribe: Boolean): Flow<Result<Unit>>

    // 检查是否已关注歌手
    fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>>

    fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>>
}
