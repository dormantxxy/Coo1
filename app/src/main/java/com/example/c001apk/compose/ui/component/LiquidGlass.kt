package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃通用容器（与底栏同视觉语言，Compose 纯实现，无灰边）。
 *
 * 特性：
 * - 半透明玻璃底（浅色=白半透明，深色=深灰半透明）
 * - 顶部高光（玻璃质感）
 * - 默认无发丝描边（用户要求移除灰边，用底栏的无边玻璃）
 * - 可选玻璃 tint（蓝色玻璃 / 白色玻璃）
 *
 * 适用于：顶栏、悬浮按钮、弹窗卡片、搜索框、加载卡、输入条等悬浮层。
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color? = null,
    borderAlpha: Float = 0f,
    glassAlpha: Float = 0.68f,
    highlightHeight: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // 省电模式：纯色背景（不透明、无玻璃质感），其余模式液态玻璃
    val battery = LiquidGlassState.isBatteryMode
    val glassColor = if (battery) {
        if (dark) Color(0xFF2A2A2C) else Color(0xFFF2F2F4)
    } else {
        tint ?: if (dark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    }
    // 无边线（borderAlpha 默认 0），与底栏一致
    val rimColor = if (dark) Color.White else Color(0xFF8E8E93)
    val effAlpha = if (battery) 1f else glassAlpha

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassColor.copy(alpha = effAlpha))
            .then(
                // 省电模式无线条
                if (borderAlpha > 0f && !battery) {
                    Modifier.border(0.7.dp, rimColor.copy(alpha = borderAlpha), shape)
                } else Modifier
            )
            .then(
                if (battery) Modifier
                else Modifier.liquidGlassHighlight(shape, highlightHeight, glassColor)
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * 顶部高光 + 微弱底部渐变，让半透明层有「玻璃」的折射感。
 */
private fun Modifier.liquidGlassHighlight(
    shape: Shape,
    highlightHeight: Dp,
    glassColor: Color,
    cornerRadius: Dp = 20.dp,
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        // 顶部高光
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (glassColor.luminance() < 0.5f) 0.14f else 0.45f),
                    Color.Transparent,
                )
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, highlightHeight.toPx()),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    }
)