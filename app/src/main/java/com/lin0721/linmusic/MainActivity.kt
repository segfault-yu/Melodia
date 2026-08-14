package com.lin0721.linmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.log.CrashHandler
import com.lin0721.linmusic.core.player.FloatingLyricService
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.ui.theme.MelodiaTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsPreferences: SettingsPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化日志与崩溃收集系统
        AppLogger.init(this)
        CrashHandler.init(this)
        enableEdgeToEdge()

        // 监听悬浮歌词开关
        lifecycleScope.launch {
            settingsPreferences.showDesktopLrc.collectLatest { enabled ->
                if (enabled) {
                    startFloatingLyricOrRequestPermission()
                } else {
                    stopService(Intent(this@MainActivity, FloatingLyricService::class.java))
                }
            }
        }

        setContent {
            MelodiaTheme {
                MelodiaApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val enabled = settingsPreferences.showDesktopLrc.first()
            if (enabled && android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                startService(Intent(this@MainActivity, FloatingLyricService::class.java))
            }
        }
    }

    // 有悬浮窗权限则直接启动桌面歌词，否则引导用户前往系统设置授权
    private fun startFloatingLyricOrRequestPermission() {
        if (android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingLyricService::class.java))
            return
        }
        android.widget.Toast.makeText(
            this,
            "请开启悬浮窗权限以显示桌面歌词",
            android.widget.Toast.LENGTH_LONG
        ).show()
        startActivity(
            Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
        )
    }
}
