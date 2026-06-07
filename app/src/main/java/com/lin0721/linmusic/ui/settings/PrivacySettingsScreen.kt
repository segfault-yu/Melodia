package com.lin0721.linmusic.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.TextGray

@Composable
fun PrivacySettingsView(viewModel: SettingsViewModel) {
    val defaultPlaylistPrivate by viewModel.defaultPlaylistPrivate.collectAsStateWithLifecycle()
    val userBindings by viewModel.userBindings.collectAsStateWithLifecycle()
    var profileInvisible by remember { mutableStateOf(false) }

    // 渲染隐私设置的子设置项
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        item {
            SettingsGroupCard("歌单与社交隐私") {
                SettingsSwitchRow(
                    title = "新建歌单默认私密",
                    subtitle = "开启后，新建的歌单将默认设置为仅自己可见",
                    checked = defaultPlaylistPrivate,
                    onCheckedChange = { viewModel.updateDefaultPlaylistPrivate(it) }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsSwitchRow(
                    title = "不向他人展示我的听歌排行",
                    subtitle = "隐藏主页听歌排行列表防止隐私泄露",
                    checked = profileInvisible,
                    onCheckedChange = { profileInvisible = it }
                )
            }
        }

        item {
            SettingsGroupCard("账号绑定") {
                if (userBindings.isEmpty()) {
                    Text(
                        text = "暂无账号绑定信息",
                        color = TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    userBindings.forEachIndexed { index, binding ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getBindingIcon(binding.type),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(binding.typeName, color = Color.White, fontSize = 14.sp)
                            }
                            Text(
                                text = if (binding.expired) "已过期" else "已绑定",
                                color = if (binding.expired) NeteaseRed else Color(0xFF10B981),
                                fontSize = 12.sp
                            )
                        }
                        if (index < userBindings.lastIndex) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}
