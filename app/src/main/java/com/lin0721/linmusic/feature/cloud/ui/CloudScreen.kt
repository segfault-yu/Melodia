package com.lin0721.linmusic.feature.cloud.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold

// 我的云盘：网易云盘歌曲列表。仅只读浏览，上传流程不在范围内
@Composable
fun CloudScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(title = "我的云盘", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.CloudQueue,
                title = "云盘正在开发中",
                subtitle = "将展示你上传到网易云盘的歌曲"
            )
        }
    }
}
