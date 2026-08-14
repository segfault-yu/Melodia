package com.lin0721.linmusic.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 网络策略守卫：监听 Wi-Fi 切换到移动网络，并按“仅 Wi-Fi 播放”设置拦截联网起播
class PlaybackNetworkGuard(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsPreferences: SettingsPreferences,
    private val isPlaying: () -> Boolean,
    private val onPauseRequested: () -> Unit
) {

    private var lastWasWifi = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, capabilities)
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            if (lastWasWifi && isMobile && !isWifi) {
                scope.launch(Dispatchers.Main) {
                    val alertEnabled = settingsPreferences.mobileAlert.first()
                    val wifiOnly = settingsPreferences.wifiOnlyPlay.first()

                    if (wifiOnly) {
                        onPauseRequested()
                        Toast.makeText(context, "已为暂停播放（开启了“仅Wi-Fi联网播放”）", Toast.LENGTH_LONG).show()
                    } else if (alertEnabled && isPlaying()) {
                        Toast.makeText(context, "已切换至移动网络播放", Toast.LENGTH_LONG).show()
                    }
                }
            }
            if (isWifi) {
                lastWasWifi = true
            } else if (isMobile) {
                lastWasWifi = false
            }
        }
    }

    // 注册网络回调
    fun register() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, networkCallback)
    }

    fun unregister() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        kotlin.runCatching { cm?.unregisterNetworkCallback(networkCallback) }
    }

    // 移动网络下开启了“仅 Wi-Fi 播放”时拦截起播并提示
    suspend fun blockPlaybackOnMobile(): Boolean {
        val wifiOnly = settingsPreferences.wifiOnlyPlay.first()
        if (wifiOnly && isMobileNetwork()) {
            Toast.makeText(context, "当前处于移动网络，已开启“仅Wi-Fi下联网播放”", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }

    private fun isMobileNetwork(): Boolean {
        return kotlin.runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        }.getOrDefault(false)
    }
}
