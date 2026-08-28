package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.SurfaceDark
import com.lin0721.linmusic.core.ui.theme.SurfaceLight
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

@Composable
fun AboutSettingsView(viewModel: SettingsViewModel) {
    val context = LocalContext.current

    // 渲染“关于”子设置项
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(MelodiaSpacing.md),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Melodia Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Version 1.0.0", color = TextGray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            SettingsGroupCard("应用说明与协议") {
                Text(
                    text = "Melodia 是一款基于 Jetpack Compose 构建的第三方网易云音乐播放器。\n\n" +
                            "本项目基于开源协议发布。",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text("开源协议 (MIT LICENSE)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files...",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .background(SurfaceLight, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
            }
        }

        item {
            SettingsGroupCard("诊断与日志") {
                val logLevelStr by viewModel.logLevel.collectAsStateWithLifecycle()
                val currentLogLevel = runCatching { AppLogger.LogLevel.valueOf(logLevelStr) }
                    .getOrDefault(AppLogger.LogLevel.WARN)
                var showLogLevelDialog by remember { mutableStateOf(false) }

                SettingsRow(
                    title = "日志级别",
                    subtitle = "当前: ${logLevelLabel(currentLogLevel)}，越详细越利于排查问题",
                    onClick = { showLogLevelDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsRow(
                    title = "导出并分享日志",
                    subtitle = "当应用发生故障时，可将本地运行日志导出",
                    onClick = {
                        exportAndShareLogs(context)
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SettingsRow(
                    title = "清空日志文件",
                    subtitle = "清除本地保存的运行日志，清空后将无法再导出",
                    onClick = {
                        if (AppLogger.clearLogs()) {
                            ToastManager.showToast("日志已清空")
                        } else {
                            ToastManager.showToast("清空日志失败")
                        }
                    }
                )

                if (showLogLevelDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogLevelDialog = false },
                        title = { Text("日志级别", color = Color.White) },
                        text = {
                            Column {
                                AppLogger.LogLevel.entries.forEach { level ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateLogLevel(level)
                                                showLogLevelDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentLogLevel == level,
                                            onClick = {
                                                viewModel.updateLogLevel(level)
                                                showLogLevelDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = NeteaseRed,
                                                unselectedColor = TextGray
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = logLevelLabel(level), color = Color.White, fontSize = 16.sp)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLogLevelDialog = false }) {
                                Text("取消", color = NeteaseRed)
                            }
                        },
                        containerColor = SurfaceDark
                    )
                }
            }
        }

        item {
            SettingsGroupCard("特别鸣谢") {
                Text(
                    text = "本项目的开发与运行离不开以下优秀开源项目的支持：\n\n" +
                            "• NeteaseCloudMusicApiEnhanced (api-enhanced 引擎)\n" +
                            "• Jetpack Compose & Media3\n" +
                            "• Retrofit & OkHttp\n" +
                            "• Koin\n" +
                            "• Coil\n" +
                            "• Haze",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

private const val TAG = "AboutSettingsScreen"

private fun logLevelLabel(level: AppLogger.LogLevel): String = when (level) {
    AppLogger.LogLevel.DEBUG -> "详细"
    AppLogger.LogLevel.INFO -> "标准"
    AppLogger.LogLevel.WARN -> "精简"
    AppLogger.LogLevel.ERROR -> "仅错误"
}

fun exportAndShareLogs(context: Context) {
    val logFiles = AppLogger.getLogFiles()
    if (logFiles.isEmpty()) {
        ToastManager.showToast("暂未产生诊断日志哦！")
        return
    }
    try {
        val authority = "${context.packageName}.fileprovider"
        val fileUris = ArrayList(logFiles.map { FileProvider.getUriForFile(context, authority, it) })
        val action = if (fileUris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
        val shareIntent = Intent(action).apply {
            type = "text/plain"
            if (fileUris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, fileUris[0])
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
            }
            putExtra(Intent.EXTRA_SUBJECT, "Melodia 诊断日志反馈")
            putExtra(Intent.EXTRA_TEXT, "这是来自用户的 Melodia 诊断日志文件。")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授权目标 App 读取该 URI
        }
        context.startActivity(Intent.createChooser(shareIntent, "导出并提交日志"))
    } catch (e: Exception) {
        AppLogger.e(TAG, "日志导出分享失败", e)
        ToastManager.showToast("日志导出分享失败了...")
    }
}
