package com.lin0721.linmusic.ui.library

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.ui.components.LoginBottomSheet
import com.lin0721.linmusic.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.TextGray
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.gestures.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.lin0721.linmusic.ui.components.ProfileSidebar
import kotlinx.coroutines.launch
import com.lin0721.linmusic.data.local.UserProfile

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onPlaylistClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onBack: () -> Unit,
    onOpenSidebar: () -> Unit = {},
    onLoginScreenVisibilityChanged: (Boolean) -> Unit = {}
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    // 监听网页登录界面可见性
    LaunchedEffect(showWebViewLogin) {
        onLoginScreenVisibilityChanged(showWebViewLogin)
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val onAvatarClick: () -> Unit = {
        if (uiState.userProfile != null) {
            onOpenSidebar()
        } else {
            showLoginSheet = true
        }
    }
    val isGridView = uiState.isGridView

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 1. 顶部栏 (支持搜索展开)
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "top_bar_search"
            ) { active ->
                if (active) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text("搜索您的收藏内容...", color = TextGray, fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(NeteaseRed),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除", tint = TextGray)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 用户头像
                        val avatarUrl = uiState.userProfile?.avatarUrl
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = "$avatarUrl?param=100y100",
                                contentDescription = "用户头像",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onAvatarClick() },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceDark)
                                    .clickable { onAvatarClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "音乐库",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = Color.White)
                        }

                        IconButton(onClick = {
                            if (uiState.userProfile != null) {
                                playlistNameInput = ""
                                showCreateDialog = true
                            } else {
                                com.lin0721.linmusic.ui.components.ToastManager.showToast("请先登录以创建歌单！")
                                showLoginSheet = true
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "创建歌单", tint = Color.White)
                        }
                    }
                }
            }

            // 用户登录状态条件渲染
            if (uiState.userProfile == null) {
                NotLoggedInView(
                    onLoginClick = { showLoginSheet = true }
                )
            } else {
                // 2. 分类过滤器横向滚动列表
                FilterChipsRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.updateFilter(it) },
                    playlistCount = uiState.playlistCount,
                    albumCount = uiState.albumCount,
                    artistCount = uiState.artistCount
                )

                // 3. 排序与视图展示状态栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { showSortMenu = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (uiState.sortOrder) {
                                LibrarySortOrder.RECENTLY_PLAYED -> "最近播放"
                                LibrarySortOrder.CREATE_TIME -> "创建时间"
                                LibrarySortOrder.NAME -> "字母排序"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("最近播放", color = Color.White) },
                                onClick = {
                                    viewModel.updateSortOrder(LibrarySortOrder.RECENTLY_PLAYED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("创建时间", color = Color.White) },
                                onClick = {
                                    viewModel.updateSortOrder(LibrarySortOrder.CREATE_TIME)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("字母排序", color = Color.White) },
                                onClick = {
                                    viewModel.updateSortOrder(LibrarySortOrder.NAME)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.updateGridView(!isGridView) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                            contentDescription = "切换视图",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. 混合聚合列表
                if (uiState.isLoading && uiState.allItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeteaseRed)
                    }
                } else if (uiState.filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "未找到相关收藏项" else "列表为空，快去添加吧！",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (isGridView) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 180.dp, top = 4.dp, start = 16.dp, end = 16.dp)
                    ) {
                        val rows = uiState.filteredItems.chunked(3)
                        items(rows.size, key = { "grid_row_$it" }) { rowIndex ->
                            val row = rows[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { item ->
                                    LibraryGridItem(
                                        item = item,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (item.type == LibraryItemType.PLAYLIST) {
                                                onPlaylistClick(item.id.toLong())
                                            } else if (item.type == LibraryItemType.ARTIST) {
                                                onArtistClick(item.id.toLong())
                                            } else {
                                                com.lin0721.linmusic.ui.components.ToastManager.showToast("已收藏的专辑: ${item.title}")
                                            }
                                        }
                                    )
                                }
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 180.dp, top = 4.dp)
                    ) {
                        items(uiState.filteredItems, key = { "${it.type}_${it.id}" }) { item ->
                            LibraryItemRow(
                                item = item,
                                onClick = {
                                    if (item.type == LibraryItemType.PLAYLIST) {
                                        onPlaylistClick(item.id.toLong())
                                    } else if (item.type == LibraryItemType.ARTIST) {
                                        onArtistClick(item.id.toLong())
                                    } else {
                                        com.lin0721.linmusic.ui.components.ToastManager.showToast("已收藏的专辑: ${item.title}")
                                    }
                                },
                                onLongClick = {
                                    viewModel.togglePin(item.id)
                                    val action = if (item.isPinned) "取消置顶" else "置顶"
                                    com.lin0721.linmusic.ui.components.ToastManager.showToast("已$action: ${item.title}")
                                }
                            )
                        }
                    }
                }
            }

            // 创建歌单对话框
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("新建歌单", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("请输入新歌单的名称：", color = TextGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (playlistNameInput.isEmpty()) {
                                Text("歌单名称", color = TextGray, fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = playlistNameInput,
                                onValueChange = { playlistNameInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(NeteaseRed),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (playlistNameInput.isNotBlank()) {
                                viewModel.createPlaylist(playlistNameInput) {
                                    com.lin0721.linmusic.ui.components.ToastManager.showToast("歌单创建成功！")
                                }
                                showCreateDialog = false
                            } else {
                                com.lin0721.linmusic.ui.components.ToastManager.showToast("名字不能为空哦！")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeteaseRed)
                    ) {
                        Text("创建", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("取消", color = Color.White)
                    }
                },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 登录管理底部面板与 WebView
        if (showLoginSheet) {
            LoginBottomSheet(
                onDismiss = { showLoginSheet = false },
                onWebLogin = {
                    showLoginSheet = false
                    showWebViewLogin = true
                },
            )
        }

        if (showWebViewLogin) {
            WebViewLoginScreen(
                onClose = { showWebViewLogin = false },
                onLoginSuccess = { cookies ->
                    showWebViewLogin = false
                    viewModel.handleLoginSuccess(cookies)
                    com.lin0721.linmusic.ui.components.ToastManager.showToast("登录成功，正在同步乐库...")
                }
            )
        }
    }
}
}

// 未登录界面 placeholder
@Composable
private fun NotLoggedInView(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = NeteaseRed,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "开启您的专属乐库",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "登录后即可同步您的歌单、收藏的歌手与专辑。",
            color = TextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp)
        ) {
            Text(
                text = "立即登录",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 顶部过滤芯片组件
@Composable
private fun FilterChipsRow(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    playlistCount: Int,
    albumCount: Int,
    artistCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChipItem(
            label = "全部",
            isSelected = selectedFilter == LibraryFilter.ALL,
            onClick = { onFilterSelected(LibraryFilter.ALL) }
        )
        FilterChipItem(
            label = "歌单${if (playlistCount > 0) " $playlistCount" else ""}",
            isSelected = selectedFilter == LibraryFilter.PLAYLIST,
            onClick = { onFilterSelected(LibraryFilter.PLAYLIST) }
        )
        FilterChipItem(
            label = "专辑${if (albumCount > 0) " $albumCount" else ""}",
            isSelected = selectedFilter == LibraryFilter.ALBUM,
            onClick = { onFilterSelected(LibraryFilter.ALBUM) }
        )
        FilterChipItem(
            label = "歌手${if (artistCount > 0) " $artistCount" else ""}",
            isSelected = selectedFilter == LibraryFilter.ARTIST,
            onClick = { onFilterSelected(LibraryFilter.ARTIST) }
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) NeteaseRed else SurfaceDark,
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextGray,
        label = "chip_content"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 混合列表的单行条目
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryItemRow(
    item: LibraryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧封面图（针对歌手做圆图，歌单/专辑方图，已点赞歌曲做定制心形渐变图）
        if (item.isLikedSongs) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1), // 紫罗兰
                                Color(0xFFA855F7), // 紫色
                                Color(0xFFEC4899)  // 粉色
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            val shape = if (item.type == LibraryItemType.ARTIST) CircleShape else RoundedCornerShape(8.dp)
            
            AsyncImage(
                model = "${item.coverUrl}?param=150y150",
                contentDescription = item.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 右侧文字内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 如果是置顶条目，显示绿色的置顶图标
                if (item.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "已置顶",
                        tint = Color(0xFF10B981), // Emerald 绿，高档耐看
                        modifier = Modifier
                            .size(13.dp)
                            .padding(end = 4.dp)
                    )
                }

                Text(
                    text = when (item.type) {
                        LibraryItemType.PLAYLIST -> {
                            if (item.trackCount > 0) "${item.subtitle} • ${item.trackCount}首" else item.subtitle
                        }
                        else -> item.subtitle
                    },
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LibraryGridItem(
    item: LibraryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = if (item.type == LibraryItemType.ARTIST) CircleShape else RoundedCornerShape(8.dp)

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (item.isLikedSongs) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFFA855F7),
                                Color(0xFFEC4899)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            AsyncImage(
                model = "${item.coverUrl}?param=300y300",
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = item.subtitle,
            color = TextGray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
