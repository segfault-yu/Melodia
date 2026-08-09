package com.lin0721.linmusic.feature.library.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
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
import com.lin0721.linmusic.core.ui.components.FilterChipsRow
import com.lin0721.linmusic.core.ui.components.LoginBottomSheet
import com.lin0721.linmusic.core.ui.components.MelodiaDragHandle
import com.lin0721.linmusic.core.ui.components.WebViewLoginScreen
import com.lin0721.linmusic.core.ui.theme.BottomSheetShape
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.gestures.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.lin0721.linmusic.core.ui.components.ProfileSidebar
import kotlinx.coroutines.launch
import com.lin0721.linmusic.core.auth.UserProfile

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLoginSheet by remember { mutableStateOf(false) }
    var showWebViewLogin by remember { mutableStateOf(false) }

    // 监听网页登录界面可见性
    LaunchedEffect(showWebViewLogin) {
        onLoginScreenVisibilityChanged(showWebViewLogin)
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collect { message ->
            com.lin0721.linmusic.core.ui.components.ToastManager.showToast(message)
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val onAvatarClick: () -> Unit = {
        if (userProfile != null) {
            onOpenSidebar()
        } else {
            showLoginSheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                            .padding(horizontal = MelodiaSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = MelodiaSpacing.md),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text("搜索您的收藏内容...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = MelodiaSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 用户头像
                        val avatarUrl = userProfile?.avatarUrl
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
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onAvatarClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "音乐库",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        IconButton(onClick = {
                            if (userProfile != null) {
                                playlistNameInput = ""
                                showCreateDialog = true
                            } else {
                                com.lin0721.linmusic.core.ui.components.ToastManager.showToast("请先登录以创建歌单！")
                                showLoginSheet = true
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "创建歌单", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // 用户登录状态条件渲染
            if (userProfile == null) {
                NotLoggedInView(
                    onLoginClick = { showLoginSheet = true }
                )
            } else {
                val successState = uiState as? LibraryUiState.Success
                // 2. 分类过滤器横向滚动列表
                FilterChipsRow(
                    items = listOf(
                        "全部",
                        "歌单${if ((successState?.playlistCount ?: 0) > 0) " ${successState?.playlistCount}" else ""}",
                        "专辑${if ((successState?.albumCount ?: 0) > 0) " ${successState?.albumCount}" else ""}",
                        "歌手${if ((successState?.artistCount ?: 0) > 0) " ${successState?.artistCount}" else ""}"
                    ),
                    selectedIndex = selectedFilter.ordinal,
                    onSelected = { index -> viewModel.updateFilter(LibraryFilter.entries[index]) }
                )

                // 3. 排序与视图展示状态栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.sm),
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
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (sortOrder) {
                                LibrarySortOrder.RECENTLY_PLAYED -> "最近播放"
                                LibrarySortOrder.CREATE_TIME -> "创建时间"
                                LibrarySortOrder.NAME -> "字母排序"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )


                    }

                    IconButton(
                        onClick = { viewModel.updateGridView(!isGridView) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.GridView,
                            contentDescription = "切换视图",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. 混合聚合列表
                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is LibraryUiState.Error -> {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(MelodiaSpacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(MelodiaSpacing.md))
                                Button(
                                    onClick = { viewModel.loadLibraryData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("重试", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                    is LibraryUiState.Success -> {
                        if (state.filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(MelodiaSpacing.xl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "未找到相关收藏项" else "列表为空，快去添加吧！",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (isGridView) {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 180.dp, top = MelodiaSpacing.xs, start = MelodiaSpacing.md, end = MelodiaSpacing.md)
                            ) {
                                val rows = state.filteredItems.chunked(3)
                                items(rows, key = { row -> row.joinToString(separator = "_") { it.id } }) { row ->
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
                                                        com.lin0721.linmusic.core.ui.components.ToastManager.showToast("已收藏的专辑: ${item.title}")
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
                                contentPadding = PaddingValues(bottom = 180.dp, top = MelodiaSpacing.xs)
                            ) {
                                items(state.filteredItems, key = { "${it.type}_${it.id}" }) { item ->
                                    LibraryItemRow(
                                        item = item,
                                        onClick = {
                                            if (item.type == LibraryItemType.PLAYLIST) {
                                                onPlaylistClick(item.id.toLong())
                                            } else if (item.type == LibraryItemType.ARTIST) {
                                                onArtistClick(item.id.toLong())
                                            } else {
                                                com.lin0721.linmusic.core.ui.components.ToastManager.showToast("已收藏的专辑: ${item.title}")
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.togglePin(item.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

        // 创建歌单对话框
        if (showCreateDialog) {
            ModalBottomSheet(
                onDismissRequest = { showCreateDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = BottomSheetShape,
                dragHandle = { MelodiaDragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = MelodiaSpacing.lg, vertical = MelodiaSpacing.md)
                ) {
                    Text(
                        text = "新建歌单",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = MelodiaSpacing.md)
                    )
                    Text(
                        text = "请输入新歌单的名称：",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = MelodiaSpacing.md),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (playlistNameInput.isEmpty()) {
                            Text("歌单名称", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = playlistNameInput,
                            onValueChange = { playlistNameInput = it },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(MelodiaSpacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.width(MelodiaSpacing.md))
                        Button(
                            onClick = {
                                if (playlistNameInput.isNotBlank()) {
                                    viewModel.createPlaylist(playlistNameInput)
                                    showCreateDialog = false
                                } else {
                                    com.lin0721.linmusic.core.ui.components.ToastManager.showToast("名字不能为空哦！")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("创建", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
                }
            )
        }

        if (showSortMenu) {
            ModalBottomSheet(
                onDismissRequest = { showSortMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = BottomSheetShape,
                dragHandle = { MelodiaDragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = MelodiaSpacing.lg, vertical = MelodiaSpacing.md)
                ) {
                    Text(
                        text = "选择排序方式",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = MelodiaSpacing.md)
                    )

                    val sortOptions = listOf(
                        LibrarySortOrder.RECENTLY_PLAYED to "最近播放",
                        LibrarySortOrder.CREATE_TIME to "创建时间",
                        LibrarySortOrder.NAME to "字母排序"
                    )

                    sortOptions.forEach { (order, label) ->
                        val isSelected = sortOrder == order
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSortOrder(order)
                                    showSortMenu = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
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
            .padding(MelodiaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(MelodiaSpacing.lg))

        Text(
            text = "开启您的专属乐库",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "登录后即可同步您的歌单、收藏的歌手与专辑。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = MelodiaSpacing.md)
        )

        Spacer(modifier = Modifier.height(MelodiaSpacing.xl))

        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp)
        ) {
            Text(
                text = "立即登录",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
            .padding(horizontal = MelodiaSpacing.md, vertical = MelodiaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧封面图（针对歌手做圆图，歌单/专辑方图，已点赞歌曲做定制心形渐变图）
        if (item.isLikedSongs) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (item.id == "-2") {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B82F6), // 蓝色
                                Color(0xFF10B981)  // 绿色
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            val shape = if (item.type == LibraryItemType.ARTIST) CircleShape else RoundedCornerShape(10.dp)

            AsyncImage(
                model = "${item.coverUrl}?param=150y150",
                contentDescription = item.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(MelodiaSpacing.md))

        // 右侧文字内容
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(MelodiaSpacing.xs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 如果是置顶条目，显示绿色的置顶图标
                if (item.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "已置顶",
                        tint = Color(0xFF10B981), // Emerald 绿，高档耐看
                        modifier = Modifier
                            .size(13.dp)
                            .padding(end = MelodiaSpacing.xs)
                    )
                }

                Text(
                    text = when (item.type) {
                        LibraryItemType.PLAYLIST -> {
                            if (item.trackCount > 0) "${item.subtitle} • ${item.trackCount}首" else item.subtitle
                        }
                        else -> item.subtitle
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val shape = if (item.type == LibraryItemType.ARTIST) CircleShape else RoundedCornerShape(10.dp)

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (item.isLikedSongs) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
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
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (item.id == "-2") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF10B981)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = item.subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
