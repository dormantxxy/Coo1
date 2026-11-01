package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.util.noRippleClickable
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

/**
 * iOS 26 液态玻璃圆形悬浮按钮（发布 / 回帖 / 应用详情共用）。
 *
 * 与底栏完全同一套实现：`drawBackdrop` 引用页面内容 backdrop，实时模糊背后内容
 * （vibrancy + blur），再叠一层半透明玻璃底色（无灰边）。传入 `backdrop` 即真模糊；
 * 不传时退化为半透明玻璃底。
 */
@Composable
fun Ios26Fab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Default.Add,
    painter: Painter? = null,
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    val shape = CircleShape
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.55f)
        else Color(0xFFFAFAFA).copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(shape)
            .then(
                if (backdrop != null)
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(12f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                else
                    Modifier
            )
            .noRippleClickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}