package com.lin0721.linmusic.feature.account.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold

// 账号与会员：等级、VIP、签到、关注与粉丝。数据层尚未接入
@Composable
fun AccountScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(title = "账号与会员", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.WorkspacePremium,
                title = "账号与会员正在开发中",
                subtitle = "将展示等级、会员状态、签到与关注粉丝数"
            )
        }
    }
}
