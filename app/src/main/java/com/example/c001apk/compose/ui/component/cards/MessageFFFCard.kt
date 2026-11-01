package com.example.c001apk.compose.ui.component.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.history.HistoryType
import com.example.c001apk.compose.util.CookieUtil

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */

data class FFFCardItem(
    val title: String,
    val value: String?,
    val imageVector: ImageVector?,
    val type: String,
    /** 图标着色（方案10 用主题色区分入口，不铺色块） */
    val tint: Color? = null,
)

/**
 * 消息页顶部三数字：动态 / 关注 / 粉丝。
 * 沿用方案10 的描边网格样式（纯数字，无图标）。
 */
@Composable
fun MessageFFFCard(
    modifier: Modifier = Modifier,
    fffList: List<String>,
    onViewFFFList: (String, String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        FFFCardRow(
            dataList = listOf(
                FFFCardItem(
                    title = "动态",
                    value = fffList.getOrNull(0),
                    imageVector = null,
                    type = FFFListType.FEED.name
                ),
                FFFCardItem(
                    title = "关注",
                    value = fffList.getOrNull(1),
                    imageVector = null,
                    type = FFFListType.FOLLOW.name
                ),
                FFFCardItem(
                    title = "粉丝",
                    value = fffList.getOrNull(2),
                    imageVector = null,
                    type = FFFListType.FAN.name
                ),
            ),
            onViewFFFList = { type -> onViewFFFList(if (CookieUtil.uid.isEmpty()) "" else CookieUtil.uid, type) },
            onViewHistory = {},
        )
    }
}

/**
 * 方案 10：极简描边网格。
 *
 * 视觉要点（对齐 iOS 26）：
 * - 整块只有一张卡片底，网格靠 0.5dp 发丝分隔线切分，不用一个个小格子；
 * - 每格「图标 + 文字」垂直居中，图标只上色不铺底板，保持克制；
 * - 按压时该格出现淡色高亮 + 轻微缩放，是唯一的动态反馈。
 */
@Composable
fun FFFCardRow(
    modifier: Modifier = Modifier,
    dataList: List<FFFCardItem>,
    onViewFFFList: (String) -> Unit,
    onViewHistory: (String) -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        dataList.forEachIndexed { index, item ->
            FFFCardItem(
                modifier = Modifier.weight(1f),
                onViewFFFList = {
                    when (item.type) {
                        FFFListType.FAV.name -> onViewHistory(HistoryType.FAV.name)
                        FFFListType.HISTORY.name -> onViewHistory(HistoryType.HISTORY.name)
                        else -> if (CookieUtil.isLogin) onViewFFFList(item.type)
                    }
                },
                title = item.title,
                value = item.value,
                imageVector = item.imageVector,
                tint = item.tint,
                showDivider = index != dataList.lastIndex,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FFFCardItem(
    modifier: Modifier = Modifier,
    onViewFFFList: () -> Unit,
    title: String,
    value: String?,
    imageVector: ImageVector?,
    tint: Color? = null,
    showDivider: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "ffCardPress",
    )
    val iconColor = tint ?: MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                    else Color.Transparent
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onViewFFFList() },
                )
                .padding(vertical = 15.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (imageVector == null) {
                Text(
                    text = value ?: EMPTY_STRING,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Image(
                    imageVector = imageVector,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconColor),
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(
                    if (value.isNullOrEmpty() && imageVector == null) 0f else 1f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 0.5dp 发丝分隔线（贴格子右侧，最后一个不画）
        if (showDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 14.dp)
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            )
        }
    }
}
