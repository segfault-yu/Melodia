package com.lin0721.linmusic.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NetworkSettingsView() {
    var wifiOnlyPlay by remember { mutableStateOf(false) }
    var mobileAlert by remember { mutableStateOf(true) }
    var useProxy by remember { mutableStateOf(false) }

    // 渲染网络设置的子设置项
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroupCard("网络连接参数") {
                SettingsSwitchRow(
                    title = "仅 Wi-Fi 网络下联网播放",
                    subtitle = "开启后，在移动网络环境将无法播放在线曲目",
                    checked = wifiOnlyPlay,
                    onCheckedChange = { wifiOnlyPlay = it }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "流量播放警告提示",
                    subtitle = "从 Wi-Fi 切换为移动数据时弹出提醒",
                    checked = mobileAlert,
                    onCheckedChange = { mobileAlert = it }
                )
            }
        }

        item {
            SettingsGroupCard("网络连接代理") {
                SettingsSwitchRow(
                    title = "使用代理服务器",
                    subtitle = "配置自定义网络代理进行数据解析",
                    checked = useProxy,
                    onCheckedChange = { useProxy = it }
                )
            }
        }
    }
}
