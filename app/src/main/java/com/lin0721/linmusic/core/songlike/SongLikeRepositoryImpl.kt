package com.lin0721.linmusic.core.songlike

import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow

class SongLikeRepositoryImpl(
    private val apiService: SongLikeApi
) : SongLikeRepository {

    override fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>> = apiFlow(
        request = { apiService.getLikedSongIds(LikeSongListRequest(uid = uid)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.ids }
    )

    override fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>> = apiFlow(
        request = { apiService.likeSong(LikeSongRequest(trackId = songId, like = like)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { Unit }
    )
}
