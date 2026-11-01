package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassShape = RoundedCornerShape(50)

@Composable
private fun glassBackground(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)

@Composable
private fun glassBorder(): Color =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

/**
 * iOS 26 风格胶囊按钮（带按压反馈）。
 */
@Composable
fun Ios26PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Ios26Pressable(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        scaleDown = 0.93f,
    ) {
        Row(
            modifier = Modifier
                .shadow(2.dp, GlassShape, clip = false)
                .clip(GlassShape)
                .background(glassBackground())
                .border(0.6.dp, glassBorder(), GlassShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * iOS 26 风格小圆形图标按钮（按压缩放 + 高亮）。
 */
@Composable
fun Ios26CircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Ios26Pressable(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape),
        scaleDown = 0.88f,
    ) { pressed ->
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (pressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    else glassBackground()
                )
                .border(0.6.dp, glassBorder(), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
