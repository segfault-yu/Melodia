package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MelodiaPress
import com.lin0721.linmusic.core.ui.theme.PressStyle

// 触控区尺寸与 M3 IconButton 对齐，调用点自带 size 时以调用点为准
private val IconButtonSize = 40.dp
private const val DisabledContentAlpha = 0.38f

// M3 IconButton 的替代品：去掉水波纹，改按压缩放。播放主控传 MelodiaPress.Transport
@Composable
fun MelodiaIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PressStyle = MelodiaPress.Icon,
    // 圆底必须由本组件画：调用点的 modifier 排在缩放层外侧，在那里加 background 底色不会跟着缩
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(IconButtonSize)
            .pressable(
                style = style,
                shape = CircleShape,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = LocalContentColor.current
        CompositionLocalProvider(
            LocalContentColor provides
                if (enabled) contentColor else contentColor.copy(alpha = DisabledContentAlpha),
            content = content
        )
    }
}
