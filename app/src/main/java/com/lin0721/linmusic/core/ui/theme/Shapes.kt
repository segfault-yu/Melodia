package com.lin0721.linmusic.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// MaterialTheme 的圆角梯度
val MelodiaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// BottomSheet 顶部圆角是非对称形状，单独定义
val BottomSheetShape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

// BottomSheet 拖拽把手的胶囊圆角
val DragHandleShape: Shape = RoundedCornerShape(2.dp)
