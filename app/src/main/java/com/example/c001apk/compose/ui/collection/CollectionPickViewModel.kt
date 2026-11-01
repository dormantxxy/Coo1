package com.example.c001apk.compose.ui.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 「收藏到收藏单」选择弹窗的 ViewModel。
 * 独立于帖子详情页，可在主页卡片 / 帖子详情处复用。
 */
@HiltViewModel
class CollectionPickViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
) : ViewModel() {

    var collections by mutableStateOf<List<HomeFeedResponse.Data>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var toastText by mutableStateOf<String?>(null)
        private set

    /** 拉取官方「我的收藏」收藏单列表（showDefault=0，与官方我的收藏页一致） */
    fun loadCollections() {
        if (loading) return
        loading = true
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getFollowList(
                url = "/v6/collection/list",
                uid = null,
                id = null,
                showDefault = 0,
                page = 1,
                lastItem = null,
            ).collect { state ->
                when (state) {
                    is LoadingState.Success -> collections = state.response
                    is LoadingState.Error -> toastText = state.errMsg
                    else -> Unit
                }
                loading = false
            }
        }
    }

    /** 把帖子加入指定收藏单（官方 /v6/collection/addItem）*/
    fun addToCollection(
        collectionId: String,
        targetId: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf(
                "id" to collectionId,
                "cancelId" to "",
                "targetId" to targetId,
                "type" to "feed",
            )
            networkRepo.addToCollection(data).collect { result ->
                val response = result.getOrNull()
                val ok = response != null && response.error != 1
                onResult(ok, response?.message ?: if (ok) "已收藏" else "收藏失败")
            }
        }
    }

    /** 新建收藏单并把帖子加进去 */
    fun createAndAdd(
        title: String,
        targetId: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "请输入收藏单名称")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.createCollection(hashMapOf("title" to trimmed)).collect { result ->
                val response = result.getOrNull()
                val created = response != null && response.error != 1
                if (!created) {
                    onResult(false, response?.message ?: "创建失败")
                } else {
                    // 创建成功后重新拉取列表，把帖子加入最新创建的收藏单
                    delay(600)
                    networkRepo.getFollowList(
                        url = "/v6/collection/list",
                        uid = null,
                        id = null,
                        showDefault = 0,
                        page = 1,
                        lastItem = null,
                    ).collect { state ->
                        if (state is LoadingState.Success) {
                            collections = state.response
                            val fresh = state.response.firstOrNull { it.title == trimmed }
                                ?: state.response.firstOrNull()
                            if (fresh != null) {
                                addToCollection(fresh.id.orEmpty(), targetId) { ok, msg ->
                                    onResult(ok, msg)
                                }
                            } else {
                                onResult(false, "创建成功但未找到收藏单")
                            }
                        } else {
                            onResult(true, "收藏单已创建")
                        }
                    }
                }
            }
        }
    }

    /**
     * 快速收藏：
     * - wantCollect=true：只有 1 个收藏单时直接加入；>=2 个时返回 needPick=true 让 UI 弹窗
     * - wantCollect=false：从收藏单移除该帖子
     * 回调：(needPick, collected, msg)
     */
    fun quickFavorite(
        targetId: String,
        wantCollect: Boolean,
        onResult: (needPick: Boolean, collected: Boolean, msg: String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var listState: LoadingState<List<HomeFeedResponse.Data>> = LoadingState.Loading
            networkRepo.getFollowList(
                url = "/v6/collection/list",
                uid = null,
                id = null,
                showDefault = 0,
                page = 1,
                lastItem = null,
            ).collect { listState = it }
            val list = (listState as? LoadingState.Success)?.response
            if (list == null) {
                onResult(false, false, "无法获取收藏单")
                return@launch
            }
            collections = list
            if (wantCollect) {
                when {
                    list.isEmpty() -> onResult(true, false, "")
                    list.size == 1 -> addToCollection(list[0].id.orEmpty(), targetId) { ok, msg ->
                        onResult(false, ok, msg)
                    }
                    else -> onResult(true, false, "")
                }
            } else {
                removeFavorite(targetId) { ok, msg -> onResult(false, ok, msg) }
            }
        }
    }

    /**
     * 从收藏单中移除该帖子：查 itemlist 拿 collection_item_info.id，再调 /v6/collection/removeItem。
     *
     * - 已知收藏单（hintCollectionId，收藏单列表页用）：只查这一个收藏单，1 次请求，最快；
     * - 未知收藏单（帖子页点收藏图标）：并行查所有收藏单，比原来串行快很多；
     * - 找不到时返回失败（不再「假成功」），让 UI 恢复显示并提示。
     */
    fun removeFavorite(
        targetId: String,
        hintCollectionId: String? = null,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1) 已知收藏单：走最短路径
            if (!hintCollectionId.isNullOrEmpty()) {
                val itemId = findItemId(hintCollectionId, targetId)
                if (itemId != null) {
                    removeItem(itemId, onResult)
                    return@launch
                }
            }

            // 2) 未知收藏单：并行扫所有收藏单
            var listState: LoadingState<List<HomeFeedResponse.Data>> = LoadingState.Loading
            networkRepo.getFollowList(
                url = "/v6/collection/list",
                uid = null,
                id = null,
                showDefault = 0,
                page = 1,
                lastItem = null,
            ).collect { listState = it }
            val list = (listState as? LoadingState.Success)?.response
            if (list == null) {
                withContext(Dispatchers.Main) { onResult(false, "无法获取收藏单") }
                return@launch
            }
            collections = list
            val itemIds = coroutineScope {
                list.map { col ->
                    async(Dispatchers.IO) { findItemId(col.id, targetId) }
                }.awaitAll()
            }
            val itemId = itemIds.firstOrNull { !it.isNullOrEmpty() }
            if (itemId == null) {
                withContext(Dispatchers.Main) { onResult(false, "没有找到该收藏，可能已在别处取消") }
            } else {
                removeItem(itemId, onResult)
            }
        }
    }

    /** 在指定收藏单的 itemlist 里找这条帖子的 collection_item_info.id */
    private suspend fun findItemId(collectionId: String?, targetId: String): String? {
        if (collectionId.isNullOrEmpty()) return null
        var state: LoadingState<List<HomeFeedResponse.Data>> = LoadingState.Loading
        networkRepo.getFollowList(
            url = "/v6/collection/itemlist",
            uid = null,
            id = collectionId,
            showDefault = null,
            page = 1,
            lastItem = null,
        ).collect { state = it }
        return (state as? LoadingState.Success)?.response
            ?.firstOrNull {
                it.id == targetId ||
                    it.entityId == targetId ||
                    it.collectionItemInfo?.targetId == targetId
            }
            ?.collectionItemInfo?.id
    }

    /** 真正调用官方移除接口 */
    private suspend fun removeItem(itemId: String, onResult: (Boolean, String) -> Unit) {
        networkRepo.removeFromCollection(hashMapOf("itemId" to itemId)).collect { r ->
            val resp = r.getOrNull()
            val ok = resp != null && resp.error != 1
            withContext(Dispatchers.Main) {
                onResult(ok, resp?.message ?: if (ok) "已取消收藏" else "取消失败")
            }
        }
    }

    fun resetToast() {
        toastText = null
    }
}