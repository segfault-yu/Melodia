package com.lin0721.linmusic.feature.search.data

import com.lin0721.linmusic.core.api.HomepageBlock
import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.feature.search.domain.SearchSongsResult
import kotlinx.coroutines.flow.Flow

// 搜索数据仓储（search 业务域）
interface SearchRepository {

    // 获取默认搜索词
    fun getDefaultSearchKeyword(): Flow<Result<String>>

    // 云搜索歌曲
    fun searchSongs(keyword: String, offset: Int = 0, limit: Int = 30): Flow<Result<SearchSongsResult>>

    // 获取热搜榜
    fun getHotSearches(): Flow<Result<List<HotSearch>>>

    // 获取精品歌单标签（含封面图）
    fun getPlaylistTags(): Flow<Result<List<PlaylistTag>>>

    // 获取发现页（Search Screen）原始区块结构 —— 当前无调用者，保留待后续确认取舍
    fun getDiscoveryBlocks(refresh: Boolean = true, cursor: String? = null): Flow<Result<List<HomepageBlock>>>
}
