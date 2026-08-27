package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.ArtistMv
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// MV Tab: 卡片式列表，滚动到底自动追加下一页
fun LazyListScope.artistMvTab(
    mvs: List<ArtistMv>,
    loadingMore: Boolean,
    onMvClick: (ArtistMv) -> Unit
) {
    if (mvs.isEmpty() && !loadingMore) {
        item(key = "empty_mv") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "暂无MV",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(MelodiaSpacing.md))
                Text(
                    text = "暂无相关的 MV 视频",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(MelodiaSpacing.xs))
                Text(
                    text = "艺人尚未上传或暂无播放授权",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    } else {
        items(mvs, key = { it.id }) { mv ->
            ArtistMvCard(mv = mv, onClick = { onMvClick(mv) })
        }
        if (loadingMore) {
            item(key = "mv_loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MelodiaSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
