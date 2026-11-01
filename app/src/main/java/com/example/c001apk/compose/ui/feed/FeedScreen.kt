package com.example.c001apk.compose.ui.feed
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import com.example.c001apk.compose.ui.component.CoilLoader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.compose.material.icons.filled.MoreHoriz
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.LikeType
import com.example.c001apk.compose.ui.component.ArticleItem
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.FooterCard
import com.example.c001apk.compose.ui.component.Ios26Fab
import com.example.c001apk.compose.ui.component.ItemCard
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.c001apk.compose.ui.component.cards.FeedCard
import com.example.c001apk.compose.ui.component.cards.FeedHeader
import com.example.c001apk.compose.ui.component.cards.FeedReplyCard
import com.example.c001apk.compose.ui.component.cards.FeedReplySortCard
import com.example.c001apk.compose.ui.collection.CollectionPickDialog
import com.example.c001apk.compose.ui.collection.CollectionPickViewModel
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.feed.reply.ReplyActivity
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.ShareType
import com.example.c001apk.compose.util.Utils.richToString
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.getAllLinkAndText
import com.example.c001apk.compose.util.getShareText
import com.example.c001apk.compose.util.isScrollingUp
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.noRippleClickable
import com.example.c001apk.compose.util.shareText
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    isCompat: Boolean = true,
    onBackClick: () -> Unit,
    id: String,
    isViewReply: Boolean,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewHistory: () -> Unit = {},
) {

    val viewModel =
        hiltViewModel<FeedViewModel, FeedViewModel.ViewModelFactory>(key = if (isCompat) id else "KEY") { factory ->
            factory.create(id, isViewReply)
        }


    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val pickViewModel = hiltViewModel<CollectionPickViewModel>()
    val state = rememberPullToRefreshState()

    // 捕获帖子内容，供回复悬浮按钮 drawBackdrop 实时模糊（与底栏同款真模糊）
    val fabBackdrop = rememberLayerBackdrop()

    val localFolders by viewModel.localFolders.collectAsStateWithLifecycle(initialValue = emptyList())
    val officialCollections = viewModel.officialCollections
    var showLocalFolderDialog by remember { mutableStateOf(false) }
    var showOfficialDialog by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var showCollectDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var newLocalFolder by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    var dropdownMenuExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(true)
    var openBottomSheet by remember { mutableStateOf(false) }
    var viewReply by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val shouldShowSortCard by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > viewModel.itemSize - 1 }
    }
    val shouldShowTopCard by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }

    fun onShowTotalReply(id: String, uid: String, frid: String?) {
        openBottomSheet = true
        viewModel.replyId = id
        viewModel.replyUid = uid
        viewModel.frid = frid
        viewModel.resetReplyState()
        viewModel.fetchTotalReply()
    }

    fun setReplyType(index: Int) {
        selected = index
        viewModel.listType = when (index) {
            0 -> "lastupdate_desc"
            1 -> "dateline_desc"
            2 -> "popular"
            else -> EMPTY_STRING
        }
        viewModel.fromFeedAuthor = if (index == 3) 1 else 0
    }

    LaunchedEffect(id) {
        if (id != viewModel.id) {
            viewReply = false
            setReplyType(0)
            viewModel.refresh(id, isViewReply)
        }
    }

    val articleList = remember(key1 = viewModel.feedState) { viewModel.articleList }

    val replyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = if (SDK_INT >= 33)
                    result.data?.getParcelableExtra(
                        "response_data", HomeFeedResponse.Data::class.java
                    )
                else
                    result.data?.getParcelableExtra("response_data")
                data?.let {
                    viewModel.updateReply(it)
                    context.makeToast("回复成功")
                    if (viewModel.replyType == "feed" && shouldShowSortCard) {
                        scope.launch {
                            lazyListState.scrollToItem(viewModel.itemSize)
                        }
                    }
                }
            }
        }

    fun launchReply() {
        val intent = Intent(context, ReplyActivity::class.java)
        intent.putExtra("type", viewModel.replyType)
        intent.putExtra("rid", viewModel.replyId)
        intent.putExtra("username", viewModel.replyName)
        val options = ActivityOptionsCompat.makeCustomAnimation(
            context, R.anim.anim_bottom_sheet_slide_up, R.anim.anim_bottom_sheet_slide_down
        )
        replyLauncher.launch(intent, options)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Ios26TopBar(
                rounded = true,
                windowInsets = WindowInsets.systemBars
                    .only(
                        WindowInsetsSides.Top
                                + if (isCompat) WindowInsetsSides.Start else WindowInsetsSides.End
                    ),
                navigationIcon = {
                    BackButton {
                        if (!isCompat) {
                            viewModel.resetState()
                        }
                        onBackClick()
                    }
                },
                title = {
                    val success = viewModel.feedState as? LoadingState.Success<HomeFeedResponse.Data>
                    if (success != null) {
                        FeedTopBarUser(
                            data = success.response,
                            onViewUser = onViewUser,
                        )
                    } else {
                        Text(text = viewModel.feedTypeName)
                    }
                },
                actions = {
                    if (viewModel.feedState is LoadingState.Success) {
                        Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
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
                                listOf("复制链接", "分享")
                                    .forEachIndexed { index, menu ->
                                        DropdownMenuItem(
                                            text = { Text(menu) },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                when (index) {
                                                    0 -> context.copyText(
                                                        getShareText(ShareType.FEED, id)
                                                    )

                                                    1 -> context.shareText(
                                                        getShareText(ShareType.FEED, id)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                if (viewModel.isFav) {
                                    DropdownMenuItem(
                                        text = { Text("取消本地收藏") },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            viewModel.removeLocalFavorite()
                                        }
                                    )
                                } else {
                                    // 本地收藏简化为单一默认分组：点击直接保存当前帖子到本地（不跳转、不弹窗）
                                    DropdownMenuItem(
                                        text = { Text("收藏到本地") },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            viewModel.addLocalFavorite("")
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("我的收藏") },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        // 点击弹窗选择收藏单
                                        showCollectDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (viewModel.isBlocked) "取消拉黑" else "拉黑") },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        viewModel.blockUser()
                                    }
                                )
                                if (isLogin) {
                                    DropdownMenuItem(
                                        text = { Text("举报") },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            onReport(id, ReportType.FEED)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isLogin && viewModel.feedState is LoadingState.Success) {
                AnimatedVisibility(
                    modifier = if (!isCompat) Modifier.navigationBarsPadding() else Modifier,
                    visible = lazyListState.isScrollingUp(),
                    enter = slideInVertically { it * 2 },
                    exit = slideOutVertically { it * 2 }
                ) {
                    Ios26Fab(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        backdrop = fabBackdrop,
                        onClick = {
                            viewModel.replyId = viewModel.id
                            viewModel.replyUid = viewModel.feedUid
                            viewModel.replyName = viewModel.feedUsername
                            viewModel.replyType = "feed"
                            launchReply()
                        }
                    )
                }
            }
        },
    ) { paddingValues ->

        PullToRefreshBox(
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    end = if (isCompat) 0.dp else paddingValues.calculateRightPadding(layoutDirection),
                    start = if (isCompat) paddingValues.calculateLeftPadding(layoutDirection) else 0.dp
                )
                .layerBackdrop(fabBackdrop),
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = paddingValues.calculateBottomPadding() + 20.dp),
                state = lazyListState
            ) {
                when (viewModel.feedState) {
                    LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                        item(key = "feedState") {
                            Box(modifier = Modifier.fillParentMaxSize()) {
                                LoadingCard(
                                    modifier = Modifier.align(Alignment.Center),
                                    state = viewModel.feedState,
                                    onClick = if (viewModel.feedState is LoadingState.Loading) null
                                    else viewModel::refresh,
                                    isFeed = true,
                                )
                            }
                        }
                    }

                    is LoadingState.Success -> {
                        val response = (viewModel.feedState as LoadingState.Success).response
                        if (!articleList.isNullOrEmpty()) {
                            ArticleItem(
                                response = response,
                                articleList = articleList,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onLike = {
                                    if (isLogin) {
                                        viewModel.onLike(
                                            response.id.orEmpty(),
                                            response.userAction?.like ?: 0,
                                            LikeType.FEED
                                        )
                                    }
                                },
                                onViewUser = onViewUser,
                            )
                        } else {
                            item(key = "feed") {
                                FeedCard(
                                    isFeedContent = true,
                                    data = response,
                                    onViewUser = onViewUser,
                                    // 帖子内点评论直接下滑到评论区开头，不另开页面
                                    onViewFeed = { _, _ ->
                                        viewModel.isViewReply = true
                                    },
                                    onOpenLink = onOpenLink,
                                    onCopyText = onCopyText,
                                    onReport = onReport,
                                    onLike = viewModel::onLike,
                                    onDelete = { id, deleteType, frid ->
                                        viewModel.frid = frid
                                        viewModel.onDelete(id, deleteType)
                                    },
                                    onBlockUser = {},
                                    // 点击收藏：只有 1 个收藏单直接保存，多个弹窗；已收藏则移除
                                    onCollect = { id, want ->
                                        pickViewModel.quickFavorite(id, want) { needPick, _, msg ->
                                            if (needPick) showCollectDialog = true
                                            if (msg.isNotEmpty()) context.makeToast(msg)
                                        }
                                    },
                                    // 长按收藏：弹窗选择
                                    onCollectPick = { showCollectDialog = true },
                                )
                            }
                        }

                        item(key = "sort") {
                            FeedReplySortCard(
                                replyCount = viewModel.replyCount,
                                selected = selected,
                                updateSortReply = { index ->
                                    setReplyType(index)
                                    if (shouldShowSortCard)
                                        viewModel.isViewReply = true
                                    viewModel.refresh()
                                }
                            )
                        }

                        if (viewModel.listType == "lastupdate_desc") {
                            if (!response.topReplyRows.isNullOrEmpty()) {
                                response.topReplyRows?.getOrNull(0)?.let { reply ->
                                    item(key = "topReplyRows") {
                                        FeedReplyCard(
                                            data = reply,
                                            onViewUser = onViewUser,
                                            onShowTotalReply = ::onShowTotalReply,
                                            onOpenLink = onOpenLink,
                                            onCopyText = onCopyText,
                                            onReport = onReport,
                                            onLike = viewModel::onLike,
                                            onDelete = { id, deleteType, frid ->
                                                viewModel.frid = frid
                                                viewModel.onDelete(id, deleteType)
                                            },
                                            onBlockUser = { uid, frid ->
                                                viewModel.frid = frid
                                                viewModel.onBlockUser(uid)
                                            },
                                            onReply = { rid, uid, username, frid ->
                                                viewModel.replyId = rid
                                                viewModel.replyUid = uid
                                                viewModel.replyName = username
                                                viewModel.frid = frid
                                                viewModel.replyType = "reply"
                                                launchReply()
                                            },
                                        )
                                    }
                                }
                            }

                            if (!response.replyMeRows.isNullOrEmpty()) {
                                response.replyMeRows?.getOrNull(0)?.let { reply ->
                                    item(key = "replyMeRows") {
                                        FeedReplyCard(
                                            data = reply,
                                            onViewUser = onViewUser,
                                            onShowTotalReply = ::onShowTotalReply,
                                            onOpenLink = onOpenLink,
                                            onCopyText = onCopyText,
                                            onReport = onReport,
                                            onLike = viewModel::onLike,
                                            onDelete = { id, deleteType, frid ->
                                                viewModel.frid = frid
                                                viewModel.onDelete(id, deleteType)
                                            },
                                            onBlockUser = { uid, frid ->
                                                viewModel.frid = frid
                                                viewModel.onBlockUser(uid)
                                            },
                                            onReply = { rid, uid, username, frid ->
                                                viewModel.replyId = rid
                                                viewModel.replyUid = uid
                                                viewModel.replyName = username
                                                viewModel.frid = frid
                                                viewModel.replyType = "reply"
                                                launchReply()
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        if (viewModel.isViewReply) {
                            viewModel.isViewReply = false
                            scope.launch {
                                lazyListState.scrollToItem(viewModel.itemSize)
                            }
                        }
                    }
                }

                if (viewModel.feedState is LoadingState.Success) {

                    ItemCard(
                        loadingState = viewModel.loadingState,
                        loadMore = viewModel::loadMore,
                        isEnd = viewModel.isEnd,
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText,
                        onShowTotalReply = ::onShowTotalReply,
                        onReport = onReport,
                        onLike = viewModel::onLike,
                        onDelete = { id, deleteType, frid ->
                            viewModel.frid = frid
                            viewModel.onDelete(id, deleteType)
                        },
                        onBlockUser = { uid, frid ->
                            viewModel.frid = frid
                            viewModel.onBlockUser(uid)
                        },
                        onFollowUser = viewModel::onFollowUser,
                        onReply = { rid, uid, username, frid ->
                            viewModel.replyId = rid
                            viewModel.replyUid = uid
                            viewModel.replyName = username
                            viewModel.frid = frid
                            viewModel.replyType = "reply"
                            launchReply()
                        },
                    )

                    FooterCard(
                        footerState = viewModel.footerState,
                        loadMore = viewModel::loadMore,
                        isFeed = true
                    )

                }

            }
            if (shouldShowSortCard) {
                FeedReplySortCard(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    replyCount = viewModel.replyCount,
                    selected = selected,
                    updateSortReply = { index ->
                        setReplyType(index)
                        viewModel.isViewReply = true
                        viewModel.refresh()
                    }
                )
            }

        }

    }

    fun resetBottomSheet() {
        openBottomSheet = false
        viewModel.resetReplyState()
    }

    if (openBottomSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                resetBottomSheet()
            },
            sheetState = bottomSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
            ) {

                ItemCard(
                    loadingState = viewModel.replyLoadingState,
                    loadMore = viewModel::loadMoreReply,
                    isEnd = viewModel.isEndReply,
                    onViewUser = { uid ->
                        resetBottomSheet()
                        onViewUser(uid)
                    },
                    onViewFeed = { id, isViewReply ->
                        resetBottomSheet()
                        onViewFeed(id, isViewReply)
                    },
                    onOpenLink = { url, title ->
                        resetBottomSheet()
                        onOpenLink(url, title)
                    },
                    onCopyText = {
                        context.copyText(it?.getAllLinkAndText?.richToString())
                    },
                    onReport = { id, type ->
                        resetBottomSheet()
                        onReport(id, type)
                    },
                    isTotalReply = true,
                    onLike = { id, like, likeType ->
                        viewModel.onLikeReply(id, like, likeType)
                    },
                    onDelete = { id, deleteType, _ ->
                        viewModel.onDeleteRely(id, deleteType)
                    },
                    onBlockUser = { uid, _ ->
                        viewModel.onBlockReplyUser(uid)
                    },
                    isReply2Reply = !viewModel.frid.isNullOrEmpty(),
                    onShowTotalReply = { id, uid, frid ->
                        viewModel.replyId = id
                        viewModel.replyUid = uid
                        viewModel.frid = frid
                        viewModel.resetReplyState()
                        viewModel.fetchTotalReply()
                    },
                    onReply = { rid, uid, username, _ ->
                        viewModel.isSheet = true
                        viewModel.replyId = rid
                        viewModel.replyUid = uid
                        viewModel.replyName = username
                        viewModel.frid = null
                        viewModel.replyType = "reply"
                        launchReply()
                    },
                )

                FooterCard(
                    footerState = viewModel.replyFooterState,
                    loadMore = viewModel::loadMoreReply,
                    isFeed = true
                )
            }
        }
    }


    if (showLocalFolderDialog) {
        AlertDialog(
            onDismissRequest = { showLocalFolderDialog = false },
            title = { Text("收藏到本地", modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FolderPickRow("未分类") {
                        viewModel.addLocalFavorite("")
                        showLocalFolderDialog = false
                    }
                    localFolders.forEach { folder ->
                        FolderPickRow(folder.name) {
                            viewModel.addLocalFavorite(folder.name)
                            showLocalFolderDialog = false
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newLocalFolder,
                        onValueChange = { newLocalFolder = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("新建收藏夹") },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = newLocalFolder.isNotBlank(),
                    onClick = {
                        viewModel.addLocalFavoriteToNewFolder(newLocalFolder)
                        newLocalFolder = ""
                        showLocalFolderDialog = false
                    }
                ) {
                    Text("新建并收藏")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalFolderDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
        )
    }

    if (showOfficialDialog) {
        AlertDialog(
            onDismissRequest = { showOfficialDialog = false },
            title = { Text("我的收藏（官方同步）", modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (viewModel.officialLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    if (!viewModel.officialLoading && officialCollections.isEmpty()) {
                        Text(
                            text = "还没有官方收藏夹，点下方「新建收藏夹」创建一个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    officialCollections.forEach { collection ->
                        FolderPickRow(collection.title.orEmpty()) {
                            viewModel.addToOfficialCollection(collection.id.orEmpty()) { ok, msg ->
                                context.makeToast(msg)
                                if (ok) showOfficialDialog = false
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showOfficialDialog = false
                    showCreateCollectionDialog = true
                }) {
                    Text("新建收藏夹")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfficialDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
        )
    }

    if (showCreateCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = { Text("新建官方收藏夹", modifier = Modifier.fillMaxWidth()) },
            text = {
                OutlinedTextField(
                    value = newCollectionName,
                    onValueChange = { newCollectionName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("收藏夹名称") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newCollectionName.isNotBlank(),
                    onClick = {
                        viewModel.createOfficialCollection(newCollectionName) { _, msg ->
                            context.makeToast(msg)
                        }
                        newCollectionName = ""
                        showCreateCollectionDialog = false
                        showOfficialDialog = true
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
        )
    }
    if (showCollectDialog) {
        CollectionPickDialog(
            targetId = viewModel.id,
            onDismiss = { showCollectDialog = false },
            // 保存到收藏单成功后自动关闭弹窗，无需手动关闭
            onCollected = { showCollectDialog = false },
        )
    }

    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

}
@Composable
private fun FolderPickRow(name: String, onClick: () -> Unit) {
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FeedTopBarUser(
    data: HomeFeedResponse.Data,
    onViewUser: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { onViewUser(data.uid.orEmpty()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoilLoader(
            url = data.userAvatar,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.username.orEmpty(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 第二行：发帖时间 · 设备
            val device = data.deviceTitle.orEmpty().richToString()
            val timeText = data.dateline?.takeIf { it > 0 }
                ?.let { com.example.c001apk.compose.util.DateUtils.fromToday(it) }
                .orEmpty()
            val subtitle = listOf(timeText, device)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            Text(
                text = subtitle.ifBlank { " " },
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
