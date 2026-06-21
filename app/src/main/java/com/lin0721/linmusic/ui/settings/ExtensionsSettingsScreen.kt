package com.lin0721.linmusic.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ExtensionsSettingsView(viewModel: SettingsViewModel) {
    val showLockscreen by viewModel.showLockscreen.collectAsStateWithLifecycle()
    val carMode by viewModel.carMode.collectAsStateWithLifecycle()

    // 渲染扩展模块的子设置项
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroupCard("悬浮与桌面") {
                SettingsSwitchRow(
                    title = "启用系统锁屏显示",
                    subtitle = "在锁屏界面展示播放控制器与歌词面板",
                    checked = showLockscreen,
                    onCheckedChange = { viewModel.updateShowLockscreen(it) }
                )
            }
        }

        item {
            SettingsGroupCard("设备与集成") {
                SettingsSwitchRow(
                    title = "车载模式蓝牙自动启动",
                    subtitle = "连接车载蓝牙设备时自动恢复媒体播放",
                    checked = carMode,
                    onCheckedChange = { viewModel.updateCarMode(it) }
                )
            }
        }
    }
}
