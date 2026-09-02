package com.lin0721.linmusic.feature.message.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold

// 消息：通知、@我、评论回复、私信。私信为只读，不做发送
@Composable
fun MessageScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(title = "消息", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Notifications,
                title = "消息正在开发中",
                subtitle = "将展示通知、@我、评论回复与私信"
            )
        }
    }
}
