package com.lin0721.linmusic.feature.cloud.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MelodiaDragHandle
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MelodiaPress
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.feature.cloud.upload.UploadStatus
import com.lin0721.linmusic.feature.cloud.upload.UploadTask

private val StatusGreen = Color(0xFF639922)

// 上传进度弹窗：只读展示 CloudUploadManager 的队列状态，不持有状态。
// 关闭弹窗只是隐藏 UI，Service 和系统通知不受影响，继续跑
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudUploadProgressSheet(
    tasks: List<UploadTask>,
    onRetry: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val total = tasks.size
    val doneCount = tasks.count { it.status == UploadStatus.SUCCESS }
    // 每个 task 的 progress 在成功时已经是 1f，这里只需要对全队列取平均，不能再叠加 doneCount
    val overallProgress = if (total == 0) 0f else (tasks.sumOf { it.progress.toDouble() } / total).toFloat()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        shape = BottomSheetShape,
        dragHandle = { MelodiaDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = MelodiaSpacing.md)
        ) {
            Text(
                text = "正在上传",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "$doneCount / $total 完成",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(MelodiaSpacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { overallProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = NeteaseRed,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    text = "${(overallProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = MelodiaSpacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(MelodiaSpacing.md))

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(bottom = MelodiaSpacing.sm)
            ) {
                items(tasks, key = { it.id }) { task ->
                    UploadTaskRow(task = task, onRetry = { onRetry(task.id) })
                }
            }
        }
    }
}

@Composable
private fun UploadTaskRow(task: UploadTask, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (task.status) {
            UploadStatus.SUCCESS -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = StatusGreen,
                modifier = Modifier.size(18.dp)
            )

            UploadStatus.FAILED -> Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = NeteaseRed,
                modifier = Modifier.size(18.dp)
            )

            UploadStatus.QUEUED -> Icon(
                Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            else -> CircularProgressIndicator(
                color = NeteaseRed,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = MelodiaSpacing.sm)) {
            Text(
                text = task.fileName,
                color = if (task.status == UploadStatus.FAILED) NeteaseRed else Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.status == UploadStatus.FAILED && task.errorMessage != null) {
                Text(
                    text = task.errorMessage,
                    color = NeteaseRed.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        when (task.status) {
            UploadStatus.UPLOADING -> Text(
                text = "${(task.progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            UploadStatus.FAILED -> Row(
                modifier = Modifier
                    .pressable(MelodiaPress.Icon, shape = CircleShape, onClick = onRetry)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "重试",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            else -> Unit
        }
    }
}
