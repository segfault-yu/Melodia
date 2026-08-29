package com.lin0721.linmusic.feature.player.ui

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// 只展示用户能理解的物理输出类型，蓝牙 LE/USB/有线各算一类
private val RELEVANT_DEVICE_TYPES = setOf(
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET
)

private fun listOutputDevices(audioManager: AudioManager): List<AudioDeviceInfo> =
    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { it.type in RELEVANT_DEVICE_TYPES }

private fun deviceIcon(type: Int): ImageVector = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Icons.Rounded.Smartphone
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> Icons.Rounded.BluetoothAudio
    else -> Icons.Rounded.Headset
}

// 未授予 BLUETOOTH_CONNECT 时系统只返回占位名，兜底成按类型区分的通用名称
private fun deviceLabel(device: AudioDeviceInfo): String {
    val name = device.productName?.toString().orEmpty()
    if (name.isNotBlank() && name != "?") return name
    return when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "此手机"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙设备"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "有线耳机"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB 耳机"
        else -> "未知设备"
    }
}

// 名称之下的设备类型子标题
private fun deviceTypeLabel(type: Int): String = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "手机扬声器"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> "蓝牙音频"
    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "有线耳机"
    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB 音频"
    else -> "音频设备"
}

// ────────────────────────────────────────────────────────────────────────────
// "连接设备"输出切换弹层：枚举系统音频输出设备，点选后经 SessionCommand 下发给 ExoPlayer
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputDeviceSheet(
    onDeviceSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var devices by remember { mutableStateOf(listOutputDevices(audioManager)) }
    // -1 表示用户还没手动点过；系统没有公开 API 能查真实路由，只能启发式猜：优先蓝牙，其次手机扬声器
    var selectedDeviceId by remember { mutableIntStateOf(-1) }
    val heuristicDefaultId = remember(devices) {
        devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET }?.id
            ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }?.id
            ?: -1
    }
    val effectiveSelectedId = if (selectedDeviceId != -1) selectedDeviceId else heuristicDefaultId

    // 弹层打开期间实时感知耳机插拔/蓝牙连接变化
    DisposableEffect(audioManager) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                devices = listOutputDevices(audioManager)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                devices = listOutputDevices(audioManager)
            }
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = BottomSheetShape,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = MelodiaSpacing.xs)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(DragHandleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = MelodiaSpacing.lg, end = MelodiaSpacing.lg, bottom = MelodiaSpacing.lg)
        ) {
            Text(
                text = "连接设备",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = MelodiaSpacing.md)
            )

            if (devices.isEmpty()) {
                Text(
                    text = "未检测到可用的输出设备",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm)) {
                devices.forEach { device ->
                    val isSelected = effectiveSelectedId != -1 && effectiveSelectedId == device.id
                    val rowBackground = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        Color.White.copy(alpha = 0.05f)
                    }
                    val badgeBackground = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    }
                    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(rowBackground)
                            .clickable {
                                selectedDeviceId = device.id
                                onDeviceSelected(device.id)
                            }
                            .padding(horizontal = MelodiaSpacing.md, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(badgeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = deviceIcon(device.type),
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(MelodiaSpacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deviceLabel(device),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = deviceTypeLabel(device.type),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
