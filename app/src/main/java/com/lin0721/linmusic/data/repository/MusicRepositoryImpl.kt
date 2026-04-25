package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.PlaylistDetailRequest
import com.lin0721.linmusic.data.remote.api.PersonalizedData
import com.lin0721.linmusic.data.remote.api.SongUrlRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val apiService: NeteaseApiService
) : MusicRepository {

    override fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>> = flow {
        val response = apiService.getPersonalizedPlaylists()
        if (response.isSuccess) {
            emit(Result.success(PersonalizedData(playlists = response.result)))
        } else {
            emit(Result.failure(Exception("Failed to load personalized playlists: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>> = flow {
        val response = apiService.getPlaylistDetail(PlaylistDetailRequest(id = id))
        if (response.isSuccess && response.playlist != null) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to load playlist detail: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getSongUrl(songId: Long): Flow<Result<String>> = flow {
        val response = apiService.getSongUrl(SongUrlRequest(ids = "[$songId]"))

        if (response.isSuccess) {
            val songItem = response.data.firstOrNull()
            if (songItem != null && !songItem.url.isNullOrBlank()) {
                // 如果有 freeTrialInfo 且需要限制，这里可以抛错，目前允许试听
                emit(Result.success(songItem.url))
            } else {
                emit(Result.failure(Exception("无法获取播放链接，可能是 VIP 歌曲或无版权")))
            }
        } else {
            emit(Result.failure(Exception("API Error (Code: ${response.code})")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }
}
