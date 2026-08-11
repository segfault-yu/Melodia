package com.lin0721.linmusic.feature.home.data

import com.lin0721.linmusic.core.contentfilter.ContentFilter
import com.lin0721.linmusic.feature.home.domain.ToplistInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class HomeRepositoryImpl(
    private val apiService: HomeApi,
    private val contentFilter: ContentFilter
) : HomeRepository {

    override fun getPersonalizedPlaylists(): Flow<Result<PersonalizedData>> = flow {
        val response = apiService.getPersonalizedPlaylists()
        if (response.isSuccess) {
            emit(Result.success(PersonalizedData(playlists = response.result)))
        } else {
            emit(Result.failure(Exception("Failed to load personalized playlists: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getRecentPlaylists(): Flow<Result<List<RecentPlayItem>>> = flow {
        val response = apiService.getRecentPlaylists()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data.list))
        } else {
            emit(Result.failure(Exception("Failed to load recent playlists: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getToplistDetail(): Flow<Result<List<ToplistInfo>>> = flow {
        val response = apiService.getToplistDetail()
        if (response.code == 200) {
            val domainList = response.list
                // 过滤封面图为空的无效榜单条目
                .filter { it.coverImgUrl.isNotBlank() && it.name.isNotBlank() }
                .map { dto ->
                    ToplistInfo(
                        id = dto.id,
                        name = dto.name,
                        coverUrl = "${dto.coverImgUrl}?param=300y300",
                        updateDesc = dto.updateFrequency,
                        topSongs = dto.tracks?.map { "${it.first} - ${it.second}" } ?: emptyList()
                    )
                }
            emit(Result.success(domainList))
        } else {
            emit(Result.failure(Exception("Failed to load toplist: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getDailyRecommendSongs(): Flow<Result<List<DailySong>>> = flow {
        val response = apiService.getDailyRecommendSongs()
        if (response.isSuccess && response.data != null) {
            val filteredSongs = contentFilter.filterBlockedArtists(response.data.dailySongs) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredSongs))
        } else {
            emit(Result.failure(Exception("Failed to load daily recommend songs: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getHistoryRecommendDates(): Flow<Result<List<String>>> = flow {
        val response = apiService.getHistoryRecommendDates()
        if (response.code == 200 && response.data != null) {
            emit(Result.success(response.data.list))
        } else {
            emit(Result.failure(Exception("Failed to load history dates: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getHistoryRecommendDetail(date: String): Flow<Result<List<DailySong>>> = flow {
        val response = apiService.getHistoryRecommendDetail(HistoryDetailRequest(date = date))
        if (response.code == 200 && response.data != null) {
            val filteredSongs = contentFilter.filterBlockedArtists(response.data.dailySongs) { it.ar.map { a -> a.id } }
            emit(Result.success(filteredSongs))
        } else {
            emit(Result.failure(Exception("Failed to load history detail: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}
