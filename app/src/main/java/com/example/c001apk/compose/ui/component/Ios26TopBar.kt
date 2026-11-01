package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS 26 风格顶栏，兼容 Material3 TopAppBar 的调用方式（保持 v15 行为）。
 *
 * 特点：
 * - 52dp 紧凑高度，17sp SemiBold 标题；
 * - 半透明 surface 底 + 极细底部分隔线；
 * - bottomContent 可放 ≤4 项的 tab；
 * - rounded = true 时底边 22dp iOS 26 圆角（默认关闭，仅指定页面开启）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ios26TopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = 52.dp,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    rounded: Boolean = false,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val container = colors.containerColor
    val bg = if (container.alpha > 0.01f) container.copy(alpha = 0.94f) else container
    val shape = if (rounded) {
        RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
    } else {
        RoundedCornerShape(0.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .windowInsetsPadding(windowInsets)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(expandedHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                ) {
                    title()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        bottomContent?.invoke()
        // iOS 26 细线：圆角时两端内缩，直角时通栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (rounded) 18.dp else 0.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))
        )
    }
}

/**
 * 带 tab 的 iOS 26 顶栏。
 *
 * 规则：
 * - tab 数量 <= 4：tab 直接占满顶栏标题位（顶栏不再显示标题文字），不额外占行；
 * - tab 数量 >= 5：标题留在顶栏，tab 放在顶栏下方一行原位横滑；
 * - tab 数量 <= 1：不显示 tab（单个 tab 没有意义），顶栏照常显示标题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ios26TabbedTopBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = 52.dp,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    rounded: Boolean = false,
    onLongPressTab: (() -> Unit)? = null,
) {
    val inline = tabs.size in 2..4

    Ios26TopBar(
        modifier = modifier,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        rounded = rounded,
        navigationIcon = navigationIcon,
        actions = actions,
        title = {
            if (inline) {
                // 顶栏字符提示不再显示，完全让位给 tab
                IosTabRow(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onSelected = onSelected,
                    modifier = Modifier.fillMaxWidth(),
                    onLongPress = onLongPressTab,
                )
            } else {
                Text(
                    text = title.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        bottomContent = if (tabs.size >= 5) {
            {
                IosTabRow(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    onSelected = onSelected,
                    onLongPress = onLongPressTab,
                )
            }
        } else null,
    )
}
