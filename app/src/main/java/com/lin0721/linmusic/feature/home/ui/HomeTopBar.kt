package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.ui.components.FilterChipsRow
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import java.util.Calendar

// 按当前时段取问候语
private fun getGreetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }
}

// 顶部问候栏：未登录时整行可点击拉起登录
@Composable
fun TopGreetingBar(
    userProfile: UserProfile?,
    onLoginClick: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    val greeting = remember { getGreetingText() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (userProfile == null) Modifier.clickable { onLoginClick() } else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = MelodiaSpacing.lg, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userProfile != null) {
            AsyncImage(
                model = "${userProfile.avatarUrl}?param=200y200",
                contentDescription = "用户头像",
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onLoginClick() },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (userProfile != null) {
                Text(text = "$greeting，", fontSize = 12.sp, color = Color.LightGray)
                Text(text = userProfile.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(text = "未登录", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "点击登录", fontSize = 12.sp, color = Color.LightGray)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// 内容类型筛选胶囊，选中态仅作用于本地 UI
@Composable
fun FilterPills() {
    var selectedIndex by remember { mutableStateOf(0) }
    FilterChipsRow(
        items = listOf("全部", "音乐", "播客"),
        selectedIndex = selectedIndex,
        onSelected = { selectedIndex = it }
    )
}
