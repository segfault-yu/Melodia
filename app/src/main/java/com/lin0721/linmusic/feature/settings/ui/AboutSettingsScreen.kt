package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.SurfaceLight
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

@Composable
fun AboutSettingsView() {
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
                SettingsRow(
                    title = "导出并分享日志",
                    subtitle = "当应用发生故障时，可将本地运行日志导出",
                    onClick = {
                        exportAndShareLogs(context)
                    }
                )
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

fun exportAndShareLogs(context: Context) {
    val logFile = com.lin0721.linmusic.core.log.AppLogger.getLogFile()
    if (logFile == null || !logFile.exists()) {
        ToastManager.showToast("暂未产生诊断日志哦！")
        return
    }
    try {
        val authority = "${context.packageName}.fileprovider"
        val fileUri = FileProvider.getUriForFile(context, authority, logFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "Melodia 诊断日志反馈")
            putExtra(Intent.EXTRA_TEXT, "这是来自用户的 Melodia 诊断日志文件。")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授权目标 App 读取该 URI
        }
        context.startActivity(Intent.createChooser(shareIntent, "导出并提交日志"))
    } catch (e: Exception) {
        ToastManager.showToast("日志导出分享失败了...")
    }
}
