package com.lin0721.linmusic.core.auth

import com.lin0721.linmusic.core.api.AccountInfoResponse
import kotlinx.coroutines.flow.Flow

// 登录态与账号信息（跨业务域共享能力）
interface AuthRepository {

    // 获取当前登录账号信息
    fun getAccountInfo(): Flow<Result<AccountInfoResponse>>

    // 退出登录并清理网络会话
    fun logout(): Flow<Result<Unit>>
}
