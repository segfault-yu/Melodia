package com.lin0721.linmusic.ui.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.TextGray

@Composable
fun StorageSettingsView(viewModel: SettingsViewModel, context: Context) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // 储存空间的布局与清理缓存触发
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item {
                SettingsGroupCard("存储管理") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearApplicationCache(context) }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("清理应用缓存", color = Color.White, fontSize = 15.sp)
                            Text("深度清理图片缓存、播放器缓冲和临时数据文件", color = TextGray, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = NeteaseRed
                        )
                    }
                }
            }
        }

        // 加载指示器
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        }
    }
}
