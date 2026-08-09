package com.lin0721.linmusic.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lin0721.linmusic.data.local.SettingsPreferences
import com.lin0721.linmusic.core.auth.UserPreferences
import com.lin0721.linmusic.core.api.UserBindingItem
import com.lin0721.linmusic.core.api.UserLevelData
import com.lin0721.linmusic.core.api.VipInfoData
import com.lin0721.linmusic.core.auth.AuthRepository
import com.lin0721.linmusic.data.repository.MusicRepository
import com.lin0721.linmusic.player.AudioCacheManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SettingsViewModel(
    private val musicRepository: MusicRepository,
    private val settingsPreferences: SettingsPreferences,
    private val userPreferences: UserPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ─── 本地偏合设置对外状态流动 ───
    val wifiQuality = settingsPreferences.wifiQuality.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "lossless"
    )

    val mobileQuality = settingsPreferences.mobileQuality.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "standard"
    )

    val useRealIp = settingsPreferences.useRealIp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val realIpValue = settingsPreferences.realIpValue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val defaultPlaylistPrivate = settingsPreferences.defaultPlaylistPrivate.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val streamCacheEnabled = settingsPreferences.streamCacheEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val audioCacheMaxSize = settingsPreferences.audioCacheMaxSize.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 512 * 1024 * 1024L
    )

    val autoPlayNext = settingsPreferences.autoPlayNext.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val playMode = settingsPreferences.playMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "loop"
    )

    val wifiOnlyPlay = settingsPreferences.wifiOnlyPlay.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val mobileAlert = settingsPreferences.mobileAlert.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val useProxy = settingsPreferences.useProxy.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val showDesktopLrc = settingsPreferences.showDesktopLrc.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val showLockscreen = settingsPreferences.showLockscreen.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val carMode = settingsPreferences.carMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val lyricTextSize = settingsPreferences.lyricTextSize.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 14
    )

    val lyricTextColor = settingsPreferences.lyricTextColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "#FFFFFF"
    )

    // ─── 服务端拉取数据状态 ───
    private val _userLevel = MutableStateFlow<UserLevelData?>(null)
    val userLevel = _userLevel.asStateFlow()

    private val _vipInfo = MutableStateFlow<VipInfoData?>(null)
    val vipInfo = _vipInfo.asStateFlow()

    private val _userBindings = MutableStateFlow<List<UserBindingItem>>(emptyList())
    val userBindings = _userBindings.asStateFlow()

    // 加载中状态与消息事件
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // 昵称重名实时检查防抖状态
    private val _nicknameInput = MutableStateFlow("")
    val nicknameInput = _nicknameInput.asStateFlow()

    private val _isNicknameDuplicated = MutableStateFlow<Boolean?>(null)
    val isNicknameDuplicated = _isNicknameDuplicated.asStateFlow()

    init {
        // 昵称输入实时防抖查重 (500ms 延迟)
        _nicknameInput
            .debounce(500)
            .filter { it.isNotBlank() }
            .distinctUntilChanged()
            .onEach { name ->
                val currentName = userPreferences.userProfile.first()?.nickname
                if (name == currentName) {
                    _isNicknameDuplicated.value = false
                    return@onEach
                }
                musicRepository.checkNickname(name).collect { result ->
                    result.onSuccess { duplicated ->
                        _isNicknameDuplicated.value = duplicated
                    }.onFailure {
                        _isNicknameDuplicated.value = null
                    }
                }
            }
            .launchIn(viewModelScope)

        // 初始化拉取远端账号数据
        fetchRemoteSettingsData()
    }

    // ─── 核心设置修改方法 ───

    fun updateWifiQuality(quality: String) {
        viewModelScope.launch {
            settingsPreferences.saveWifiQuality(quality)
        }
    }

    fun updateMobileQuality(quality: String) {
        viewModelScope.launch {
            settingsPreferences.saveMobileQuality(quality)
        }
    }

    fun updateUseRealIp(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveUseRealIp(enabled)
        }
    }

    fun updateRealIpValue(ip: String) {
        viewModelScope.launch {
            settingsPreferences.saveRealIpValue(ip)
        }
    }

    fun updateDefaultPlaylistPrivate(private: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveDefaultPlaylistPrivate(private)
        }
    }

    fun updateStreamCacheEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveStreamCacheEnabled(enabled)
        }
    }

    fun updateAudioCacheMaxSize(context: Context, size: Long) {
        viewModelScope.launch {
            settingsPreferences.saveAudioCacheMaxSize(size)
            AudioCacheManager.recreateCache(context, size)
        }
    }

    fun updateAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveAutoPlayNext(enabled)
        }
    }

    fun updatePlayMode(mode: String) {
        viewModelScope.launch {
            settingsPreferences.savePlayMode(mode)
        }
    }

    fun updateWifiOnlyPlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveWifiOnlyPlay(enabled)
        }
    }

    fun updateMobileAlert(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveMobileAlert(enabled)
        }
    }

    fun updateUseProxy(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveUseProxy(enabled)
        }
    }

    fun updateShowDesktopLrc(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveShowDesktopLrc(enabled)
        }
    }

    fun updateShowLockscreen(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveShowLockscreen(enabled)
        }
    }

    fun updateCarMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.saveCarMode(enabled)
        }
    }

    fun updateLyricTextSize(size: Int) {
        viewModelScope.launch {
            settingsPreferences.saveLyricTextSize(size)
        }
    }

    fun updateLyricTextColor(color: String) {
        viewModelScope.launch {
            settingsPreferences.saveLyricTextColor(color)
        }
    }

    fun onNicknameInputChanged(name: String) {
        _nicknameInput.value = name
        if (name.isBlank()) {
            _isNicknameDuplicated.value = null
        }
    }

    // ─── 服务端接口数据加载 ───

    fun fetchRemoteSettingsData() {
        viewModelScope.launch {
            _isLoading.value = true
            val profile = userPreferences.userProfile.first() ?: return@launch

            // 并发获取用户等级、VIP 信息和绑定第三方账号信息
            launch {
                musicRepository.getUserLevel().collect { res ->
                    res.onSuccess { _userLevel.value = it }
                }
            }
            launch {
                musicRepository.getVipInfo().collect { res ->
                    res.onSuccess { _vipInfo.value = it }
                }
            }
            launch {
                musicRepository.getUserBindings(profile.uid).collect { res ->
                    res.onSuccess { _userBindings.value = it }
                }
            }
            _isLoading.value = false
        }
    }

    // 每日签到
    fun executeDailySignin(type: Int = 0) {
        viewModelScope.launch {
            musicRepository.dailySignin(type).collect { res ->
                res.onSuccess { point ->
                    if (point > 0) {
                        _toastEvent.emit("签到成功！积分+$point")
                    } else {
                        _toastEvent.emit("今天已经签到过了")
                    }
                }.onFailure {
                    _toastEvent.emit("签到失败: ${it.message}")
                }
            }
        }
    }

    // 更换头像
    fun uploadUserAvatar(file: java.io.File) {
        viewModelScope.launch {
            _isLoading.value = true
            musicRepository.uploadAvatar(file).collect { res ->
                res.onSuccess { newUrl ->
                    // 更新本地用户元数据
                    val currentProfile = userPreferences.userProfile.first()
                    if (currentProfile != null) {
                        userPreferences.saveUserProfile(currentProfile.copy(avatarUrl = newUrl))
                    }
                    _toastEvent.emit("头像更新成功")
                }.onFailure {
                    _toastEvent.emit("头像更换失败: ${it.message}")
                }
                _isLoading.value = false
            }
        }
    }

    // 保存资料修改 (修改昵称及个性签名)
    fun saveProfileChanges(nickname: String, signature: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            musicRepository.updateUserProfile(
                nickname = nickname,
                gender = 1,
                birthday = 0L,
                province = 110000,
                city = 110101,
                signature = signature
            ).collect { res ->
                res.onSuccess {
                    // 更新本地 user profile nickname
                    val currentProfile = userPreferences.userProfile.first()
                    if (currentProfile != null) {
                        userPreferences.saveUserProfile(
                            currentProfile.copy(nickname = nickname)
                        )
                    }
                    _toastEvent.emit("资料保存成功")
                    onFinished(true)
                }.onFailure {
                    _toastEvent.emit("资料保存失败: ${it.message}")
                    onFinished(false)
                }
                _isLoading.value = false
            }
        }
    }

    // 退出登录，注销远端会话并清空本地 preferences 缓存
    fun executeLogout(onFinished: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout().collect { }
            userPreferences.clearUserProfile()
            _toastEvent.emit("已成功退出登录")
            onFinished()
        }
    }

    // ─── 深度缓存清理 ───

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearApplicationCache(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                // 0. 清理 ExoPlayer 媒体缓存
                AudioCacheManager.clearCache(context)

                // 1. 清理 Coil 图片缓存
                val imageLoader = coil.Coil.imageLoader(context)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()

                // 2. 递归清理应用的 cacheDir 临时缓存目录
                val cacheDir = context.cacheDir
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.forEach { file ->
                        file.deleteRecursively()
                    }
                }

                // 3. 递归清理外部缓存目录 (例如 ExoPlayer 等产生的媒体缓存)
                val extCacheDir = context.externalCacheDir
                if (extCacheDir != null && extCacheDir.exists() && extCacheDir.isDirectory) {
                    extCacheDir.listFiles()?.forEach { file ->
                        file.deleteRecursively()
                    }
                }
            }
            _toastEvent.emit("应用临时数据与图片缓存已清理完成")
            _isLoading.value = false
        }
    }

    // 工具辅助方法
    private fun getQualityDisplayName(quality: String): String {
        return when (quality) {
            "standard" -> "标准"
            "exhigh" -> "极高"
            "lossless" -> "无损 (FLAC)"
            "hires" -> "Hi-Res"
            "jyeffect" -> "高清环绕声"
            "sky" -> "沉浸环绕声"
            "jymaster" -> "超清母带"
            else -> quality
        }
    }
}
