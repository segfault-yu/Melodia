package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.core.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val apiService: NeteaseApiService
) : MusicRepository {

    override fun createPlaylist(name: String, privacy: Int): Flow<Result<PlaylistDetail>> = flow {
        val response = apiService.createPlaylist(com.lin0721.linmusic.core.api.PlaylistCreateRequest(name = name, privacy = privacy))
        if (response.isSuccess && response.playlist != null) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to create playlist: code ${response.code}")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

}
