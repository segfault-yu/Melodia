package com.lin0721.linmusic.data.repository

import com.lin0721.linmusic.core.api.PlaylistDetail
import kotlinx.coroutines.flow.Flow

// 音乐数据层接口
interface MusicRepository {

    // 创建歌单
    fun createPlaylist(name: String, privacy: Int = 0): Flow<Result<PlaylistDetail>>

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
