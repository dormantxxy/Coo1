package com.example.c001apk.compose.ui.search
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.IosTabRow
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.decode
import com.example.c001apk.compose.util.noRippleClickable
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/9
 */

enum class SearchType {
    FEED, APP, GAME, PRODUCT, USER, TOPIC
}

enum class SearchFeedType {
    ALL, FEED, ARTICLE, COOLPIC, COMMENT, RATING, ANSWER, QUESTION, VOTE
}

enum class SearchOrderType {
    DATELINE, HOT, REPLY
}

private val SearchType.displayName: String
    get() = when (this) {
        SearchType.FEED -> "综合"
        SearchType.APP -> "应用"
        SearchType.GAME -> "游戏"
        SearchType.PRODUCT -> "产品"
        SearchType.USER -> "用户"
        SearchType.TOPIC -> "话题"
    }

private val SearchFeedType.displayName: String
    get() = when (this) {
        SearchFeedType.ALL -> "全部"
        SearchFeedType.FEED -> "动态"
        SearchFeedType.ARTICLE -> "文章"
        SearchFeedType.COOLPIC -> "酷图"
        SearchFeedType.COMMENT -> "评论"
        SearchFeedType.RATING -> "评分"
        SearchFeedType.ANSWER -> "回答"
        SearchFeedType.QUESTION -> "提问"
        SearchFeedType.VOTE -> "投票"
    }

private val SearchOrderType.displayName: String
    get() = when (this) {
        SearchOrderType.DATELINE -> "最新"
        SearchOrderType.HOT -> "热门"
        SearchOrderType.REPLY -> "回复"
    }

private fun pageTypeLabel(pageType: String?): String = when (pageType) {
    "user" -> "用户"
    "apk" -> "应用"
    "topic" -> "话题"
    "product" -> "产品"
    "game" -> "游戏"
    else -> pageType.orEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    onBackClick: () -> Unit,
    keyword: String,
    title: String?,
    pageType: String?,
    pageParam: String?,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    initialPage: Int = 0,
    updateInitPage: (Int) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val tabList = SearchType.entries
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (pageType.isNullOrEmpty()) tabList.size else 1 }
    )
    var refreshState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var orderMenuExpanded by remember { mutableStateOf(false) }
    var feedType by rememberSaveable { mutableStateOf(SearchFeedType.ALL) }
    var orderType by rememberSaveable { mutableStateOf(SearchOrderType.DATELINE) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Ios26TopBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .noRippleClickable { onBackClick() },
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = keyword.decode,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!title.isNullOrEmpty()) {
                            Text(
                                text = "${pageTypeLabel(pageType)}: $title",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            )
                        }
                    }
                },
                actions = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = pagerState.currentPage == 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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
                                listOf("筛选类型", "排序")
                                    .forEachIndexed { index, menu ->
                                        DropdownMenuItem(
                                            text = { Text(menu) },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                when (index) {
                                                    0 -> typeMenuExpanded = true
                                                    1 -> orderMenuExpanded = true
                                                }
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }

                            }
                            DropdownMenu(
                                expanded = typeMenuExpanded,
                                onDismissRequest = { typeMenuExpanded = false }
                            ) {
                                SearchFeedType.entries.forEach { feed ->
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                typeMenuExpanded = false
                                                feedType = feed
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = feedType == feed,
                                            onClick = {
                                                typeMenuExpanded = false
                                                feedType = feed
                                            }
                                        )
                                        Text(
                                            text = feed.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 16.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = orderMenuExpanded,
                                onDismissRequest = { orderMenuExpanded = false }
                            ) {
                                SearchOrderType.entries.forEach { order ->
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                orderMenuExpanded = false
                                                orderType = order
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = orderType == order,
                                            onClick = {
                                                orderMenuExpanded = false
                                                orderType = order
                                            }
                                        )
                                        Text(
                                            text = order.displayName,
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
                },
                rounded = true,
                bottomContent = if (pageType.isNullOrEmpty() && tabList.size > 1) {
                    {
                        IosTabRow(
                            tabs = tabList.map { it.displayName },
                            selectedIndex = pagerState.currentPage,
                            onSelected = { index ->
                                if (pagerState.currentPage == index) refreshState = true
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }
                } else null,
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {

            HorizontalPager(
                state = pagerState
            ) { index ->
                SearchContentScreen(
                    searchType = SearchType.valueOf(SearchType.entries[index].name),
                    keyword = keyword.decode,
                    pageType = pageType,
                    pageParam = pageParam,
                    refreshState = refreshState,
                    resetRefreshState = {
                        refreshState = false
                    },
                    feedType = feedType,
                    orderType = orderType,
                    paddingValues = PaddingValues(
                        start = paddingValues.calculateLeftPadding(layoutDirection),
                        bottom = paddingValues.calculateBottomPadding(),
                    ),
                    onViewUser = onViewUser,
                    onViewFeed = onViewFeed,
                    onOpenLink = onOpenLink,
                    onCopyText = onCopyText,
                    updateInitPage = {
                        updateInitPage(pagerState.currentPage)
                    },
                    onReport = onReport,
                )
            }
        }

    }

}
