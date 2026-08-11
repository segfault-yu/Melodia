package com.lin0721.linmusic.feature.settings.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

// 定义多级菜单类型
enum class SettingsSubMenu(val title: String) {
    PLAYBACK_DOWNLOAD("播放与下载"),
    AUDIO_QUALITY("音质"),
    PRIVACY("隐私设置"),
    STORAGE("储存空间"),
    NETWORK("网络设置"),
    EXTENSIONS("扩展"),
    LYRICS("歌词设置"),
    ABOUT("关于")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var activeSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }

    BackHandler(enabled = activeSubMenu != null) {
        activeSubMenu = null
    }
    
    // 监听 Toast 提示消息
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collectLatest { msg ->
            ToastManager.showToast(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeSubMenu?.title ?: "设置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeSubMenu != null) {
                                activeSubMenu = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 使用 AnimatedContent 实现主菜单和子菜单的平滑切换动画
            AnimatedContent(
                targetState = activeSubMenu,
                transitionSpec = {
                    if (targetState != null) {
                        // 进入子菜单：从右往左滑入
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        // 返回主菜单：从左向右滑入
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "SettingsMenuTransition"
            ) { subMenu ->
                if (subMenu == null) {
                    // 主设置菜单
                    MainSettingsMenu(
                        onNavigate = { activeSubMenu = it },
                        onBack = onBack,
                        viewModel = viewModel
                    )
                } else {
                    // 各分类子菜单内容
                    SubMenuContent(
                        subMenu = subMenu,
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }
}

// 主设置列表菜单
@Composable
private fun MainSettingsMenu(
    onNavigate: (SettingsSubMenu) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        // 多级设置菜单入口组
        item {
            SettingsGroupCard("常规设置") {
                SettingsSubMenu.values().forEachIndexed { index, item ->
                    SettingsRow(
                        title = item.title,
                        subtitle = "",
                        onClick = { onNavigate(item) }
                    )
                    if (index < SettingsSubMenu.values().lastIndex) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    }
                }
            }
        }

        // 底部账户操作按钮组
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 退出登录按钮
                val logoutInteraction = remember { MutableInteractionSource() }
                val isPressed by logoutInteraction.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "")

                Button(
                    onClick = {
                        viewModel.executeLogout {
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, NeteaseRed.copy(alpha = 0.6f)),
                    interactionSource = logoutInteraction
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = NeteaseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "退出登录",
                            color = NeteaseRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 统一子菜单内容分发器
@Composable
private fun SubMenuContent(
    subMenu: SettingsSubMenu,
    viewModel: SettingsViewModel,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (subMenu) {
            SettingsSubMenu.PLAYBACK_DOWNLOAD -> PlaybackDownloadSettingsView(viewModel)
            SettingsSubMenu.AUDIO_QUALITY -> AudioQualitySettingsView(viewModel)
            SettingsSubMenu.PRIVACY -> PrivacySettingsView(viewModel)
            SettingsSubMenu.STORAGE -> StorageSettingsView(viewModel, context)
            SettingsSubMenu.NETWORK -> NetworkSettingsView(viewModel)
            SettingsSubMenu.EXTENSIONS -> ExtensionsSettingsView(viewModel)
            SettingsSubMenu.LYRICS -> LyricsSettingsView(viewModel)
            SettingsSubMenu.ABOUT -> AboutSettingsView()
        }
    }
}

// ─── 辅助卡片及布局小组件 (不带 private，以便同包子模块访问) ───

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 12.sp)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextGray
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextGray, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeteaseRed,
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = SurfaceLight
            )
        )
    }
}

fun getBindingIcon(type: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        1 -> Icons.Default.PhoneAndroid
        10 -> Icons.Default.ChatBubbleOutline
        20 -> Icons.Default.AccountCircle
        else -> Icons.Default.Link
    }
}
