package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.api.PersonalizedData
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
            emit(Result.failure(Exception("API Error (Code: ${response.code})")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

}
