package com.lin0721.linmusic.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.TextGray

@Composable
fun NetworkSettingsView(viewModel: SettingsViewModel) {
    val useRealIp by viewModel.useRealIp.collectAsStateWithLifecycle()
    val realIpValue by viewModel.realIpValue.collectAsStateWithLifecycle()
    val wifiOnlyPlay by viewModel.wifiOnlyPlay.collectAsStateWithLifecycle()
    val mobileAlert by viewModel.mobileAlert.collectAsStateWithLifecycle()
    val useProxy by viewModel.useProxy.collectAsStateWithLifecycle()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroupCard("网络连接 parameters") {
                SettingsSwitchRow(
                    title = "仅 Wi-Fi 网络下联网播放",
                    subtitle = "开启后，在移动网络环境将无法播放在线曲目",
                    checked = wifiOnlyPlay,
                    onCheckedChange = { viewModel.updateWifiOnlyPlay(it) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "流量播放警告提示",
                    subtitle = "从 Wi-Fi 切换为移动数据时弹出提醒",
                    checked = mobileAlert,
                    onCheckedChange = { viewModel.updateMobileAlert(it) }
                )
            }
        }

        item {
            SettingsGroupCard("网络代理") {
                SettingsSwitchRow(
                    title = "使用国内 IP 地址",
                    subtitle = "在海外IP可能会受到限制，可开启此处尝试解决",
                    checked = useRealIp,
                    onCheckedChange = { viewModel.updateUseRealIp(it) }
                )

                if (useRealIp) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text("真实 IP 地址", color = Color.White, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("可在此处输入国内 IP，不填写则为随机", color = TextGray, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (realIpValue.isEmpty()) {
                                Text("IP 127.0.0.1", color = TextGray, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = realIpValue,
                                onValueChange = { viewModel.updateRealIpValue(it) },
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                cursorBrush = SolidColor(NeteaseRed),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                SettingsSwitchRow(
                    title = "使用代理服务器",
                    subtitle = "配置自定义网络代理进行数据解析",
                    checked = useProxy,
                    onCheckedChange = { viewModel.updateUseProxy(it) }
                )
            }
        }
    }
}
