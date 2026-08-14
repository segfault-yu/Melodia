package com.lin0721.linmusic.feature.artist.ui

import com.lin0721.linmusic.core.model.ArtistAlbum
import com.lin0721.linmusic.core.model.ArtistDetailInfo
import com.lin0721.linmusic.core.model.ArtistInfo
import com.lin0721.linmusic.core.model.Track

// 歌手详情页 UI 状态
sealed interface ArtistUiState {
    data object Loading : ArtistUiState

    // 加载成功，携带歌手详情与聚合的热门歌曲/专辑/相似歌手
    data class Success(
        val artist: ArtistDetailInfo,
        val isFollowed: Boolean,
        val fansCount: Long,
        val topSongs: List<Track>,
        val albums: List<ArtistAlbum>,
        val similarArtists: List<ArtistInfo>
    ) : ArtistUiState

    data class Error(val message: String) : ArtistUiState
}
