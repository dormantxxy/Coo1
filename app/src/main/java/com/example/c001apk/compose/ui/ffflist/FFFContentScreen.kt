package com.example.c001apk.compose.ui.ffflist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.ui.collection.CollectionCountSync
import com.example.c001apk.compose.ui.collection.CollectionPickViewModel
import com.example.c001apk.compose.ui.component.CommonScreen
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */
@Composable
fun FFFContentScreen(
    uid: String?,
    id: String?,
    type: String,
    paddingValues: PaddingValues,
    refreshState: Boolean?,
    resetRefreshState: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewFFFList: (String?, String, String?, String?) -> Unit,
) {

    val viewModel =
        hiltViewModel<FFFContentViewModel, FFFContentViewModel.ViewModelFactory>(key = id + uid + type) { factory ->
            factory.create(
                url = when (type) {
                    FFFListType.FEED.name -> "/v6/user/feedList?showAnonymous=0&isIncludeTop=1"
                    FFFListType.FOLLOW.name, FFFListType.USER_FOLLOW.name -> "/v6/user/followList"
                    FFFListType.APK.name -> "/v6/user/apkFollowList"
                    FFFListType.FAN.name -> "/v6/user/fansList"
                    FFFListType.RECENT.name -> "/v6/user/recentHistoryList"
                    FFFListType.LIKE.name -> "/v6/user/likeList"
                    FFFListType.REPLY.name -> "/v6/user/replyList"
                    FFFListType.REPLYME.name -> "/v6/user/replyToMeList"
                    FFFListType.COLLECTION.name -> "/v6/collection/list"
                    FFFListType.COLLECTION_ITEM.name -> "/v6/collection/itemList"
                    else -> EMPTY_STRING
                },
                uid = uid, id = id, showDefault = if (type.contains("COLLECTION")) 0 else null
            )
        }

    var showRecentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 收藏单内容页（我的收藏 → 某个收藏单）：列表里每条本来就已收藏。
    // 点击收藏图标 = 从该收藏单移除，走和帖子详情完全同一套逻辑
    // （CollectionPickViewModel.removeFavorite：itemlist → collection_item_info.id → removeItem）。
    val isCollectionItem = type == FFFListType.COLLECTION_ITEM.name
    val pickViewModel = hiltViewModel<CollectionPickViewModel>()
    var removingIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val collectHandler: ((String, Boolean) -> Unit)? = if (isCollectionItem) {
        { feedId, _ ->
            if (feedId !in removingIds) {
                // 先播放抽走动画，同时发请求
                removingIds = removingIds + feedId
                pickViewModel.removeFavorite(
                    targetId = feedId,
                    hintCollectionId = id,
                ) { ok, msg ->
                    if (ok) {
                        viewModel.removeLocally(feedId)
                        // 收藏单条目数 -1（返回「我的收藏」列表时即时生效）
                        CollectionCountSync.onRemoved(id)
                    } else {
                        context.makeToast(msg)
                    }
                    removingIds = removingIds - feedId
                }
            }
        }
    } else null

    CommonScreen(
        viewModel = viewModel,
        refreshState = refreshState,
        resetRefreshState = resetRefreshState,
        paddingValues = paddingValues,
        onViewUser = onViewUser,
        onViewFeed = onViewFeed,
        onOpenLink = onOpenLink,
        onCopyText = onCopyText,
        onReport = onReport,
        onViewFFFList = onViewFFFList,
        onCollect = collectHandler,
        forceCollected = isCollectionItem,
        collapsingIds = removingIds,
        onDeleteCollection = { collectionId ->
            if (type == FFFListType.COLLECTION.name) {
                viewModel.deleteCollection(collectionId) { ok, msg ->
                    context.makeToast(msg)
                }
            }
        },
        onHandleRecent = { actionId, targetId, targetType, isTop ->
            viewModel.actionId = actionId
            viewModel.targetId = targetId
            viewModel.targetType = targetType
            viewModel.isTop = isTop
            showRecentDialog = true
        },
    )

    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

    when {
        showRecentDialog -> {
            Dialog(onDismissRequest = { showRecentDialog = false }) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors()
                        .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (viewModel.isTop == 1) "移除置顶" else "置顶",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecentDialog = false
                                    viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.TOP)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "删除",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecentDialog = false
                                    viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.DELETE)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "清空全部",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecentDialog = false
                                    viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.DELETE_ALL)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }

}