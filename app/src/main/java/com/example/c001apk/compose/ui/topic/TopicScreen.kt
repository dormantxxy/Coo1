package com.example.c001apk.compose.ui.topic
import com.example.c001apk.compose.ui.component.Ios26Fab
import com.example.c001apk.compose.ui.component.Ios26TabbedTopBar
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

import androidx.compose.material.icons.filled.MoreHoriz
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.feed.reply.ReplyActivity
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/9
 */

enum class ProductSortType {
    REPLY, HOT, DATELINE
}

/** 排序项中文名（之前直接显示枚举名 REPLY/HOT/DATELINE，菜单里是英文） */
private val ProductSortType.displayName: String
    get() = when (this) {
        ProductSortType.REPLY -> "最近回复"
        ProductSortType.HOT -> "热度排序"
        ProductSortType.DATELINE -> "最新发布"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(
    onBackClick: () -> Unit,
    tag: String?,
    id: String?,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onSearch: (String, String, String) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val viewModel =
        hiltViewModel<TopicViewModel, TopicViewModel.ViewModelFactory>(key = id) { factory ->
            factory.create(
                url = when {
                    !tag.isNullOrEmpty() -> "/v6/topic/newTagDetail"
                    !id.isNullOrEmpty() -> "/v6/product/detail"
                    else -> throw IllegalArgumentException("参数为空")
                },
                tag = tag, id = id
            )
        }

    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { viewModel.tabList?.size ?: 0 })
    val scope = rememberCoroutineScope()
    var refreshState by remember { mutableStateOf(false) }
    var sortType by rememberSaveable { mutableStateOf(ProductSortType.REPLY) }
    var isScrollingUp by remember { mutableStateOf(false) }

    // 捕获话题页内容，供发帖悬浮按钮 drawBackdrop 实时模糊（与底栏同款真模糊）
    val fabBackdrop = rememberLayerBackdrop()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Ios26TabbedTopBar(
                rounded = true,
                tabs = viewModel.tabList?.map { it.title.orEmpty() }.orEmpty(),
                selectedIndex = pagerState.currentPage,
                onSelected = { index ->
                    if (pagerState.currentPage == index) refreshState = true
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                title = (viewModel.topicState as? LoadingState.Success)?.response?.title
                    ?: EMPTY_STRING,
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                actions = {
                    if (viewModel.topicState is LoadingState.Success) {
                        Row(Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(
                                onClick = {
                                    onSearch(
                                        viewModel.title,
                                        if (viewModel.entityType == "topic") "tag" else "product_phone",
                                        if (viewModel.entityType == "topic") viewModel.title else viewModel.id.orEmpty()
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                            Box {
                                IconButton(onClick = { dropdownMenuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreHoriz,
                                        contentDescription = null
                                    )
                                }
                                DropdownMenu(
                                    expanded = dropdownMenuExpanded,
                                    onDismissRequest = { dropdownMenuExpanded = false }
                                ) {
                                    if (viewModel.entityType == "product"
                                        && viewModel.tabList?.getOrNull(pagerState.currentPage)?.title == "讨论"
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("排序") },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                sortMenuExpanded = true
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                    if (isLogin) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (viewModel.isFollowed) "取消关注"
                                                    else "关注"
                                                )
                                            },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                if (viewModel.entityType == "topic")
                                                    viewModel.onGetFollow()
                                                else
                                                    viewModel.onPostFollow()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (viewModel.isBlocked) "取消拉黑"
                                                else "拉黑"
                                            )
                                        },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            viewModel.blockTopic()
                                        }
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    ProductSortType.entries.forEach { sort ->
                                        Row(
                                            modifier = Modifier
                                                .clickable {
                                                    sortMenuExpanded = false
                                                    sortType = sort
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = sort == sortType,
                                                onClick = {
                                                    sortMenuExpanded = false
                                                    sortType = sort
                                                }
                                            )
                                            Text(
                                                text = sort.displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(end = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (isLogin) {
                AnimatedVisibility(
                    visible = isScrollingUp,
                    enter = slideInVertically { it * 2 },
                    exit = slideOutVertically { it * 2 }
                ) {
                    // iOS26 液态玻璃悬浮按钮（原来这里是 Material3 FloatingActionButton）
                    Ios26Fab(
                        painter = rememberDrawablePainter(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.outline_note_alt_24,
                                context.theme
                            )
                        ),
                        backdrop = fabBackdrop,
                        onClick = {
                            val intent = Intent(context, ReplyActivity::class.java)
                            intent.putExtra("type", "createFeed")
                            intent.putExtra(
                                "targetType",
                                if (viewModel.entityType == "topic") "tag" else "product_phone"
                            )
                            intent.putExtra("targetId", viewModel.id)
                            if (viewModel.entityType == "topic")
                                intent.putExtra("title", viewModel.title)
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
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .layerBackdrop(fabBackdrop)
        ) {
            when (viewModel.topicState) {
                LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LoadingCard(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 10.dp),
                            state = viewModel.topicState,
                            onClick = if (viewModel.topicState is LoadingState.Loading) null
                            else viewModel::refresh
                        )
                    }
                }

                is LoadingState.Success -> {

                    viewModel.tabList?.let { tabList ->
                        LaunchedEffect(tabList, viewModel.selectedTab) {
                            val target = tabList.map { it.pageName }.indexOf(viewModel.selectedTab)
                            if (target > 0 && target < tabList.size && pagerState.currentPage != target) {
                                pagerState.scrollToPage(target)
                            }
                        }

                        HorizontalPager(
                            state = pagerState
                        ) { index ->
                            TopicContentScreen(
                                refreshState = refreshState,
                                resetRefreshState = {
                                    refreshState = false
                                },
                                paddingValues = PaddingValues(
                                    start = paddingValues.calculateLeftPadding(layoutDirection),
                                    bottom = paddingValues.calculateBottomPadding(),
                                ),
                                entityType = viewModel.entityType,
                                id = viewModel.id,
                                url = tabList[index].url.orEmpty(),
                                title = tabList[index].title.orEmpty(),
                                sortType = sortType,
                                onViewUser = onViewUser,
                                onViewFeed = onViewFeed,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onReport = onReport,
                                isScrollingUp = {
                                    isScrollingUp = it
                                }
                            )
                        }

                    }

                }
            }
        }

    }

    viewModel.toastText?.let {
        context.makeToast(it)
        viewModel.resetToastText()
    }

}
