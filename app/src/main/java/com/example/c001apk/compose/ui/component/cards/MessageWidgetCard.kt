package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.theme.cardBg
import com.example.c001apk.compose.util.CookieUtil
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 消息页操作区：本地收藏 / 浏览历史 / 我的常去 / 我的收藏 / 我的赞 / 我的回复。
 *
 * 恢复为原来的「两行 × 三个图标文字」布局，不再使用 iOS26 玻璃小卡片。
 */
@Composable
fun MessageWidgetCard(
    modifier: Modifier = Modifier,
    onViewFFFList: (String?, String) -> Unit,
    onViewHistory: (String) -> Unit,
) {
    val historyClick: (String) -> Unit = { type -> onViewHistory(type) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(cardBg())
    ) {
        FFFCardRow(
            dataList = listOf(
                FFFCardItem(
                    title = "本地收藏",
                    value = null,
                    imageVector = Icons.Outlined.Archive,
                    type = FFFListType.FAV.name,
                    tint = Color(0xFF0A84FF)
                ),
                FFFCardItem(
                    title = "浏览历史",
                    value = null,
                    imageVector = Icons.Outlined.History,
                    type = FFFListType.HISTORY.name,
                    tint = Color(0xFF34C759)
                ),
                FFFCardItem(
                    title = "我的常去",
                    value = null,
                    imageVector = Icons.Outlined.MyLocation,
                    type = FFFListType.RECENT.name,
                    tint = Color(0xFFFF9F0A)
                ),
            ),
            onViewFFFList = { type ->
                onViewFFFList(CookieUtil.uid, type)
            },
            onViewHistory = historyClick,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        )

        FFFCardRow(
            dataList = listOf(
                FFFCardItem(
                    title = "我的收藏",
                    value = null,
                    imageVector = Icons.Outlined.StarOutline,
                    type = FFFListType.COLLECTION.name,
                    tint = Color(0xFFBF5AF2)
                ),
                FFFCardItem(
                    title = "我的赞",
                    value = null,
                    imageVector = Icons.Outlined.FavoriteBorder,
                    type = FFFListType.LIKE.name,
                    tint = Color(0xFFFF375F)
                ),
                FFFCardItem(
                    title = "我的回复",
                    value = null,
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    type = FFFListType.REPLY.name,
                    tint = Color(0xFF30B0C7)
                ),
            ),
            onViewFFFList = { type ->
                onViewFFFList(
                    if (type == FFFListType.COLLECTION.name) null else CookieUtil.uid,
                    type
                )
            },
            onViewHistory = {},
        )
    }
}
