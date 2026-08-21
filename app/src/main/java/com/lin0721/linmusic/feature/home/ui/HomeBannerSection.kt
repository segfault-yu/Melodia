package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.feature.home.domain.HomeBanner

// 站内歌单类轮播位可点击跳转，其余类型（网页活动）在 Melodia 无落地页，只展示不响应
private const val TARGET_TYPE_PLAYLIST = 1000

private val BannerHeight = 148.dp
private val BannerCorner = 8.dp

@Composable
fun HomeBannerSection(
    banners: List<HomeBanner>,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    val listState = rememberLazyListState()
    val activeIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Column(modifier = modifier.fillMaxWidth().padding(top = MelodiaSpacing.md)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm),
            // 不吸附的话可以停在两张中间，指示点与实际露出的图对不上
            flingBehavior = rememberSnapFlingBehavior(listState)
        ) {
            // 服务端未给稳定 banner 主键，同一活动可能占用多个位次，故 key 带上下标
            items(
                count = banners.size,
                key = { index -> "${banners[index].id}_$index" }
            ) { index ->
                val banner = banners[index]
                val playlistId = banner.targetId.takeIf {
                    banner.targetType == TARGET_TYPE_PLAYLIST && it > 0
                }

                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(BannerHeight)
                        .clip(RoundedCornerShape(BannerCorner))
                        .then(
                            if (playlistId != null) Modifier.clickable { onPlaylistClick(playlistId) }
                            else Modifier
                        )
                ) {
                    AsyncImage(
                        model = banner.picUrl.withCoverParam("800y360"),
                        contentDescription = banner.typeTitle,
                        modifier = Modifier.fillMaxWidth().height(BannerHeight),
                        contentScale = ContentScale.Crop
                    )

                    if (banner.typeTitle.isNotBlank()) {
                        Text(
                            text = banner.typeTitle,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(MelodiaSpacing.sm)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            banners.indices.forEach { index ->
                val selected = index == activeIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.5.dp)
                        .height(5.dp)
                        .width(if (selected) 14.dp else 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.22f)
                        )
                )
            }
        }
    }
}
