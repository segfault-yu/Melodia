package com.lin0721.linmusic.feature.create.data

import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.api.PlaylistCreateRequest
import com.lin0721.linmusic.core.api.PlaylistDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class CreateRepositoryImpl(
    private val apiService: NeteaseApiService
) : CreateRepository {

    override fun createPlaylist(name: String, privacy: Int): Flow<Result<PlaylistDetail>> = flow {
        val response = apiService.createPlaylist(PlaylistCreateRequest(name = name, privacy = privacy))
        if (response.isSuccess && response.playlist != null) {
            emit(Result.success(response.playlist))
        } else {
            emit(Result.failure(Exception("Failed to create playlist: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}
