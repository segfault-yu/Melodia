package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.feature.home.data.DailySong
import com.lin0721.linmusic.feature.home.data.PersonalizedPlaylist
import com.lin0721.linmusic.feature.home.domain.ToplistInfo

// 为你推荐：五张固定入口卡，封面复用各数据源的首图
@Composable
fun ForYouSection(
    dailySongs: List<DailySong>,
    toplists: List<ToplistInfo>,
    recommendPlaylists: List<PersonalizedPlaylist>,
    onDailyRecommendClick: () -> Unit,
    onHotlistClick: (Long) -> Unit,
    onIntelligenceClick: () -> Unit,
    onRadarClick: (Long) -> Unit,
    onRoamingClick: () -> Unit
) {
    val hotlist = remember(toplists) {
        toplists.firstOrNull { it.name.contains("热") } ?: toplists.firstOrNull()
    }

    val radarPlaylist = remember(recommendPlaylists) {
        recommendPlaylists.firstOrNull { it.name.contains("雷达") } ?: recommendPlaylists.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "为你推荐", showAction = false)

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)
        ) {
            item {
                val coverUrl = dailySongs.firstOrNull()?.al?.picUrl
                ForYouCard(
                    title = "每日推荐",
                    subtitle = "专属歌曲每日更新",
                    coverUrl = coverUrl,
                    fallbackGradientColors = listOf(Color(0xFFE53935), Color(0xFFE35D5B)),
                    onClick = onDailyRecommendClick
                )
            }

            item {
                ForYouCard(
                    title = "热歌榜",
                    subtitle = "最热音乐随时听",
                    coverUrl = hotlist?.coverUrl,
                    fallbackGradientColors = listOf(Color(0xFFFFB300), Color(0xFFFBC02D)),
                    onClick = { hotlist?.let { onHotlistClick(it.id) } }
                )
            }

            item {
                val coverUrl = recommendPlaylists.getOrNull(2)?.picUrl
                ForYouCard(
                    title = "心动模式",
                    subtitle = "开启智能红心电台",
                    coverUrl = coverUrl,
                    fallbackGradientColors = listOf(Color(0xFF8E24AA), Color(0xFFAB47BC)),
                    onClick = onIntelligenceClick
                )
            }

            item {
                ForYouCard(
                    title = "私人雷达",
                    subtitle = "根据喜好精准定制",
                    coverUrl = radarPlaylist?.picUrl,
                    fallbackGradientColors = listOf(Color(0xFF1E88E5), Color(0xFF42A5F5)),
                    onClick = { radarPlaylist?.let { onRadarClick(it.id) } }
                )
            }

            item {
                val coverUrl = recommendPlaylists.getOrNull(1)?.picUrl
                ForYouCard(
                    title = "音乐漫游",
                    subtitle = "无限探索相似歌曲",
                    coverUrl = coverUrl,
                    fallbackGradientColors = listOf(Color(0xFF43A047), Color(0xFF66BB6A)),
                    onClick = onRoamingClick
                )
            }
        }
    }
}

@Composable
private fun ForYouCard(
    title: String,
    subtitle: String,
    coverUrl: String?,
    fallbackGradientColors: List<Color>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // 固化清晰封面的加载请求
    val coverRequest = remember(coverUrl) {
        if (!coverUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data("$coverUrl?param=300y300")
                .crossfade(true)
                .build()
        } else {
            null
        }
    }
    // 固化低清虚化背景的加载请求 (拉取 30x30 小图)
    val blurBgRequest = remember(coverUrl) {
        if (!coverUrl.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data("$coverUrl?param=30y30")
                .crossfade(true)
                .build()
        } else {
            null
        }
    }
    val fallbackIcon = when (title) {
        "每日推荐" -> Icons.Rounded.DateRange
        "热歌榜" -> Icons.Rounded.Star
        "心动模式" -> Icons.Rounded.Favorite
        "私人雷达" -> Icons.Rounded.Search
        "音乐漫游" -> Icons.Rounded.Shuffle
        else -> Icons.Rounded.MusicNote
    }

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        // 底层模糊背景
        if (blurBgRequest != null) {
            AsyncImage(
                model = blurBgRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(8.dp)
                    .graphicsLayer(alpha = 0.35f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = fallbackGradientColors))
            )
        }

        // 上方 150dp 封面区
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(150.dp)
        ) {
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = fallbackGradientColors))
                )
            }

            if (coverUrl.isNullOrBlank()) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 底部副标题区
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
