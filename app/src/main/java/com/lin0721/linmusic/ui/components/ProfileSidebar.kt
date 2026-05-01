package com.lin0721.linmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.data.local.UserProfile
import com.lin0721.linmusic.ui.theme.BackgroundDark
import com.lin0721.linmusic.ui.theme.NeteaseRed
import com.lin0721.linmusic.ui.theme.SurfaceDark
import com.lin0721.linmusic.ui.theme.SurfaceLight

/**
 * 用户个人中心侧边栏
 */
@Composable
fun ProfileSidebar(
    userProfile: UserProfile,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(BackgroundDark)
    ) {
        // --- 头部区域 ---
        ProfileHeader(
            userProfile = userProfile,
            onClick = { /* 查看个人主页 */ }
        )

        HorizontalDivider(
            color = SurfaceLight.copy(alpha = 0.3f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // --- 功能菜单列表 ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SidebarMenuItem(
                icon = Icons.Outlined.Email,
                label = "消息中心",
                onClick = { }
            )
            SidebarMenuItem(
                icon = Icons.Outlined.Timer,
                label = "定时关闭",
                onClick = { }
            )
            SidebarMenuItem(
                icon = Icons.Outlined.CloudDownload,
                label = "离线缓存",
                onClick = { }
            )
            SidebarMenuItem(
                icon = Icons.Outlined.Equalizer,
                label = "音质设置",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = SurfaceLight.copy(alpha = 0.2f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SidebarMenuItem(
                icon = Icons.Outlined.Settings,
                label = "设置",
                onClick = { }
            )
            SidebarMenuItem(
                icon = Icons.Outlined.Info,
                label = "关于",
                onClick = { }
            )
        }

        // --- 底部退出登录 ---
        HorizontalDivider(
            color = SurfaceLight.copy(alpha = 0.2f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        LogoutButton(onClick = onLogout)
    }
}

/**
 * 侧边栏头部（头像 + 昵称 + 引导语）
 */
@Composable
private fun ProfileHeader(
    userProfile: UserProfile,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 大头像
        AsyncImage(
            model = "${userProfile.avatarUrl}?param=300y300",
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 昵称
        Text(
            text = userProfile.nickname,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 引导语
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "查看个人主页",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 侧边栏菜单项
 */
@Composable
private fun SidebarMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 退出登录按钮
 */
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Logout,
            contentDescription = "退出登录",
            tint = NeteaseRed.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "退出登录",
            fontSize = 15.sp,
            color = NeteaseRed.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}
