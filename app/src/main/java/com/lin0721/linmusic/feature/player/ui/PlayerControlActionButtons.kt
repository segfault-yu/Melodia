package com.lin0721.linmusic.feature.player.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// 控件区底部次级入口：定时关闭、分享、播放队列
@Composable
fun ActionButtons(
    onTimerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.xl)
            .padding(top = MelodiaSpacing.sm, bottom = MelodiaSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(
                onClick = onTimerClick,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(Icons.Rounded.AccessTime, contentDescription = "定时关闭", tint = TextGray, modifier = Modifier.size(24.dp))
            }
        }
        Row {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "分享",
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(
                onClick = onQueueClick,
                modifier = Modifier.offset(x = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = TextGray, modifier = Modifier.size(30.dp))
            }
        }
    }
}
