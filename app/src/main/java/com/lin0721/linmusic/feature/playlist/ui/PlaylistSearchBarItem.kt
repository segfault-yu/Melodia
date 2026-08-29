package com.lin0721.linmusic.feature.playlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

// ────────────────────────────────────────────────────────────────────────────
// 搜索栏：悬浮在列表上方的独立浮层，展开位移由调用方通过 modifier 的 graphicsLayer 驱动
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
                    .height(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MelodiaSpacing.sm))
                androidx.compose.foundation.text.BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    singleLine    = true,
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("在此页面上查找", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
            }
            Spacer(Modifier.width(12.dp))

            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = sortOption.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { showSortMenu = true }
                )
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    PlaylistSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            trailingIcon = {
                                if (option == sortOption) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                onSortOptionChange(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
