package com.example.c001apk.compose.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ui.component.Ios26Fab
import com.example.c001apk.compose.ui.component.IosTabRow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.c001apk.compose.ui.feed.reply.ReplyActivity
import com.example.c001apk.compose.ui.home.app.AppListScreen
import com.example.c001apk.compose.ui.home.feed.HomeFeedScreen
import com.example.c001apk.compose.ui.home.topic.HomeTopicScreen
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.ReportType
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextAlign
import com.example.c001apk.compose.util.noRippleClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import com.example.c001apk.compose.util.makeToast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

/**
 * Created by bggRGjQaUbCoE on 2024/6/5
 */

enum class TabType {
    FOLLOW, APP, FEED, HOT, TOPIC, PRODUCT, COOLPIC
}

private val TabType.displayName: String
    get() = when (this) {
        TabType.FOLLOW -> "关注"
        TabType.APP -> "应用"
        TabType.FEED -> "头条"
        TabType.HOT -> "热榜"
        TabType.TOPIC -> "话题"
        TabType.PRODUCT -> "数码"
        TabType.COOLPIC -> "酷图"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    refreshState: Boolean,
    resetRefreshState: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onViewApp: (String) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 捕获页面内容，供悬浮按钮 drawBackdrop 实时模糊（与底栏同款真模糊）
    val fabBackdrop = rememberLayerBackdrop()

    // 隐藏栏目（只隐藏不删除，保证下次仍可勾选恢复）
    var hiddenTabs by remember { mutableStateOf(HomeTabPrefs.loadHidden(context)) }
    // 打开编辑弹窗前快照，用于「取消」还原
    var hiddenSnapshot by remember { mutableStateOf(hiddenTabs) }
    var showEditDialog by remember { mutableStateOf(false) }

    val tabList = remember(hiddenTabs) {
        TabType.entries.filter { it !in hiddenTabs }.ifEmpty { TabType.entries.toList() }
    }
    val feedPageIndex = tabList.indexOf(TabType.FEED).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = feedPageIndex,
        pageCount = { tabList.size }
    )
    var isScrollingUp by remember { mutableStateOf(false) }

    // 弹窗里实时增减栏目后，当前页可能越界，需要收回到最后一页
    LaunchedEffect(tabList.size) {
        if (pagerState.currentPage > tabList.lastIndex && tabList.isNotEmpty()) {
            runCatching { pagerState.scrollToPage(tabList.lastIndex) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            if (isLogin && pagerState.currentPage == feedPageIndex) {
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter = slideInVertically { it * 2 },
                    exit = slideOutVertically { it * 2 }
                ) {
                    Ios26Fab(
                        modifier = Modifier.padding(bottom = 78.dp),
                        backdrop = fabBackdrop,
                        onClick = {
                            val intent = Intent(context, ReplyActivity::class.java)
                            intent.putExtra("type", "createFeed")
                            val animationBundle = ActivityOptionsCompat.makeCustomAnimation(
                                context,
                                R.anim.anim_bottom_sheet_slide_up,
                                R.anim.anim_bottom_sheet_slide_down
                            ).toBundle()
                            ContextCompat.startActivity(context, intent, animationBundle)
                        }
                    )
                }
            }
        },
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
                .layerBackdrop(fabBackdrop),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IosTabRow(
                    tabs = tabList.map { it.displayName },
                    selectedIndex = pagerState.currentPage,
                    onSelected = { index ->
                        if (pagerState.currentPage == index) {
                            onRefresh()
                        }
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    modifier = Modifier.weight(1f),
                    horizontalPadding = 7,
                    centerWhenFits = true,
                    onLongPress = {
                        hiddenSnapshot = hiddenTabs
                        showEditDialog = true
                    },
                )
                IconButton(
                    onClick = { onSearch() },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                // 冷启动优化：默认 pager 会预加载左右相邻页，
                // 等于一启动就同时发起 3 个首页接口请求，抢带宽导致首屏更慢。
                // 这里只加载当前页，滑动时再按需创建。
                beyondViewportPageCount = 0,
            ) { index ->

                val safeIndex = index.coerceIn(0, tabList.lastIndex)
                when (val type = tabList[safeIndex]) {
                    TabType.FOLLOW, TabType.FEED, TabType.HOT, TabType.COOLPIC ->
                        HomeFeedScreen(
                            refreshState = refreshState,
                            resetRefreshState = resetRefreshState,
                            type = type,
                            onViewUser = onViewUser,
                            onViewFeed = onViewFeed,
                            onOpenLink = onOpenLink,
                            onCopyText = onCopyText,
                            onReport = onReport,
                            isScrollingUp = {
                                isScrollingUp = it
                            }
                        )

                    TabType.APP -> AppListScreen(
                        refreshState = refreshState,
                        resetRefreshState = resetRefreshState,
                        onViewApp = onViewApp,
                    )

                    TabType.TOPIC, TabType.PRODUCT -> HomeTopicScreen(
                        type = type,
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText
                    )
                }

            }

        }

    }

    // 长按 tab 打开栏目编辑。
    // 结构参照「字体大小」弹窗（标准 AlertDialog，状态栏表现正常），
    // 内容改为 iOS26 风格的 ✓ 勾选列表。
    // 勾选变化会**实时**作用到顶部 tab；点「取消」或点外部关闭则还原。
    if (showEditDialog) {
        val allTabs = TabType.entries

        AlertDialog(
            onDismissRequest = {
                hiddenTabs = hiddenSnapshot
                showEditDialog = false
            },
            title = {
                Text(
                    text = "编辑顶部栏目",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "打勾的显示在顶栏，取消勾选仅隐藏",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        allTabs.forEach { tab ->
                            val checked = tab !in hiddenTabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .noRippleClickable {
                                        val next = hiddenTabs.toMutableSet()
                                        if (checked) {
                                            // 至少保留一个可见栏目
                                            if (allTabs.count { it !in next } > 1) next.add(tab)
                                        } else {
                                            next.remove(tab)
                                        }
                                        // 实时应用：立刻更新顶部 tab
                                        hiddenTabs = next
                                    }
                                    .padding(horizontal = 6.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (checked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (checked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tab.displayName,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HomeTabPrefs.saveHidden(context, hiddenTabs)
                        showEditDialog = false
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    hiddenTabs = hiddenSnapshot
                    showEditDialog = false
                }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
        )
    }
}
