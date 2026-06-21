package com.lin0721.linmusic.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.TextGray

@Composable
fun LyricsSettingsView(viewModel: SettingsViewModel) {
    val showDesktopLrc by viewModel.showDesktopLrc.collectAsStateWithLifecycle()
    val lyricTextSize by viewModel.lyricTextSize.collectAsStateWithLifecycle()
    val lyricTextColor by viewModel.lyricTextColor.collectAsStateWithLifecycle()

    var showSizeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    val sizeLabel = "${lyricTextSize} sp"
    val colorLabel = when (lyricTextColor) {
        "#FFFFFF" -> "白色"
        "#E03E3E" -> "网易红"
        "#10B981" -> "绿色"
        else -> lyricTextColor
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item {
                SettingsGroupCard("悬浮歌词") {
                    SettingsSwitchRow(
                        title = "启用桌面悬浮歌词",
                        subtitle = "返回桌面时以悬浮窗形态展示当前播放词句",
                        checked = showDesktopLrc,
                        onCheckedChange = { viewModel.updateShowDesktopLrc(it) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SettingsRow(
                        title = "悬浮歌词字号",
                        subtitle = sizeLabel,
                        onClick = { showSizeDialog = true }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SettingsRow(
                        title = "悬浮歌词颜色",
                        subtitle = colorLabel,
                        onClick = { showColorDialog = true }
                    )
                }
            }
        }

        // 字号选择 Dialog
        if (showSizeDialog) {
            AlertDialog(
                onDismissRequest = { showSizeDialog = false },
                title = { Text("选择悬浮歌词字号", color = Color.White) },
                text = {
                    Column {
                        val options = listOf(12, 14, 16, 18, 20)
                        options.forEach { size ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateLyricTextSize(size)
                                        showSizeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lyricTextSize == size,
                                    onClick = {
                                        viewModel.updateLyricTextSize(size)
                                        showSizeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeteaseRed,
                                        unselectedColor = TextGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${size} sp", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSizeDialog = false }) {
                        Text("取消", color = NeteaseRed)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // 颜色选择 Dialog
        if (showColorDialog) {
            AlertDialog(
                onDismissRequest = { showColorDialog = false },
                title = { Text("选择悬浮歌词颜色", color = Color.White) },
                text = {
                    Column {
                        val options = listOf(
                            "#FFFFFF" to "白色",
                            "#E03E3E" to "网易红",
                            "#10B981" to "绿色"
                        )
                        options.forEach { (colorCode, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateLyricTextColor(colorCode)
                                        showColorDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lyricTextColor == colorCode,
                                    onClick = {
                                        viewModel.updateLyricTextColor(colorCode)
                                        showColorDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeteaseRed,
                                        unselectedColor = TextGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label, color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorDialog = false }) {
                        Text("取消", color = NeteaseRed)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
