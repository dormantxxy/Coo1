package com.example.c001apk.compose.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS 26 紧凑 tab 行（不再等分宽度）。
 *
 * 视觉与 v15 一致：每个 tab 按文字宽度自然排布，间距紧凑，
 * 选中态由「一个滑动胶囊」跟随（按选中项的实际宽高定位），
 * 因此点击 / 左右滑动时其它 tab 不会闪背景。
 *
 * 对齐规则：
 * - centerWhenFits = true 时：内容宽度 <= 容器宽度 → 整行居中（首页顶栏栏目变少时用）
 * - centerWhenFits = false（默认）：一直左对齐 / 横向滚动，与其它页面原样一致
 * - 内容宽度 > 容器宽度：左侧对齐并横向滚动，选中项自动滚入可见区
 * - tabs.size <= 1：不渲染
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 10,
    onLongPress: (() -> Unit)? = null,
    centerWhenFits: Boolean = false,
) {
    if (tabs.size <= 1) return

    val safeIndex = selectedIndex.coerceIn(0, tabs.size - 1)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // 每项的位置与尺寸（px）
    var itemOffsets by remember(tabs) {
        mutableStateOf(List(tabs.size) { Offset.Zero })
    }
    var itemSizes by remember(tabs) {
        mutableStateOf(List(tabs.size) { androidx.compose.ui.unit.IntSize.Zero })
    }

    // 容器宽度 / 内容宽度：用于判断是否需要滚动 / 是否居中
    var containerWidth by remember { mutableIntStateOf(0) }
    var contentWidth by remember { mutableIntStateOf(0) }
    val fits = centerWhenFits && contentWidth > 0 && containerWidth > 0 && contentWidth <= containerWidth

    val capsuleShape = RoundedCornerShape(10.dp)

    // 选中项自动滚入可见区（仅在需要滚动时）
    LaunchedEffect(safeIndex, tabs.size, fits) {
        if (fits) return@LaunchedEffect
        if (safeIndex !in itemOffsets.indices) return@LaunchedEffect
        val x = itemOffsets[safeIndex].x
        val w = itemSizes[safeIndex].width
        if (w <= 0) return@LaunchedEffect
        val viewport = scrollState.viewportSize
        if (viewport <= 0) return@LaunchedEffect
        val target = when {
            x < scrollState.value -> x.toInt()
            x + w > scrollState.value + viewport -> (x + w - viewport).toInt()
            else -> return@LaunchedEffect
        }
        runCatching { scrollState.animateScrollTo(target.coerceAtLeast(0)) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { containerWidth = it.width }
            .then(if (fits) Modifier else Modifier.horizontalScroll(scrollState)),
        contentAlignment = if (fits) Alignment.Center else Alignment.CenterStart,
    ) {
        Box(modifier = Modifier.onSizeChanged { contentWidth = it.width }) {
            // 滑动胶囊（唯一动效来源）
            val offsetX = itemOffsets.getOrNull(safeIndex)?.x ?: 0f
            val widthPx = itemSizes.getOrNull(safeIndex)?.width ?: 0
            val alpha by animateFloatAsState(
                targetValue = if (widthPx > 0) 1f else 0f,
                animationSpec = tween(120),
                label = "capsuleAlpha",
            )
            if (widthPx > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = with(density) { offsetX.toDp() })
                        .width(with(density) { widthPx.toDp() })
                        .height(30.dp)
                        .clip(capsuleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f * alpha)
                        )
                        .align(Alignment.CenterStart)
                )
            }

            Row(
                modifier = Modifier.height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = index == safeIndex
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInParent()
                                if (itemOffsets.getOrNull(index) != pos) {
                                    itemOffsets = itemOffsets.toMutableList().also { it[index] = pos }
                                }
                                if (itemSizes.getOrNull(index) != coords.size) {
                                    itemSizes = itemSizes.toMutableList().also { it[index] = coords.size }
                                }
                            }
                            .clip(capsuleShape)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelected(index) },
                                onLongClick = onLongPress,
                            )
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.5.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 兼容旧调用名的可滚动 tab 栏，内部统一走 [IosTabRow]。
 */
@Composable
fun IosScrollableTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp),
) {
    IosTabRow(
        tabs = tabs,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        modifier = modifier,
    )
}
