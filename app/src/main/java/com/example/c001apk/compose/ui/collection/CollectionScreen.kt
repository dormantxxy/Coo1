package com.example.c001apk.compose.ui.collection
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.FooterCard
import com.example.c001apk.compose.ui.component.ItemCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.makeToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.c001apk.compose.util.ReportType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.c001apk.compose.ui.component.Ios26SearchField

/** 按搜索关键词过滤 ItemCard 的加载状态。 */
private fun filterLoadingState(
    viewModel: CollectionViewModel,
): LoadingState<List<HomeFeedResponse.Data>> {
    val state = viewModel.loadingState
    return if (state is LoadingState.Success) {
        LoadingState.Success(viewModel.filteredList(state.response))
    } else state
}

/**
 * Created by bggRGjQaUbCoE on 2024/6/21
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBackClick: () -> Unit,
    id: String,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val viewModel =
        hiltViewModel<CollectionViewModel, CollectionViewModel.ViewModelFactory>(key = id) { factory ->
            factory.create(id = id)
        }
    // 和帖子详情页完全同一套移除逻辑（查 itemlist → collection_item_info.id → removeItem）
    val pickViewModel = hiltViewModel<CollectionPickViewModel>()
    val layoutDirection = LocalLayoutDirection.current
    val state = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val shouldShowTitle by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    var searchInput by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    // 正在播放「抽走」动画的条目 id：先原地收起，再从数据里移除
    var removingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Ios26TopBar(
                rounded = true,
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton {
                        onBackClick()
                    }
                },
                title = {
                    AnimatedContent(
                        shouldShowTitle,
                        label = EMPTY_STRING,
                    ) {
                        if (it) {
                            Text(text = viewModel.title)
                        } else {
                            Text(text = "收藏单")
                        }
                    }
                },
                bottomContent = {
                    // 收藏夹内搜索：模糊匹配正文 / 标题 / 用户名 / id
                    Ios26SearchField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            viewModel.searchQuery = it.text
                        },
                        placeholder = "在收藏夹内搜索",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                        onSearch = { viewModel.searchQuery = searchInput.text },
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    )
                },
            )
        }
    ) { paddingValues ->

        PullToRefreshBox(
            modifier = Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateLeftPadding(layoutDirection),
            ),
            state = state,
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                viewModel.isPull = true
                viewModel.refresh()
            },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = viewModel.isRefreshing,
                    state = state,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 10.dp + paddingValues.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                state = lazyListState
            ) {
                when (viewModel.collectionState) {
                    LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                        item(key = "collectionState") {
                            Box(modifier = Modifier.fillParentMaxSize()) {
                                LoadingCard(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 10.dp),
                                    state = viewModel.collectionState,
                                    onClick = if (viewModel.collectionState is LoadingState.Loading) null
                                    else viewModel::refresh
                                )
                            }
                        }
                    }

                    is LoadingState.Success -> {
                        val response = (viewModel.collectionState as LoadingState.Success).response
                        item(key = "title") {
                            Text(
                                text = viewModel.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                        item(key = "info") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoilLoader(
                                        url = response.userAvatar,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .clickable { onViewUser(response.uid.orEmpty()) }
                                    )
                                    Text(
                                        text = response.username.orEmpty(),
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 10.dp),
                                    )
                                    Text(
                                        text = "${fromToday(response.lastupdate ?: 0)}更新",
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 10.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${response.followNum}人关注",
                                        color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                response.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (viewModel.collectionState is LoadingState.Success) {

                    ItemCard(
                        // 传入按关键词过滤后的列表
                        loadingState = filterLoadingState(viewModel),

                        loadMore = viewModel::loadMore,
                        isEnd = viewModel.isEnd,
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText,
                        onReport = onReport,
                        onLike = viewModel::onLike,
                        onDelete = { id, deleteType, _ ->
                            viewModel.onDelete(id, deleteType)
                        },
                        onBlockUser = { uid, _ ->
                            viewModel.onBlockUser(uid)
                        },
                        // 收藏单里每条都是已收藏：按钮显示实心，点击就是「取消收藏本条」
                        forceCollected = true,
                        collapsingIds = removingIds,
                        onCollect = { feedId, _ ->
                            if (feedId !in removingIds) {
                                // 先播放抽走动画，同时发请求（已知收藏单 id，1 次请求就能拿到 itemId）
                                removingIds = removingIds + feedId
                                pickViewModel.removeFavorite(
                                    targetId = feedId,
                                    hintCollectionId = id,
                                ) { ok, msg ->
                                    if (ok) {
                                        viewModel.removeLocally(feedId)
                                        // 收藏单条目数 -1：返回「我的收藏」列表时卡片上的
                                        // 「N个内容」立即变，不用再手动下拉刷新
                                        CollectionCountSync.onRemoved(id)
                                    } else {
                                        context.makeToast(msg)
                                    }
                                    removingIds = removingIds - feedId
                                }
                            }
                        },
                    )

                    FooterCard(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        footerState = viewModel.footerState,
                        loadMore = viewModel::loadMore,
                    )
                }
            }
        }

    }

}