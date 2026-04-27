package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.api.PlaylistDetail
import com.lin0721.linmusic.data.remote.api.PlaylistDetailRequest
import com.lin0721.linmusic.data.remote.api.PersonalizedData
import com.lin0721.linmusic.data.remote.api.SongUrlRequest
import com.lin0721.linmusic.data.remote.api.Artist
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
                emit(Result.success(songItem.url))
            } else {
                emit(Result.failure(Exception("无法获取播放链接：该歌曲可能需要开启 VIP 或其版权受限。")))
            }
        } else {
            emit(Result.failure(Exception("网易云 API 响应异常 (Code: ${response.code})，可能是风控拦截。")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun getTopArtists(): Flow<Result<List<Artist>>> = flow {
        val response = apiService.getTopArtists()
        if (response.isSuccess) {
            emit(Result.success(response.artists))
        } else {
            emit(Result.failure(Exception("API Error (Code: ${response.code})")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }
}
