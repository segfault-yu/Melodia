package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.core.api.PlaylistDetail
import com.lin0721.linmusic.core.api.Track
import kotlinx.coroutines.flow.Flow

// 单个字符/单词的耗时元数据
data class WordInfo(
    val text: String,
    val startOffsetMs: Long,  // 相对于整行歌词起始时间的偏移毫秒数
    val durationMs: Long      // 该字/词的持续发音毫秒数
)

// 歌词行领域模型
data class LyricLine(
    val timeMs: Long,
    val durationMs: Long = 0, // 新增：整行歌词的持续发音时间
    val text: String,
    val translation: String? = null,
    val words: List<WordInfo> = emptyList() // 如果是普通LRC则此列表为空；YRC则填入单字列表
)

// 音乐数据层接口
interface MusicRepository {

    // 获取歌单详情
    fun getPlaylistDetail(id: Long): Flow<Result<PlaylistDetail>>

    // 获取专辑详情，映射至统一领域模型 PlaylistDetail
    fun getAlbumDetail(id: Long): Flow<Result<PlaylistDetail>>

    // 获取歌曲播放链接
    fun getSongUrl(songId: Long): Flow<Result<String>>

    // 创建歌单
    fun createPlaylist(name: String, privacy: Int = 0): Flow<Result<PlaylistDetail>>

    // ================== 歌词 ==================

    // 获取歌曲歌词（已解析 LRC 格式）
    fun getLyrics(songId: Long): Flow<Result<List<LyricLine>>>

    // ================== 歌曲详情 ==================

    fun getSongDetail(songId: Long): Flow<Result<Track>>

    // ================== 相似歌曲 ==================

    fun getSimilarSongs(songId: Long): Flow<Result<List<Track>>>

    // ================== 智能推荐 ==================

    fun getIntelligenceSongs(songId: Long, playlistId: Long): Flow<Result<List<Track>>>

    // ================== 红心/喜欢 ==================

    fun getLikedSongIds(uid: Long): Flow<Result<List<Long>>>

    fun likeSong(songId: Long, like: Boolean): Flow<Result<Unit>>

    // 歌单歌曲添加/删除操作
    fun manipulatePlaylistTracks(op: String, playlistId: Long, trackId: Long): Flow<Result<Unit>>

    // 收藏/取消收藏歌单
    fun subscribePlaylist(playlistId: Long, subscribe: Boolean): Flow<Result<Unit>>


    // ================== 百科与乐谱详情 ==================
    // 获取合并后的歌曲详情与百科信息
    fun getSongWiki(songId: Long): Flow<Result<SongWikiData>>

    // ================== 设置和隐私扩展 ==================
    // 获取用户等级信息
    fun getUserLevel(): Flow<Result<com.lin0721.linmusic.core.api.UserLevelData>>

    // 获取 VIP 状态信息
    fun getVipInfo(): Flow<Result<com.lin0721.linmusic.core.api.VipInfoData>>

    // 获取账号绑定信息
    fun getUserBindings(uid: Long): Flow<Result<List<com.lin0721.linmusic.core.api.UserBindingItem>>>

    // 修改用户个人资料
    fun updateUserProfile(nickname: String, gender: Int, birthday: Long, province: Int, city: Int, signature: String): Flow<Result<Unit>>

    // 检查昵称可用性
    fun checkNickname(nickname: String): Flow<Result<Boolean>>

    // 每日签到
    fun dailySignin(type: Int): Flow<Result<Int>> // 返回签到获得的积分数

    // 上传并更换头像
    fun uploadAvatar(file: java.io.File): Flow<Result<String>>
}

// 歌曲详情/百科信息领域模型
data class SongWikiData(
    val style: String = "",
    val album: String = "", 
    val language: String = "",      
    val publishTime: String = "",   
    val bpm: String = "",           
    val creators: String = "",      
    val entertainment: String = "", 
    val background: String = "",    
    val awards: String = ""         
)
