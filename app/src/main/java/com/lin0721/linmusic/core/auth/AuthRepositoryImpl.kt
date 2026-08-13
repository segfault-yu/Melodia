package com.lin0721.linmusic.core.auth

import com.lin0721.linmusic.core.api.AccountInfoResponse
import com.lin0721.linmusic.core.api.NeteaseApiService
import com.lin0721.linmusic.core.network.apiFlow
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val apiService: NeteaseApiService
) : AuthRepository {

    override fun getAccountInfo(): Flow<Result<AccountInfoResponse>> = apiFlow(
        request = { apiService.getAccountInfo() },
        isSuccess = { it.code == 200 },
        code = { it.code },
        transform = { it }
    )

    override fun logout(): Flow<Result<Unit>> = apiFlow(
        request = { apiService.logoutApi() },
        isSuccess = { it.isSuccess },
        code = { it.code },
        transform = { Unit }
    )
}
