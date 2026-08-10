package com.lin0721.linmusic.core.player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BluetoothReceiver : BroadcastReceiver(), KoinComponent {

    private val playerManager: PlayerManager by inject()
    private val settingsPreferences: SettingsPreferences by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            scope.launch {
                val carModeEnabled = settingsPreferences.carMode.first()
                if (carModeEnabled) {
                    // 延迟 2 秒以等待系统音频路由切换至蓝牙，防止手机外放爆音
                    delay(2000)
                    playerManager.resume()
                }
            }
        }
    }
}
