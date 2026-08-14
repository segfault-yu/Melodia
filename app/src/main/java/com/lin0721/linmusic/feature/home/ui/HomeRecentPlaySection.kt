package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.feature.home.data.RecentPlayItem

// 最近播放区块，自带轮播与网格两种展示形态的切换
@Composable
fun RecentPlaySection(
    items: List<RecentPlayItem>,
    onClick: (RecentPlayItem) -> Unit
) {
    var recentViewIsGrid by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = MelodiaSpacing.sm, top = 36.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(MelodiaSpacing.sm))
            Text(
                text = "最近播放",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { recentViewIsGrid = !recentViewIsGrid }) {
                Icon(
                    imageVector = if (recentViewIsGrid) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                    contentDescription = "切换视图",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (recentViewIsGrid) {
            RecentPlaylistGrid(
                items = items,
                onClick = onClick
            )
        } else {
            RecentPlaylistCarousel(
                items = items,
                onClick = onClick
            )
        }
    }
}

@Composable
fun RecentPlaylistCarousel(items: List<RecentPlayItem>, onClick: (RecentPlayItem) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)) {
        items(items, key = { it.data.id }) { item ->
            val playlist = item.data
            Column(modifier = Modifier.width(150.dp).clickable { onClick(item) }) {
                val context = LocalContext.current
                val imageRequest = remember(playlist.picUrl) {
                    ImageRequest.Builder(context)
                        .data("${playlist.picUrl}?param=300y300")
                        .crossfade(true)
                        .build()
                }
                AsyncImage(model = imageRequest, contentDescription = null, modifier = Modifier.size(150.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = playlist.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(MelodiaSpacing.xxs))
                Text(text = "歌单 · ${playlist.creator?.nickname ?: "网易云音乐"}", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// 网格形态最多展示 9 项，按三列补位对齐
@Composable
fun RecentPlaylistGrid(
    items: List<RecentPlayItem>,
    onClick: (RecentPlayItem) -> Unit
) {
    val gridItems = items.take(9)
    val rows = gridItems.chunked(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEach { item ->
                    val playlist = item.data
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(item) }
                    ) {
                        AsyncImage(
                            model = "${playlist.picUrl}?param=300y300",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = playlist.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
