package com.lin0721.linmusic.feature.artist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.model.ArtistAlbum

// 专辑 Tab: 采用精致的行模式展示
fun LazyListScope.artistAlbumTab(
    albums: List<ArtistAlbum>,
    onAlbumClick: (Long) -> Unit
) {
    if (albums.isEmpty()) {
        item(key = "empty_albums") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无专辑", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    } else {
        items(albums, key = { it.id }) { album ->
            ArtistAlbumRow(album = album, onClick = { onAlbumClick(album.id) })
        }
    }
}
