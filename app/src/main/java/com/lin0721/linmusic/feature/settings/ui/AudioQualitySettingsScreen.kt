package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.SurfaceDark

@Composable
fun AudioQualitySettingsView(viewModel: SettingsViewModel) {
    val wifiQuality by viewModel.wifiQuality.collectAsStateWithLifecycle()
    val mobileQuality by viewModel.mobileQuality.collectAsStateWithLifecycle()

    
    var qualityDialogTarget by remember { mutableStateOf<String?>(null) }

    // 渲染音质的子设置项
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroupCard("默认音质") {
                SettingsRow(
                    title = "Wi-Fi 环境播放音质",
                    subtitle = getQualityDisplayName(wifiQuality),
                    onClick = { qualityDialogTarget = "wifi" }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsRow(
                    title = "移动网络环境播放音质",
                    subtitle = getQualityDisplayName(mobileQuality),
                    onClick = { qualityDialogTarget = "mobile" }
                )
            }
        }

    }

    // 音质单选选择弹窗
    if (qualityDialogTarget != null) {
        val qualities = listOf(
            "standard" to "标准音质",
            "exhigh" to "极高音质",
            "lossless" to "无损音质 (FLAC)",
            "hires" to "Hi-Res 无损",
            "jymaster" to "超清母带"
        )
        val isWifi = qualityDialogTarget == "wifi"
        val activeQuality = if (isWifi) wifiQuality else mobileQuality
        
        Dialog(onDismissRequest = { qualityDialogTarget = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isWifi) "选择 Wi-Fi 播放音质" else "选择移动网络播放音质",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    qualities.forEach { pair ->
                        val key = pair.first
                        val label = pair.second
                        val isSelected = activeQuality == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isWifi) {
                                        viewModel.updateWifiQuality(key)
                                    } else {
                                        viewModel.updateMobileQuality(key)
                                    }
                                    qualityDialogTarget = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = if (isSelected) NeteaseRed else Color.White, fontSize = 15.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeteaseRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
