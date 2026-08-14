package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.model.ArtistInfo

// 你最爱的艺人区块
@Composable
fun FavoriteArtistsSection(artists: List<ArtistInfo>) {
    Column {
        SectionHeader(title = "你最爱的艺人", showAction = false)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.md)
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistCircleCard(artist)
            }
        }
    }
}

@Composable
fun ArtistCircleCard(artist: ArtistInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // 首页暂未接入艺人详情跳转，仅保留点击反馈
        modifier = Modifier.width(100.dp).clickable { }
    ) {
        val context = LocalContext.current
        val imageRequest = remember(artist.avatarUrl) {
            ImageRequest.Builder(context)
                .data("${artist.avatarUrl}?param=200y200")
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = artist.name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(MelodiaSpacing.sm))
        Text(
            text = artist.name,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
