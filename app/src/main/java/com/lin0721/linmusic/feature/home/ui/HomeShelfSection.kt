package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.feature.home.domain.HomeCard
import com.lin0721.linmusic.feature.home.domain.HomeShelf

// 卡片封面圆角。刻意取小值贴合 Spotify 的高密度观感，与项目其它页面的大圆角不同
private val CoverCorner = 4.dp
private val GridGap = 12.dp

// 一个货架：标题 + 定宽两列卡片。
// 两列用手写 Row 而非 LazyVerticalGrid —— 懒加载容器嵌进外层 LazyColumn 会因无界高度约束直接崩溃。
@Composable
fun HomeShelfSection(
    shelf: HomeShelf,
    onCardClick: (HomeCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = MelodiaSpacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shelf.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            shelf.moreText?.let {
                Text(
                    text = it,
                    color = TextGray,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(start = MelodiaSpacing.sm)
                )
            }
        }

        shelf.cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(GridGap)
            ) {
                rowCards.forEach { card ->
                    HomeShelfCard(
                        card = card,
                        modifier = Modifier.weight(1f),
                        onClick = { onCardClick(card) }
                    )
                }
                // 奇数张时补一个等宽占位，避免最后一张被拉伸成整行
                if (rowCards.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeShelfCard(
    card: HomeCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(CoverCorner))
        ) {
            AsyncImage(
                model = card.coverUrl.withCoverParam("400y400"),
                contentDescription = card.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = card.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp)
        )

        if (card.caption.isNotBlank()) {
            Text(
                text = card.caption,
                color = TextGray,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// 翻页加载中的占位，服务端翻完两页后不再出现
@Composable
fun HomeShelfLoadingMore() {
    Box(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.height(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
