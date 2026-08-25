package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

// 卡片数超过这个量就改横向滚动。服务端单个区块能给到 18 张，两列平铺会让一个货架吃掉两三屏，
// 滑半天还在同一个货架里；横向滚动既压住高度又不用截断内容。
private const val GRID_MAX_CARDS = 6

// 封面圆角刻意取小值贴合高密度观感，与项目其它页面的大圆角不同
private val CoverCorner = 4.dp
private val GridGap = 12.dp
private val HorizontalCardWidth = 150.dp

internal val HomeEdgePadding = 20.dp

// 一个货架：标题 + 卡片区。卡片少走两列网格，多则横向滚动。
// 两列用手写 Row 而非懒加载网格——垂直懒加载容器嵌进外层 LazyColumn 会因无界高度约束崩溃；
// 横向 LazyRow 方向不同，宽度有界，可以安全嵌套。
@Composable
fun HomeShelfSection(
    shelf: HomeShelf,
    onCardClick: (HomeCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = MelodiaSpacing.lg)) {
        Text(
            text = shelf.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = HomeEdgePadding, end = HomeEdgePadding, bottom = 13.dp)
        )

        if (shelf.cards.size <= GRID_MAX_CARDS) {
            Column(modifier = Modifier.padding(horizontal = HomeEdgePadding)) {
                shelf.cards.chunked(2).forEachIndexed { rowIndex, rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(GridGap)
                    ) {
                        rowCards.forEachIndexed { colIndex, card ->
                            HomeShelfCard(
                                card = card,
                                rank = (rowIndex * 2 + colIndex + 1).takeIf { shelf.showRank },
                                modifier = Modifier.weight(1f),
                                onClick = { onCardClick(card) }
                            )
                        }
                        // 奇数张时补等宽占位，避免最后一张被拉伸成整行
                        if (rowCards.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = HomeEdgePadding),
                horizontalArrangement = Arrangement.spacedBy(GridGap)
            ) {
                // 服务端会把同一资源投放到多个位次，key 必须带类型与下标才不会撞
                itemsIndexed(
                    items = shelf.cards,
                    key = { index, card -> "${card::class.simpleName}_${card.id}_$index" }
                ) { index, card ->
                    HomeShelfCard(
                        card = card,
                        rank = (index + 1).takeIf { shelf.showRank },
                        modifier = Modifier.width(HorizontalCardWidth),
                        onClick = { onCardClick(card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeShelfCard(
    card: HomeCard,
    rank: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(CoverCorner))
        ) {
            AsyncImage(
                model = card.coverUrl.withCoverParam("400y400"),
                contentDescription = card.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop
            )

            rank?.let {
                Text(
                    text = it.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // 歌曲与播客单集点一下直接播放，给个播放符号说清楚；歌单与专辑是进详情页，不加
            if (card is HomeCard.Song || card is HomeCard.Voice) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
