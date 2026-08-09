package com.lin0721.linmusic.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.api.SearchSong
import com.lin0721.linmusic.feature.search.domain.HotSearch
import com.lin0721.linmusic.feature.search.domain.PlaylistTag
import com.lin0721.linmusic.core.ui.components.SongRow
import com.lin0721.linmusic.core.ui.components.SongRowData
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.SurfaceDark
import com.lin0721.linmusic.core.ui.theme.TextGray
import org.koin.androidx.compose.koinViewModel

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
    onBack: () -> Unit,
    onOpenSidebar: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchLoading by viewModel.searchLoading.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
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

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
            IconButton(onClick = { /* 听歌识曲 */ }) {
                Icon(Icons.Rounded.MusicNote, contentDescription = "听歌识曲", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .then(
                    if (!isSearching) Modifier.clickable { viewModel.activateSearch() }
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

            if (isSearching) {
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.updateQuery(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(NeteaseRed),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(uiState.defaultKeyword, color = TextGray, fontSize = 14.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.updateQuery("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = TextGray, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Text(
                    text = uiState.defaultKeyword,
                    color = TextGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 内容区域
        if (isSearching && (query.isNotEmpty() || searchResults.isNotEmpty())) {
            SearchResultsList(
                results = searchResults,
                isLoading = searchLoading,
                hasMore = hasMore,
                currentTrackId = currentTrack?.mediaId,
                onSongClick = { viewModel.playSong(it) },
                onLoadMore = { viewModel.loadMore() }
            )
        } else {
            DiscoveryContent(
                uiState = uiState,
                onHotSearchClick = { viewModel.searchWithKeyword(it) }
            )
        }
    }
}

// 搜索结果列表
@Composable
private fun SearchResultsList(
    results: List<SearchSong>,
    isLoading: Boolean,
    hasMore: Boolean,
    currentTrackId: String?,
    onSongClick: (SearchSong) -> Unit,
    onLoadMore: () -> Unit
) {
    if (isLoading && results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeteaseRed)
        }
        return
    }

    if (results.isEmpty() && !isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有找到相关歌曲", color = TextGray, fontSize = 14.sp)
        }
        return
    }

    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= results.size - 5 && hasMore && !isLoading
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
                "找到 ${results.size} 首歌曲",
                color = TextGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(results, key = { it.id }) { song ->
            val isActive = currentTrackId == song.id.toString()
            SongRow(
                data = SongRowData(
                    id = song.id,
                    title = song.name,
                    artist = song.ar.joinToString(" / ") { it.name },
                    coverUrl = song.al.picUrl,
                    durationText = if (song.dt > 0) {
                        val minutes = song.dt / 1000 / 60
                        val seconds = song.dt / 1000 % 60
                        "${minutes}:%02d".format(seconds)
                    } else {
                        null
                    }
                ),
                isActive = isActive,
                onClick = { onSongClick(song) }
            )
        }

        if (isLoading && results.isNotEmpty()) {
            item(key = "loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeteaseRed, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscoveryContent(
    uiState: SearchUiState,
    onHotSearchClick: (String) -> Unit
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeteaseRed)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 180.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.hotSearches.isNotEmpty()) {
            item(key = "hot_header") {
                SectionHeader("热搜榜")
            }

            val hotRows = uiState.hotSearches.take(10).chunked(2)
            items(hotRows.size, key = { "hot_row_$it" }) { rowIndex ->
                val pair = hotRows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        if (uiState.playlistTags.isNotEmpty()) {
            item(key = "tags_header") {
                SectionHeader("精品歌单")
            }

            val rows = uiState.playlistTags.chunked(2)
            items(rows.size, key = { "tag_row_$it" }) { rowIndex ->
                val pair = rows[rowIndex]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
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
