package com.lin0721.linmusic.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.model.Album
import com.lin0721.linmusic.core.model.Artist
import com.lin0721.linmusic.core.model.PlaylistDetail
import com.lin0721.linmusic.core.model.Track
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.SurfaceDark
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.feature.search.domain.SearchResultItem
import com.lin0721.linmusic.feature.search.domain.SearchSuggestion
import com.lin0721.linmusic.feature.search.domain.SearchType
import org.koin.androidx.compose.koinViewModel
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing

private val tagFallbackColors = listOf(
    Color(0xFFE13300),
    Color(0xFF1E3264),
    Color(0xFF148A08),
    Color(0xFF8D67AB),
    Color(0xFFE8115B),
    Color(0xFF537AA1),
    Color(0xFFE91429),
    Color(0xFF477D95),
    Color(0xFF7D4B32),
    Color(0xFF1A6B52),
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    autoFocus: Boolean = false,
    onOpenSidebar: () -> Unit = {},
    onPlaylistClick: (id: Long, isAlbum: Boolean) -> Unit = { _, _ -> },
    onArtistClick: (id: Long) -> Unit = {}
) {
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()
    val inputState by viewModel.inputState.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedResultsState by viewModel.resultsByType.getValue(selectedType).collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playerManager.currentTrack.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { com.lin0721.linmusic.core.ui.components.ToastManager.showToast(it) }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) viewModel.activateSearch()
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        focusManager.clearFocus()
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (userProfile != null) {
                AsyncImage(
                    model = "${userProfile!!.avatarUrl}?param=200y200",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onOpenSidebar() }
                )
            } else {
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            com.lin0721.linmusic.core.ui.components.ToastManager.showToast("请先在主页登录以显示侧边栏哦！")
                        }
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "搜索",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* 听歌识曲，暂不实现 */ }) {
                Icon(Icons.Rounded.MusicNote, contentDescription = "听歌识曲", tint = Color.White)
            }
        }

        val defaultKeywordText = (discoveryState as? DiscoveryUiState.Success)?.defaultKeyword ?: "搜索你想听的"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MelodiaSpacing.md)
                .padding(bottom = MelodiaSpacing.sm)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .then(
                    if (!isSearchActive) Modifier.clickable { viewModel.activateSearch() }
                    else Modifier
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (isSearchActive) {
                BasicTextField(
                    value = inputState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(NeteaseRed),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (inputState.query.isNotBlank()) viewModel.searchWithKeyword(inputState.query)
                        focusManager.clearFocus()
                    }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (inputState.query.isEmpty()) {
                                Text(defaultKeywordText, color = TextGray, fontSize = 14.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                if (inputState.query.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.updateQuery("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Text(
                    text = defaultKeywordText,
                    color = TextGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isSearchActive && inputState.query.isNotBlank()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SearchTypeTabRow(selected = selectedType, onSelect = { viewModel.selectType(it) })
                    SearchResultsList(
                        state = selectedResultsState,
                        type = selectedType,
                        currentTrackId = currentTrack?.mediaId,
                        onSongClick = { viewModel.playSong(it) },
                        onAlbumClick = { id -> onPlaylistClick(id, true) },
                        onArtistClick = onArtistClick,
                        onPlaylistClick = { id -> onPlaylistClick(id, false) },
                        onLoadMore = { viewModel.loadMore() }
                    )
                }
            } else {
                DiscoveryContent(
                    state = discoveryState,
                    history = history,
                    onHistoryClick = { viewModel.searchWithKeyword(it) },
                    onClearHistory = { viewModel.clearHistory() },
                    onHotSearchClick = { viewModel.searchWithKeyword(it) }
                )
            }

            if (isSearchActive && inputState.isSuggesting && inputState.suggestions.isNotEmpty()) {
                SuggestionDropdown(
                    suggestions = inputState.suggestions,
                    onClick = { viewModel.searchWithKeyword(it) }
                )
            }
        }
    }
}

@Composable
private fun SearchTypeTabRow(selected: SearchType, onSelect: (SearchType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MelodiaSpacing.md)
            .padding(bottom = MelodiaSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm)
    ) {
        SearchType.entries.forEach { type ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) NeteaseRed else SurfaceDark)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = type.label,
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SuggestionDropdown(
    suggestions: List<SearchSuggestion>,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark)
    ) {
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(suggestion.keyword) }
                    .padding(horizontal = MelodiaSpacing.md, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = suggestion.keyword,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val SearchResultItem.stableKey: String
    get() = when (this) {
        is SearchResultItem.SongItem -> "song_${track.id}"
        is SearchResultItem.AlbumItem -> "album_${album.id}"
        is SearchResultItem.ArtistItem -> "artist_${artist.id}"
        is SearchResultItem.PlaylistItem -> "playlist_${playlist.id}"
    }

// 分类型搜索结果列表
@Composable
private fun SearchResultsList(
    state: SearchResultsUiState,
    type: SearchType,
    currentTrackId: String?,
    onSongClick: (Track) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    when (state) {
        SearchResultsUiState.Idle, SearchResultsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        }
        SearchResultsUiState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有找到相关${type.label}", color = TextGray, fontSize = 14.sp)
            }
        }
        is SearchResultsUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = TextGray, fontSize = 14.sp)
            }
        }
        is SearchResultsUiState.Success -> {
            val listState = rememberLazyListState()
            val shouldLoadMore by remember(state.hasMore, state.isLoadingMore) {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= state.items.size - 5 && state.hasMore && !state.isLoadingMore
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) onLoadMore()
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 180.dp)
            ) {
                item(key = "header") {
                    Text(
                        "找到 ${state.totalCount} 个${type.label}",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.sm)
                    )
                }

                items(state.items, key = { it.stableKey }) { item ->
                    when (item) {
                        is SearchResultItem.SongItem -> {
                            val track = item.track
                            val isActive = currentTrackId == track.id.toString()
                            SongRow(
                                data = SongRowData(
                                    id = track.id,
                                    title = track.name,
                                    artist = track.ar.joinToString(" / ") { it.name },
                                    coverUrl = track.al.picUrl,
                                    durationText = if (track.dt > 0) {
                                        val minutes = track.dt / 1000 / 60
                                        val seconds = track.dt / 1000 % 60
                                        "${minutes}:%02d".format(seconds)
                                    } else {
                                        null
                                    }
                                ),
                                isActive = isActive,
                                onClick = { onSongClick(track) }
                            )
                        }
                        is SearchResultItem.AlbumItem -> AlbumResultRow(
                            album = item.album,
                            onClick = { onAlbumClick(item.album.id) }
                        )
                        is SearchResultItem.ArtistItem -> ArtistResultRow(
                            artist = item.artist,
                            onClick = { onArtistClick(item.artist.id) }
                        )
                        is SearchResultItem.PlaylistItem -> PlaylistResultRow(
                            playlist = item.playlist,
                            onClick = { onPlaylistClick(item.playlist.id) }
                        )
                    }
                }

                if (state.isLoadingMore) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(MelodiaSpacing.md),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeteaseRed, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumResultRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MelodiaSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "${album.picUrl}?param=100y100",
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = album.artists.joinToString(" / ") { it.name },
                color = TextGray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistResultRow(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MelodiaSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "${artist.picUrl}?param=100y100",
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaylistResultRow(playlist: PlaylistDetail, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MelodiaSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "${playlist.coverImgUrl}?param=100y100",
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = listOfNotNull(
                    playlist.creator?.nickname,
                    if (playlist.trackCount > 0) "${playlist.trackCount}首" else null
                ).joinToString(" · "),
                color = TextGray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiscoveryContent(
    state: DiscoveryUiState,
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onHotSearchClick: (String) -> Unit
) {
    when (state) {
        DiscoveryUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        }
        is DiscoveryUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = TextGray, fontSize = 14.sp)
            }
        }
        is DiscoveryUiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 180.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (history.isNotEmpty()) {
                    item(key = "history_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "搜索历史",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "清除搜索历史",
                                tint = TextGray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onClearHistory() }
                            )
                        }
                    }

                    val historyRows = history.chunked(2)
                    items(historyRows.size, key = { "history_row_$it" }) { rowIndex ->
                        val pair = historyRows[rowIndex]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MelodiaSpacing.md)
                                .padding(bottom = MelodiaSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm)
                        ) {
                            pair.forEach { keyword ->
                                Text(
                                    text = keyword,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceDark)
                                        .clickable { onHistoryClick(keyword) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (state.hotSearches.isNotEmpty()) {
                    item(key = "hot_header") {
                        SectionHeader("热搜榜")
                    }

                    val hotRows = state.hotSearches.take(10).chunked(2)
                    items(hotRows.size, key = { "hot_row_$it" }) { rowIndex ->
                        val pair = hotRows[rowIndex]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MelodiaSpacing.md)
                                .padding(bottom = MelodiaSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(MelodiaSpacing.sm)
                        ) {
                            pair.forEachIndexed { colIndex, item ->
                                val rank = rowIndex * 2 + colIndex + 1
                                HotSearchCompactItem(
                                    rank = rank,
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onHotSearchClick(item.keyword) }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 精品歌单标签
                if (state.playlistTags.isNotEmpty()) {
                    item(key = "tags_header") {
                        SectionHeader("精品歌单")
                    }

                    val rows = state.playlistTags.chunked(2)
                    items(rows.size, key = { "tag_row_$it" }) { rowIndex ->
                        val pair = rows[rowIndex]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MelodiaSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEachIndexed { colIndex, tag ->
                                val globalIndex = rowIndex * 2 + colIndex
                                PlaylistTagCard(
                                    tag = tag,
                                    fallbackColor = tagFallbackColors[globalIndex % tagFallbackColors.size],
                                    modifier = Modifier.weight(1f),
                                    onClick = { }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.md)
    )
}

@Composable
private fun PlaylistTagCard(
    tag: PlaylistTag,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (tag.coverUrl.isBlank()) fallbackColor else SurfaceDark)
            .clickable(onClick = onClick)
    ) {
        if (tag.coverUrl.isNotBlank()) {
            AsyncImage(
                model = tag.coverUrl,
                contentDescription = tag.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        Text(
            text = tag.name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HotSearchCompactItem(
    rank: Int,
    item: HotSearch,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTop3 = rank <= 3

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            color = if (isTop3) NeteaseRed else TextGray,
            fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = item.keyword,
            color = Color.White,
            fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!item.iconUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier.height(12.dp)
            )
        }
    }
}
