package com.lin0721.linmusic.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

/**
 * 用户基本信息数据模型
 */
@Serializable
data class UserProfile(
    val uid: Long,
    val nickname: String,
    val avatarUrl: String
)

/**
 * 用户信息持久化管理
 *
 * 通过 DataStore 保存和读取 UserProfile，序列化为 JSON 字符串存储。
 */
class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_USER_PROFILE = stringPreferencesKey("user_profile_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 读取用户信息（响应式流）
     */
    val userProfile: Flow<UserProfile?> = context.userDataStore.data.map { prefs ->
        prefs[KEY_USER_PROFILE]?.let { jsonStr ->
            runCatching { json.decodeFromString<UserProfile>(jsonStr) }.getOrNull()
        }
    }

    /**
     * 保存用户信息
     */
    suspend fun saveUserProfile(profile: UserProfile) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_USER_PROFILE] = json.encodeToString(profile)
        }
    }

    /**
     * 清除用户信息（退出登录）
     */
    suspend fun clearUserProfile() {
        context.userDataStore.edit { prefs ->
            prefs.remove(KEY_USER_PROFILE)
        }
    }
}
