package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.util.noRippleClickable

/**
 * iOS 26 液态玻璃风格的小胶囊按钮，用于帖子里的「回复数 / 点赞数」。
 * 半透明胶囊底 + 细高光描边 + 轻微浮起阴影，和系统液态玻璃底栏保持同一视觉语言。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Ios26IconText(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    textSize: Float = 12.5f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isLike: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    val color = if (isLike) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (isLike)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
    else
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val borderColor = if (isLike)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    else
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Row(
        modifier = modifier
            .shadow(1.5.dp, shape, clip = false)
            .clip(shape)
            .background(containerColor)
            .border(0.5.dp, borderColor, shape)
            .then(
                when {
                    onClick != null && onLongClick != null ->
                        Modifier.combinedClickable(onClick = { onClick() }, onLongClick = { onLongClick() })
                    onClick != null -> Modifier.noRippleClickable { onClick() }
                    else -> Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
        if (title.isNotEmpty()) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = title,
                color = color,
                fontSize = textSize.sp,
                lineHeight = textSize.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
