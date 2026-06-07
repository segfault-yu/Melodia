package com.lin0721.linmusic.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackDownloadSettingsView() {
    var autoPlayNext by remember { mutableStateOf(true) }
    var streamCacheEnabled by remember { mutableStateOf(false) }
    var playMode by remember { mutableStateOf("列表循环") }
    
    // 渲染播放与下载的子设置项
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroupCard("播放参数") {
                SettingsSwitchRow(
                    title = "自动播放推荐新歌",
                    subtitle = "当前曲目播放完毕后自动接入相似推荐",
                    checked = autoPlayNext,
                    onCheckedChange = { autoPlayNext = it }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsRow(
                    title = "默认播放顺序",
                    subtitle = playMode,
                    onClick = {
                        playMode = if (playMode == "列表循环") "随机播放" else "列表循环"
                    }
                )
            }
        }
        item {
            SettingsGroupCard("下载与缓存") {
                SettingsSwitchRow(
                    title = "边听边存",
                    subtitle = "在线播放歌曲时自动缓存到本地",
                    checked = streamCacheEnabled,
                    onCheckedChange = { streamCacheEnabled = it }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsRow(
                    title = "下载目录",
                    subtitle = "/Android/data/com.lin0721.linmusic/files/Download",
                    onClick = {}
                )
            }
        }
    }
}
