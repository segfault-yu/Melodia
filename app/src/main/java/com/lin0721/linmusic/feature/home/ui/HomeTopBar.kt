package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.GradientStart
import com.lin0721.linmusic.core.ui.theme.MelodiaSpacing
import com.lin0721.linmusic.core.ui.theme.PillRadius
import java.util.Calendar
import kotlinx.coroutines.delay

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

// 首页/音乐/播客三个 tab 共用的固定顶栏，脱离各自 LazyColumn 独立渲染一次
@Composable
fun HomeSharedHeader(
    userProfile: UserProfile?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GradientStart, BackgroundDark)))
            .statusBarsPadding()
    ) {
        TopGreetingBar(
            userProfile = userProfile,
            onLoginClick = onAvatarClick,
            onSearchClick = onSearchClick
        )
        FilterPills(selectedIndex = selectedTab, onSelected = onTabSelected)
    }
}

// 二级药丸展开/折叠动画时长，需与下方 delay 保持一致
private const val SecondaryPillAnimDurationMs = 280

// 触发二级占位药丸展开的主分类下标，功能待定
private const val SecondaryPillTriggerIndex = 1

private val PillShapeFull = RoundedCornerShape(PillRadius)

// 音乐保留完整圆角、压在上层，贴合处天然向内收出弧线；最新贴合边不带圆角、
// 用 overlapStart 往左垫出一个圆角半径，这是真实参与 LazyRow 布局测量的宽度，
// 后面的胶囊每帧跟着这个宽度变化被动重新布局，不需要额外的位移动画。
private val PillShapeJoinEnd = RoundedCornerShape(topEnd = PillRadius, bottomEnd = PillRadius)

// 深色系数，二级药丸激活态用来跟主药丸的强调色区分开
private const val SecondaryActiveDarkenFactor = 0.7f

private fun Color.darken(factor: Float): Color =
    copy(red = red * factor, green = green * factor, blue = blue * factor)

// Modifier.padding 不接受负值，往左伸出用自定义 layout 实现：
// 内容整体左移 amount，同时对外少上报 amount 宽度，父布局据此感知
private fun Modifier.overlapStart(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overlapPx = amount.roundToPx()
    // 展开动画刚开始的头几帧测量宽度还小于圆角半径，减完会变负数，Compose 不允许负尺寸
    val reportedWidth = (placeable.width - overlapPx).coerceAtLeast(0)
    layout(reportedWidth, placeable.height) {
        placeable.place(-overlapPx, 0)
    }
}

// 内容类型筛选胶囊，选中态仅作用于本地 UI；选中"音乐"时右侧联动展开占位二级药丸
@Composable
fun FilterPills(selectedIndex: Int, onSelected: (Int) -> Unit) {
    val labels = remember { listOf("全部", "音乐", "播客") }
    val secondaryTarget = selectedIndex == SecondaryPillTriggerIndex
    var secondaryMounted by remember { mutableStateOf(secondaryTarget) }
    var secondaryVisible by remember { mutableStateOf(secondaryTarget) }
    var secondaryPillSelected by remember { mutableStateOf(false) }

    LaunchedEffect(secondaryTarget) {
        if (secondaryTarget) {
            // 挂载与置可见必须隔一帧：同帧内完成会让 AnimatedVisibility 首次组合时
            // 初始态与目标态都已是 true，跳过展开动画直接以完成态渲染
            secondaryMounted = true
            withFrameNanos {}
            secondaryVisible = true
        } else {
            secondaryVisible = false
            delay(SecondaryPillAnimDurationMs.toLong())
            secondaryMounted = false
            secondaryPillSelected = false
        }
    }

    LazyRow(
        modifier = Modifier.padding(top = MelodiaSpacing.sm),
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            // 二级药丸展开时，触发它的主药丸与二级药丸中间不留间距，拼成一个整体
            val joinsSecondary = index == SecondaryPillTriggerIndex && secondaryMounted
            item(key = label) {
                FilterPillChip(
                    text = label,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    modifier = Modifier
                        .padding(start = if (index == 0) 0.dp else 12.dp)
                        // 压在二级药丸上层，贴合处的圆角弧线才盖在最新的直边前面
                        .then(if (joinsSecondary) Modifier.zIndex(1f) else Modifier)
                )
            }
            if (joinsSecondary) {
                item(key = "secondary_demo_pill") {
                    AnimatedVisibility(
                        visible = secondaryVisible,
                        enter = expandHorizontally(
                            animationSpec = tween(SecondaryPillAnimDurationMs),
                            expandFrom = Alignment.Start
                        ) + fadeIn(animationSpec = tween(SecondaryPillAnimDurationMs)),
                        exit = shrinkHorizontally(
                            animationSpec = tween(SecondaryPillAnimDurationMs),
                            shrinkTowards = Alignment.Start
                        ) + fadeOut(animationSpec = tween(SecondaryPillAnimDurationMs)),
                        // Modifier.padding 不接受负值会直接崩溃，改用 layout{} 自己实现：
                        // 少上报一个圆角半径的宽度、内容整体左移，后面的胶囊据此自动跟上
                        modifier = Modifier.overlapStart(PillRadius)
                    ) {
                        // 未点击是标准未激活灰底，点击后颜色比主药丸的强调色更深，两者区分开
                        FilterPillChip(
                            text = "最新",
                            selected = secondaryPillSelected,
                            shape = PillShapeJoinEnd,
                            activeColor = MaterialTheme.colorScheme.primary.darken(SecondaryActiveDarkenFactor),
                            onClick = { secondaryPillSelected = true }
                        )
                    }
                }
            }
        }
    }
}

// 单个筛选药丸，选中态背景色/文字色带过渡动画
@Composable
private fun FilterPillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape = PillShapeFull,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.White.copy(alpha = 0.1f),
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "pillBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else Color.LightGray,
        label = "pillContent"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = MelodiaSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, fontSize = 14.sp)
    }
}
