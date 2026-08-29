package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// 与封面圆角（PlaylistHeaderItem 里的 10.dp）保持一致
private val SearchBarCornerRadius = 10.dp
private val SearchBarHeight = 40.dp

// ────────────────────────────────────────────────────────────────────────────
// 搜索栏：悬浮在列表上方的独立浮层，展开位移由调用方通过 modifier 的 graphicsLayer 驱动
// 搜索框与排序按钮做成毛玻璃质感的圆角矩形，圆角对齐封面；点击排序展开的是复用项目
// 通用弹层规范的 PlaylistSortSheet，而非系统 DropdownMenu
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun SearchBarItem(
    query: String,
    onQueryChange: (String) -> Unit,
    topPadding: Dp,
    backgroundColor: Color,
    sortOption: PlaylistSortOption,
    onSortOptionChange: (PlaylistSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(backgroundColor)) {
        // 占据 overlay 的高度，防止下拉后搜索栏被返回键等遮挡
        Spacer(modifier = Modifier.height(topPadding))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MelodiaSpacing.md, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(SearchBarHeight)
                    .clip(RoundedCornerShape(SearchBarCornerRadius))
                    .background(Color.White.copy(alpha = 0.12f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MelodiaSpacing.sm))
                androidx.compose.foundation.text.BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(Color.White),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("在歌单中搜索", color = Color.White, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(14.dp))
            }
            Spacer(Modifier.width(MelodiaSpacing.md))

            var showSortSheet by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .height(SearchBarHeight)
                    .clip(RoundedCornerShape(SearchBarCornerRadius))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { showSortSheet = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "排序",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showSortSheet) {
                PlaylistSortSheet(
                    sortOption = sortOption,
                    onSortOptionChange = {
                        onSortOptionChange(it)
                        showSortSheet = false
                    },
                    onDismiss = { showSortSheet = false }
                )
            }
        }
    }
}
