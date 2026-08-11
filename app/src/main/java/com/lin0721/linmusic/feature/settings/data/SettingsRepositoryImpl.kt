package com.lin0721.linmusic.feature.settings.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType

class SettingsRepositoryImpl(
    private val apiService: SettingsApi
) : SettingsRepository {

    override fun getUserLevel(): Flow<Result<UserLevelData>> = flow {
        val response = apiService.getUserLevel()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("获取用户等级失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getVipInfo(): Flow<Result<VipInfoData>> = flow {
        val response = apiService.getVipInfo()
        if (response.isSuccess && response.data != null) {
            emit(Result.success(response.data))
        } else {
            emit(Result.failure(Exception("获取VIP信息失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun getUserBindings(uid: Long): Flow<Result<List<UserBindingItem>>> = flow {
        val response = apiService.getUserBindings(UserBindingRequest(uid = uid))
        if (response.isSuccess) {
            emit(Result.success(response.bindings))
        } else {
            emit(Result.failure(Exception("获取账号绑定信息失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun updateUserProfile(
        nickname: String,
        gender: Int,
        birthday: Long,
        province: Int,
        city: Int,
        signature: String
    ): Flow<Result<Unit>> = flow {
        val response = apiService.updateUserProfile(
            UserProfileUpdateRequest(
                nickname = nickname,
                gender = gender,
                birthday = birthday,
                province = province,
                city = city,
                signature = signature
            )
        )
        if (response.isSuccess) {
            emit(Result.success(Unit))
        } else {
            emit(Result.failure(Exception("修改个人资料失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun checkNickname(nickname: String): Flow<Result<Boolean>> = flow {
        val response = apiService.checkNickname(NicknameCheckRequest(nickname = nickname))
        if (response.isSuccess) {
            emit(Result.success(response.duplicated))
        } else {
            emit(Result.failure(Exception("检查昵称重名失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun dailySignin(type: Int): Flow<Result<Int>> = flow {
        val response = apiService.dailySignin(DailySigninRequest(type = type))
        if (response.isSuccess) {
            emit(Result.success(response.point))
        } else if (response.code == -2) {
            emit(Result.success(0))
        } else {
            emit(Result.failure(Exception(response.msg ?: "签到失败")))
        }
    }.catch { e -> emit(Result.failure(e)) }

    override fun uploadAvatar(file: java.io.File): Flow<Result<String>> = flow {
        val mediaType = "image/*".toMediaType()
        val requestFile = okhttp3.RequestBody.create(mediaType, file)
        val body = okhttp3.MultipartBody.Part.createFormData("imgFile", file.name, requestFile)
        val response = apiService.uploadAvatar(body)
        if (response.isSuccess) {
            emit(Result.success(response.url ?: ""))
        } else {
            emit(Result.failure(Exception("更换头像失败: code ${response.code}")))
        }
    }.catch { e -> emit(Result.failure(e)) }
}
