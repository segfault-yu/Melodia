package com.lin0721.linmusic.feature.artist.data

import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.network.AppError
import com.lin0721.linmusic.core.network.apiFlow
import com.lin0721.linmusic.feature.artist.domain.ArtistInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ArtistRepositoryImpl(
    private val apiService: ArtistApi
) : ArtistRepository {

    override fun getTopArtists(): Flow<Result<List<Artist>>> = apiFlow(
        request = { apiService.getTopArtists() },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.artists }
    )

    override fun getFavoriteArtists(): Flow<Result<List<ArtistInfo>>> = flow {
        var artists = emptyList<ArtistInfo>()

        // 尝试获取已关注歌手（实际返回: {"data":[...], "code":200}）
        try {
            val response = apiService.getArtistSublist()
            if (response.code == 200 && response.data.isNotEmpty()) {
                artists = response.data.map { dto ->
                    ArtistInfo(
                        id = dto.id,
                        name = dto.name,
                        avatarUrl = dto.img1v1Url.takeIf { it.isNotBlank() } ?: dto.picUrl
                    )
                }
            }
        } catch (_: Exception) {
            // 网络失败或服务器返回空体，进入备用流程
        }


        if (artists.isNotEmpty()) {
            emit(Result.success(artists))
            return@flow
        }

        // 备用：热门歌手榜单
        // 注意：emit 必须放在 try/catch 之外——.first() 等短路收集算子会在拿到首个值后
        // 向上抛内部取消信号，若 emit 处在 try 块内会被这里的 catch(Exception) 误捕获，
        // 导致再次 emit 时触发 "Flow exception transparency violated" 崩溃
        val fallbackResult = try {
            val response = apiService.getTopArtists()
            if (response.isSuccess && response.artists.isNotEmpty()) {
                artists = response.artists.map { dto ->
                    ArtistInfo(
                        id = dto.id,
                        name = dto.name,
                        avatarUrl = dto.img1v1Url.takeIf { it.isNotBlank() } ?: dto.picUrl
                    )
                }
                Result.success(artists)
            } else {
                Result.failure(AppError.BizError(response.code, null))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
        emit(fallbackResult)
    }

    override fun getArtistDetail(artistId: Long): Flow<Result<ArtistDetailInfo>> = apiFlow(
        request = { apiService.getArtistDetail(ArtistDetailRequest(id = artistId)) },
        isSuccess = { it.isSuccess && it.data?.artist != null },
        code = { it.code },
        transform = { it.data!!.artist!! }
    )

    override fun getArtistAlbums(artistId: Long, limit: Int): Flow<Result<List<ArtistAlbum>>> = apiFlow(
        request = { apiService.getArtistAlbums(id = artistId, body = ArtistAlbumRequest(limit = limit)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.hotAlbums }
    )

    // 获取歌手粉丝数量
    override fun getArtistFansCount(artistId: Long): Flow<Result<Long>> = apiFlow(
        request = { apiService.getArtistFollowCount(ArtistFollowCountRequest(id = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { val data = it.data; data?.fansCnt ?: data?.fansCount ?: data?.fans ?: 0L }
    )

    override fun getArtistTopSongs(artistId: Long): Flow<Result<List<Track>>> = apiFlow(
        request = { apiService.getArtistTopSongs(ArtistTopSongsRequest(id = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.songs }
    )

    override fun subscribeArtist(artistId: Long, subscribe: Boolean): Flow<Result<Unit>> = apiFlow(
        request = {
            apiService.subscribeArtist(
                op = if (subscribe) "sub" else "unsub",
                body = ArtistSubscriptionRequest(
                    artistId = artistId,
                    artistIds = "[$artistId]"
                )
            )
        },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { Unit }
    )

    override fun checkArtistFollowed(artistId: Long): Flow<Result<Boolean>> = apiFlow(
        request = { apiService.getArtistDetailDynamic(ArtistFollowCountRequest(id = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { it.isFollow }
    )

    override fun getSimilarArtists(artistId: Long): Flow<Result<List<ArtistInfo>>> = apiFlow(
        request = { apiService.getSimiArtists(SimiArtistRequest(artistid = artistId)) },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { response ->
            response.artists.map { artist ->
                ArtistInfo(
                    id = artist.id,
                    name = artist.name,
                    avatarUrl = artist.img1v1Url.ifEmpty { artist.picUrl }
                )
            }
        }
    )
}
