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

// 用户基本信息模型
@Serializable
data class UserProfile(
    val uid: Long,
    val nickname: String,
    val avatarUrl: String
)

// 用户信息持久化管理 (DataStore + JSON)
class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_USER_PROFILE = stringPreferencesKey("user_profile_json")
        private val KEY_COOKIES = stringPreferencesKey("user_cookies")
    }

    private val json = Json { ignoreUnknownKeys = true }

    // 读取用户信息
    val userProfile: Flow<UserProfile?> = context.userDataStore.data.map { prefs ->
        prefs[KEY_USER_PROFILE]?.let { jsonStr ->
            runCatching { json.decodeFromString<UserProfile>(jsonStr) }.getOrNull()
        }
    }

    // 读取 Cookie
    val cookies: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[KEY_COOKIES]
    }

    // 保存用户信息
    suspend fun saveUserProfile(profile: UserProfile) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_USER_PROFILE] = json.encodeToString(profile)
        }
    }

    // 保存 Cookie
    suspend fun saveCookies(cookies: String) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_COOKIES] = cookies
        }
    }

    // 清除用户信息与 Cookie（退出登录）
    suspend fun clearUserProfile() {
        context.userDataStore.edit { prefs ->
            prefs.remove(KEY_USER_PROFILE)
            prefs.remove(KEY_COOKIES)
        }
    }
}
