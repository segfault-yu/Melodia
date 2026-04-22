package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.data.remote.api.NeteaseApiService
import com.lin0721.linmusic.data.remote.api.RecommendPlaylistData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val apiService: NeteaseApiService
) : MusicRepository {

    override fun getDailyRecommendPlaylists(): Flow<Result<RecommendPlaylistData>> = flow {
        val response = apiService.getDailyRecommendPlaylists()
        
        if (response.isSuccess) {
            // 注意：目前的 NeteaseResponse 没包含泛型 data 属性，
            // 真实联调时如果是扁平结构可能需要调整 Retrofit 响应模型，
            // 或者如果是嵌套结构只需 return Result.success(response.data)
            // 这里我们先暂时返回一个空对象以便走通架构
            emit(Result.success(RecommendPlaylistData()))
        } else {
            emit(Result.failure(Exception(response.msg ?: response.message ?: "Unknown API Error (Code: ${response.code})")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

}
