package com.example.c001apk.compose.ui.base

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.entityTemplateList
import com.example.c001apk.compose.constant.Constants.entityTypeList
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.FooterState
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.util.CookieUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */

enum class LikeType {
    FEED, REPLY
}

abstract class BaseViewModel(
    val networkRepo: NetworkRepo,
    val blackListRepo: BlackListRepo,
) : ViewModel() {

    var isRefreshing by mutableStateOf(false)

    var loadingState by mutableStateOf<LoadingState<List<HomeFeedResponse.Data>>>(LoadingState.Loading)

    var footerState by mutableStateOf<FooterState>(FooterState.Success)

    var page = 1
    var firstLaunch = 1
    var isLoadMore = false
    var isEnd = false
    var firstItem: String? = null
    var lastItem: String? = null

    abstract suspend fun customFetchData(): Flow<LoadingState<List<HomeFeedResponse.Data>>>

    /** 服务器端删除成功后，就直接把这条从当前列表里去掉（列表动画负责补位） */
    fun removeLocally(feedId: String) {
        val list = (loadingState as? LoadingState.Success)?.response ?: return
        fun match(item: HomeFeedResponse.Data) =
            item.id == feedId || item.entityId == feedId ||
                item.collectionItemInfo?.targetId == feedId
        if (list.none { match(it) }) return
        loadingState = LoadingState.Success(list.filterNot { match(it) })
    }

    fun fetchData() {
        viewModelScope.launch(Dispatchers.IO) {
            customFetchData().collect { state ->
                when (state) {
                    LoadingState.Empty -> {
                        if (loadingState is LoadingState.Success && !isRefreshing)
                            footerState = FooterState.End
                        else {
                            loadingState = state
                            footerState = FooterState.Success
                        }
                        isEnd = true
                    }

                    is LoadingState.Error -> {
                        if (loadingState is LoadingState.Success)
                            footerState = FooterState.Error(state.errMsg)
                        else
                            loadingState = state
                        isEnd = true
                    }

                    LoadingState.Loading -> {
                        if (loadingState is LoadingState.Success)
                            footerState = FooterState.Loading
                        else
                            loadingState = state
                    }

                    is LoadingState.Success -> {
                        page++
                        var response = state.response.filter {
                            (it.entityType in entityTypeList
                                    || it.entityTemplate in
                                    if (CookieUtil.showSquare) entityTemplateList
                                    else entityTemplateList.toMutableList()
                                        .also { list ->
                                            list.removeAll(
                                                listOf(
                                                    "iconMiniScrollCard",
                                                    "iconMiniGridCard"
                                                )
                                            )
                                        })
                                    && !blackListRepo.checkUid(
                                if (!it.fromuid.isNullOrEmpty()) it.fromuid
                                else it.uid.orEmpty()
                            )
                                    && !blackListRepo.checkTopic(
                                it.tags + it.ttitle +
                                        it.relationRows?.getOrNull(0)?.title
                            )
                        }
                        firstItem = response.firstOrNull()?.id
                        lastItem = response.lastOrNull()?.id

                        handleResponse(response)?.let {
                            response = it
                        }

                        if (isLoadMore) {
                            response =
                                ((loadingState as? LoadingState.Success)?.response ?: emptyList()) +
                                        response
                        }

                        handleLoadMore(response)?.let {
                            response = it
                        }

                        loadingState = LoadingState.Success(response)
                        footerState = FooterState.Success
                        if (response.isEmpty()) {
                            isLoadMore = false
                            isRefreshing = false
                            loadMore()
                        }
                    }
                }
                isLoadMore = false
                isRefreshing = false
            }
        }
    }

    open fun handleResponse(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data>? {
        return null
    }

    open fun handleLoadMore(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data>? {
        return null
    }

    open fun refresh() {
        if (!isRefreshing && !isLoadMore) {
            page = 1
            isEnd = false
            isLoadMore = false
            isRefreshing = true
            firstItem = null
            lastItem = null
            fetchData()
        }
    }

    open fun loadMore() {
        if (!isRefreshing && !isLoadMore) {
            isEnd = false
            isLoadMore = true
            fetchData()
            if (loadingState is LoadingState.Success) {
                footerState = FooterState.Loading
            } else {
                loadingState = LoadingState.Loading
            }
        }
    }

    var toastText by mutableStateOf<String?>(null)

    fun resetToastText() {
        toastText = null
    }

    /**
     * 点赞 / 取消点赞。
     *
     * 用户体验：点击立刻同步网络 + 图标 + 计数。
     * - 乐观更新：先本地翻转图标/计数并写入全局 LikeStateSync（所有列表/详情即时变色）
     * - 请求成功：用服务端返回的真实 count 校正 LikeStateSync 与本地列表
     * - 请求失败：回滚本地翻转，恢复服务端原值
     */
    fun onLike(id: String, like: Int, likeType: LikeType) {
        val targetLike = if (like == 1) 0 else 1 // 点击后目标状态
        val isLike = when (likeType) {
            LikeType.FEED -> if (like == 1) "unlike" else "like"
            LikeType.REPLY -> if (like == 1) "unLikeReply" else "likeReply"
        }
        val likeUrl = "/v6/feed/$isLike"

        // 1) 乐观更新：先翻转全局缓存 + 本地列表（立即响应点击）
        LikeStateSync.update(id, like, optimisticCount(id, like))
        optimisticUpdateLocalLike(id, targetLike)

        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postLike(likeUrl, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            // 服务端拒绝（消息非空）：回滚乐观状态
                            LikeStateSync.update(id, targetLike, null)
                            rollbackLocalLike(id, like)
                            toastText = response.message
                        } else if (response.data != null) {
                            // 成功：用真实 count 校正全局缓存，再由 UI 层读取覆盖
                            LikeStateSync.update(id, like, response.data.count)
                            if (handleLikeResponse(id, like, response.data.count) == null) {
                                val dataList = (loadingState as LoadingState.Success).response.map {
                                    if (it.id == id) {
                                        it.copy(
                                            likenum = response.data.count,
                                            userAction = it.userAction?.copy(like = if (like == 1) 0 else 1)
                                        )
                                    } else it
                                }
                                loadingState = LoadingState.Success(dataList)
                            }
                        }
                    } else {
                        // 网络失败：回滚乐观状态
                        LikeStateSync.update(id, targetLike, null)
                        rollbackLocalLike(id, like)
                        toastText = result.exceptionOrNull()?.message ?: "请求失败，请稍后重试"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    /** 乐观翻转当前列表里 id 对应项的点赞图标（不等待网络）。 */
    private fun optimisticUpdateLocalLike(id: String, targetLike: Int) {
        if (loadingState !is LoadingState.Success) return
        val dataList = (loadingState as LoadingState.Success).response.map {
            if (it.id == id) {
                it.copy(
                    likenum = optimisticCount(id, it.likenum?.toIntOrNull() ?: 0),
                    userAction = it.userAction?.copy(like = targetLike)
                )
            } else it
        }
        loadingState = LoadingState.Success(dataList)
    }

    /** 回滚本地列表（请求失败时恢复原 like 状态，计数回原值）。 */
    private fun rollbackLocalLike(id: String, like: Int) {
        if (loadingState !is LoadingState.Success) return
        val dataList = (loadingState as LoadingState.Success).response.map {
            if (it.id == id) {
                it.copy(
                    likenum = optimisticCount(id, it.likenum?.toIntOrNull() ?: 0, reverse = true),
                    userAction = it.userAction?.copy(like = like)
                )
            } else it
        }
        loadingState = LoadingState.Success(dataList)
    }

    /** 计算乐观翻转后的计数：like=1(已赞)→取消 减1；like=0(未赞)→点赞 加1。 */
    private fun optimisticCount(id: String, like: Int, reverse: Boolean = false): String {
        val current = (LikeStateSync.get(id)?.count?.toIntOrNull())
            ?: (loadingState as? LoadingState.Success)?.response
                ?.firstOrNull { it.id == id }?.likenum?.toIntOrNull()
            ?: 0
        val delta = if (reverse) -1 else 1
        val direction = if (like == 1) -delta else delta // 已赞→取消 减少；未赞→点赞 增加
        return (current + direction).coerceAtLeast(0).toString()
    }

    open fun handleLikeResponse(id: String, like: Int, count: String?): Boolean? {
        return null
    }

    fun onDelete(id: String, deleteType: LikeType) {
        val url = if (deleteType == LikeType.FEED) "/v6/feed/deleteFeed"
        else "/v6/feed/deleteReply"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postDelete(url, id)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            toastText = data.message
                        } else if (data.data?.count == "删除成功") {
                            var response = (loadingState as LoadingState.Success).response
                            handleDeleteResponse(id, response)?.let {
                                response = it
                                loadingState = LoadingState.Success(response)
                            }
                            toastText = data.data.count
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "请求失败，请稍后重试"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    open fun handleDeleteResponse(
        id: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        return response.filterNot { it.id == id }
    }

    open fun onBlockUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)

            if (loadingState is LoadingState.Success) {
                var response =
                    (loadingState as LoadingState.Success).response.filterNot { it.uid == uid }
                handleBlockUser(uid, response)?.let {
                    response = it
                }
                loadingState = LoadingState.Success(response)
            }
        }
    }

    open fun handleBlockUser(
        uid: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        return null
    }

    fun onFollowUser(uid: String, isFollow: Int) {
        val url = if (isFollow == 1) "/v6/user/unfollow" else "/v6/user/follow"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postFollowUnFollow(url, uid)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            toastText = response.message
                        } else {
                            val follow = if (isFollow == 1) 0 else 1
                            if (handleFollowResponse(follow) == null) {
                                val dataList = (loadingState as LoadingState.Success).response.map {
                                    if (it.uid == uid)
                                        it.copy(isFollow = follow)
                                    else it
                                }
                                loadingState = LoadingState.Success(dataList)
                            }
                            toastText = if (follow == 1) "关注成功"
                            else "取消关注成功"
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "请求失败，请稍后重试"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    open fun handleFollowResponse(follow: Int): Boolean? {
        return null
    }

}