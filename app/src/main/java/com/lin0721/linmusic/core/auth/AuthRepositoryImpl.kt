package com.lin0721.linmusic.core.auth

import com.lin0721.linmusic.core.api.AccountInfoResponse
import com.lin0721.linmusic.core.api.NeteaseApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class AuthRepositoryImpl(
    private val apiService: NeteaseApiService
) : AuthRepository {

    override fun getAccountInfo(): Flow<Result<AccountInfoResponse>> = flow {
        val response = apiService.getAccountInfo()
        if (response.code == 200) {
            emit(Result.success(response))
        } else {
            emit(Result.failure(Exception("Failed to get account info: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun logout(): Flow<Result<Unit>> = flow {
        val response = apiService.logoutApi()
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("退出登录接口异常")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}
