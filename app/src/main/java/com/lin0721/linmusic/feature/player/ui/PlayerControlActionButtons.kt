package com.lin0721.linmusic.feature.player.ui

import android.media.AudioDeviceInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// 控件区底部次级入口：输出设备、分享、播放队列
// connectedDevice 非空时说明当前连着非扬声器设备（有线/蓝牙），图标换类型、变主题色，并在图标后显示设备名；扬声器播放时保持默认
@Composable
fun ActionButtons(
    onOutputDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
    onShareClick: () -> Unit,
    connectedDevice: AudioDeviceInfo? = null
) {
    val outputDeviceIcon = connectedDevice?.let { deviceIcon(it.type) } ?: Icons.Rounded.SpeakerGroup
    val outputDeviceTint = if (connectedDevice != null) MaterialTheme.colorScheme.primary else TextGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.xl)
            .padding(top = MelodiaSpacing.sm, bottom = MelodiaSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOutputDeviceClick
                )
        ) {
            IconButton(
                onClick = onOutputDeviceClick,
                modifier = Modifier.offset(x = (-12).dp)
            ) {
                Icon(outputDeviceIcon, contentDescription = "连接设备", tint = outputDeviceTint, modifier = Modifier.size(24.dp))
            }
            if (connectedDevice != null) {
                Text(
                    text = deviceLabel(connectedDevice),
                    color = outputDeviceTint,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .offset(x = (-8).dp)
                        .widthIn(max = 140.dp)
                )
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
