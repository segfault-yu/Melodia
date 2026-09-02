package com.lin0721.linmusic.feature.newworks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold

// 关注歌手新作：关注歌手的新歌与新 MV。数据层尚未接入
@Composable
fun NewWorksScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(title = "关注歌手新作", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.NewReleases,
                title = "关注歌手新作正在开发中",
                subtitle = "将展示你关注的歌手最近发布的新歌与 MV"
            )
        }
    }
}
