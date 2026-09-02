package com.lin0721.linmusic.feature.listendata.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold

// 听歌数据：累计时长、今日/周/月排行、收听报告。数据层尚未接入
@Composable
fun ListenDataScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(title = "听歌数据", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Insights,
                title = "听歌数据正在开发中",
                subtitle = "将展示累计收听时长、听歌排行与周期报告"
            )
        }
    }
}
