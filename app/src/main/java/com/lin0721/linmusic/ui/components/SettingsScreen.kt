package com.lin0721.linmusic.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.data.local.UserPreferences
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.data.remote.api.UserBindingItem
import com.lin0721.linmusic.data.remote.api.UserLevelData
import com.lin0721.linmusic.data.remote.api.VipInfoData
import com.lin0721.linmusic.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    
    // ─── 状态绑定 ───
    val wifiQuality by viewModel.wifiQuality.collectAsStateWithLifecycle()
    val mobileQuality by viewModel.mobileQuality.collectAsStateWithLifecycle()
    val defaultPlaylistPrivate by viewModel.defaultPlaylistPrivate.collectAsStateWithLifecycle()
    
    val userLevel by viewModel.userLevel.collectAsStateWithLifecycle()
    val vipInfo by viewModel.vipInfo.collectAsStateWithLifecycle()
    val userBindings by viewModel.userBindings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // ─── 弹窗控制状态 ───
    var showProfileDialog by remember { mutableStateOf(false) }
    var qualityDialogTarget by remember { mutableStateOf<String?>(null) } // "wifi" 或 "mobile"
    var showAboutDialog by remember { mutableStateOf(false) }

    // 监听 Toast 事件
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collectLatest { msg ->
            com.lin0721.linmusic.ui.components.ToastManager.showToast(msg)
        }
    }

    // 头像选择器 Launcher（调用系统的 GetContent，安全且免读写权限）
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = copyUriToFile(context, uri)
            if (file != null) {
                viewModel.uploadUserAvatar(file)
            } else {
                com.lin0721.linmusic.ui.components.ToastManager.showToast("图片加载失败")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置和隐私",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 180.dp, top = 8.dp)
            ) {
                // ─── 1. 账号与个人资料卡片组 ───
                item {
                    SettingsGroupCard("个人资料与成长") {
                        // 用户基本资料行
                        val userPreferences = remember { UserPreferences(context) }
                        val userProfileState = userPreferences.userProfile.collectAsState(initial = null)
                        val profile = userProfileState.value
                        
                        if (profile != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showProfileDialog = true }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 头像点击支持选择上传
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceLight)
                                        .clickable { avatarPickerLauncher.launch("image/*") }
                                ) {
                                    AsyncImage(
                                        model = "${profile.avatarUrl}?param=150y150",
                                        contentDescription = "头像",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // 覆盖相机图标提示
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.nickname,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "账号ID: ${profile.uid}",
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextGray
                                )
                            }
                        }

                        // VIP 信息渲染
                        vipInfo?.let { vip ->
                            VipStatusCard(vip)
                        }

                        // 用户等级升级进度条
                        userLevel?.let { level ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "我的等级: Lv.${level.level}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "已听 ${level.nowPlayCount} 首 / 升级需 ${level.nextPlayCount} 首",
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { level.progress.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NeteaseRed,
                                    trackColor = SurfaceLight
                                )
                            }
                        }

                        // 签到行动行
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("每日签到", color = Color.White, fontSize = 15.sp)
                                Text("赚取云贝积分，兑换各种特权", color = TextGray, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.executeDailySignin() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("立即签到", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ─── 2. 播放设置 ───
                item {
                    SettingsGroupCard("播放设置") {
                        // Wi-Fi 播放音质
                        SettingsRow(
                            title = "Wi-Fi 播放默认音质",
                            subtitle = getQualityDisplayName(wifiQuality),
                            onClick = { qualityDialogTarget = "wifi" }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 移动网络播放音质
                        SettingsRow(
                            title = "移动网络（流量）播放默认音质",
                            subtitle = getQualityDisplayName(mobileQuality),
                            onClick = { qualityDialogTarget = "mobile" }
                        )
                    }
                }

                // ─── 3. 隐私与安全 ───
                item {
                    SettingsGroupCard("隐私与绑定安全") {
                        // 新建歌单默认隐私
                        SettingsSwitchRow(
                            title = "新建歌单默认私密",
                            subtitle = "开启后，新建歌单将默认仅自己可见",
                            checked = defaultPlaylistPrivate,
                            onCheckedChange = { viewModel.updateDefaultPlaylistPrivate(it) }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 账号绑定状态展示
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text("账号绑定信息", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (userBindings.isEmpty()) {
                                Text("暂无账号绑定信息", color = TextGray, fontSize = 13.sp)
                            } else {
                                userBindings.forEach { binding ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = getBindingIcon(binding.type),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(binding.typeName, color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = if (binding.expired) "已过期" else "已绑定",
                                            color = if (binding.expired) NeteaseRed else Color(0xFF10B981),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── 4. 缓存与关于 ───
                item {
                    SettingsGroupCard("通用与存储") {
                        // 缓存管理
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearApplicationCache(context) }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("清理应用缓存", color = Color.White, fontSize = 15.sp)
                                Text("深度清理包括图片缓存、ExoPlayer 缓冲和临时数据", color = TextGray, fontSize = 12.sp)
                            }
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = NeteaseRed
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        // 关于
                        SettingsRow(
                            title = "关于 Melodia",
                            subtitle = "版本检查与开源协议",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }

                // ─── 5. 退出登录 ───
                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            .height(52.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        shape = RoundedCornerShape(26.dp),
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 加载环覆盖
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeteaseRed)
                }
            }
        }
    }

    // ─── 个人资料编辑弹窗 ───
    if (showProfileDialog) {
        val userPreferences = remember { UserPreferences(context) }
        val userProfileState = userPreferences.userProfile.collectAsState(initial = null)
        val profile = userProfileState.value
        
        if (profile != null) {
            var tempNickname by remember { mutableStateOf(profile.nickname) }
            var tempSignature by remember { mutableStateOf("") } // 签名默认为空，提供修改
            val isNicknameDuplicated by viewModel.isNicknameDuplicated.collectAsStateWithLifecycle()

            Dialog(onDismissRequest = { showProfileDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "编辑个人资料",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 昵称输入
                        Text("修改昵称", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(SurfaceLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = tempNickname,
                                onValueChange = {
                                    tempNickname = it
                                    viewModel.onNicknameInputChanged(it)
                                },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(NeteaseRed),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            // 查重指示器
                            if (isNicknameDuplicated != null) {
                                if (isNicknameDuplicated == true) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeteaseRed, modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (isNicknameDuplicated == true) {
                            Text("该昵称已被他人使用", color = NeteaseRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 签名输入
                        Text("个人介绍", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(SurfaceLight, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = tempSignature,
                                onValueChange = { tempSignature = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(NeteaseRed),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 保存与取消按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showProfileDialog = false }) {
                                Text("取消", color = TextGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveProfileChanges(tempNickname, tempSignature) { success ->
                                        if (success) showProfileDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                                enabled = isNicknameDuplicated != true && tempNickname.isNotBlank()
                            ) {
                                Text("保存", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── 音质单选选择弹窗 ───
    if (qualityDialogTarget != null) {
        val qualities = listOf(
            "standard" to "标准音质",
            "exhigh" to "极高音质",
            "lossless" to "无损音质 (FLAC)",
            "hires" to "Hi-Res 无损",
            "jymaster" to "超清母带"
        )
        val isWifi = qualityDialogTarget == "wifi"
        val activeQuality = if (isWifi) wifiQuality else mobileQuality
        
        Dialog(onDismissRequest = { qualityDialogTarget = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isWifi) "选择 Wi-Fi 播放音质" else "选择移动网络播放音质",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    qualities.forEach { pair ->
                        val key = pair.first
                        val label = pair.second
                        val isSelected = activeQuality == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isWifi) {
                                        viewModel.updateWifiQuality(key)
                                    } else {
                                        viewModel.updateMobileQuality(key)
                                    }
                                    qualityDialogTarget = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = if (isSelected) NeteaseRed else Color.White, fontSize = 15.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeteaseRed)
                            }
                        }
                    }
                }
            }
        }
    }


    // ─── 关于与开源协议弹窗 ───
    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeteaseRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Melodia Player", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.4 (api-enhanced)", color = TextGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Melodia 是一款完全基于 Jetpack Compose 构建的高水准现代网易云音乐播放器客户端。\n\n" +
                                "本项目基于开源协议发布，底层借助 api-enhanced 引擎提供了多样化的音质定制通道。",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("开源协议 (MIT LICENSE)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files...",
                        color = TextGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .background(SurfaceLight, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("知道了", color = Color.White)
                    }
                }
            }
        }
    }
}

// ─── 各种卡片及单行布局小组件 ───

@Composable
private fun SettingsGroupCard(
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
private fun SettingsRow(
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
private fun SettingsSwitchRow(
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

@Composable
private fun VipStatusCard(vip: VipInfoData) {
    Spacer(modifier = Modifier.height(10.dp))

    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val dateStr = if (vip.expireTime > 0) sdf.format(java.util.Date(vip.expireTime)) else ""

    if (vip.isVip) {
        when (vip.vipType) {
            11 -> {
                // 黑胶 VIP：Obsidian Black & Gold 极奢黑金质感卡片
                val obsidianBg = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                )
                val goldBorder = Brush.linearGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFD97706))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.5.dp, goldBorder), RoundedCornerShape(12.dp))
                        .background(obsidianBg, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFFCD34D),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "黑胶 VIP 会员",
                                color = Color(0xFFFDE047),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (dateStr.isNotEmpty()) "有效期至 $dateStr" else "尊享黑胶VIP专属曲库及超清母带音质",
                                color = Color(0xFFFDE047).copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFCD34D),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("已激活", color = Color(0xFF0F172A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            10 -> {
                // 音乐包会员：炫彩银蓝/靛蓝渐变卡片
                val cyanBg = Brush.linearGradient(
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF2563EB), Color(0xFF4F46E5))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cyanBg, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "音乐包会员",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (dateStr.isNotEmpty()) "有效期至 $dateStr" else "已开通音乐包，畅听收费曲库",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("已激活", color = Color(0xFF2563EB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            else -> {
                // 其他会员类型：金色/棕色渐变卡片
                val goldBg = Brush.linearGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFFBBF24), Color(0xFFEAB308))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(goldBg, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFF78350F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "尊贵 VIP 会员",
                                color = Color(0xFF78350F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (dateStr.isNotEmpty()) "有效期至 $dateStr" else "尊享无损音质与免费版权播放",
                                color = Color(0xFF78350F).copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF78350F),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("已激活", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // 未开通会员：暗色虚线质感卡片，提示用户开通
        val inactiveBg = Brush.linearGradient(
            colors = listOf(Color(0xFF27272A), Color(0xFF18181B))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                .background(inactiveBg, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "未开通会员",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "开通会员解锁海量无损及母带音质歌曲",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeteaseRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, NeteaseRed),
                modifier = Modifier.height(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("立即开通", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getBindingIcon(type: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        1 -> Icons.Default.PhoneAndroid
        10 -> Icons.Default.ChatBubbleOutline // 微信模拟
        20 -> Icons.Default.AccountCircle // QQ模拟
        else -> Icons.Default.Link
    }
}

// 辅助方法：URI 复制
private fun copyUriToFile(context: Context, uri: android.net.Uri): java.io.File? {
    return runCatching {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = java.io.File.createTempFile("avatar_temp", ".jpg", context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
        }
        tempFile
    }.getOrNull()
}

// 获取音质可读展示名
private fun getQualityDisplayName(quality: String): String {
    return when (quality) {
        "standard" -> "标准"
        "exhigh" -> "极高"
        "lossless" -> "无损 (FLAC)"
        "hires" -> "Hi-Res"
        "jyeffect" -> "高清环绕声"
        "sky" -> "沉浸环绕声"
        "jymaster" -> "超清母带"
        else -> quality
    }
}
