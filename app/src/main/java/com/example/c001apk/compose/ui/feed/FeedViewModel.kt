package com.example.c001apk.compose.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.FeedArticleContentBean
import com.example.c001apk.compose.logic.model.FavoriteFolder
import com.example.c001apk.compose.logic.model.FeedEntity
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.HistoryFavoriteRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.FooterState
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.base.BaseViewModel
import com.example.c001apk.compose.ui.base.LikeStateSync
import com.example.c001apk.compose.ui.base.LikeType
import com.example.c001apk.compose.ui.history.HistoryType
import com.example.c001apk.compose.util.CookieUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/6
 */
@HiltViewModel(assistedFactory = FeedViewModel.ViewModelFactory::class)
class FeedViewModel @AssistedInject constructor(
    @Assisted var id: String,
    @Assisted var isViewReply: Boolean,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
    private val historyFavoriteRepo: HistoryFavoriteRepo,
) : BaseViewModel(networkRepo, blackListRepo) {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(id: String, isViewReply: Boolean): FeedViewModel
    }

    var feedState by mutableStateOf<LoadingState<HomeFeedResponse.Data>>(LoadingState.Loading)
        private set

    var itemSize = 1

    lateinit var replyType: String
    private var discussMode: Int = 1
    var listType: String = "lastupdate_desc"
    var feedType: String = "feed"
    private var blockStatus = 0
    var fromFeedAuthor = 0

    private var topId: String? = null
    private var meId: String? = null

    var articleList: List<FeedArticleContentBean>? = null

    init {
        // 详情与评论并行加载：评论不依赖详情结果（feedUid 只用于后续标记 [楼主]），
        // 因此同时发出，省掉「详情完成 → 再拉评论」的一个 RTT，评论更快出来。
        fetchFeedData()
        fetchData()
    }

    var replyCount by mutableStateOf("0")
    var isFav by mutableStateOf(false)
    var isBlocked by mutableStateOf(false)

    /** 本地收藏夹列表（用于收藏时的文件夹选择） */
    val localFolders = historyFavoriteRepo.loadFoldersFlow()

    /** 官方「我的收藏」收藏夹列表 */
    var officialCollections by mutableStateOf<List<HomeFeedResponse.Data>>(emptyList())
        private set
    var officialLoading by mutableStateOf(false)
        private set

    private suspend fun recordHistory(
        username: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String,
        folder: String = "",
        title: String = "",
        type: HistoryType = HistoryType.HISTORY
    ) {
        when (type) {
            HistoryType.FAV -> historyFavoriteRepo.saveFavorite(
                id = id,
                uid = feedUid,
                uname = username,
                avatar = avatar,
                device = device,
                message = message,
                pubDate = pubDate,
                folder = folder,
                title = title,
            )

            HistoryType.HISTORY -> historyFavoriteRepo.saveHistory(
                id = id,
                uid = feedUid,
                uname = username,
                avatar = avatar,
                device = device,
                message = message,
                pubDate = pubDate,
                title = title,
            )
        }
    }
    private fun fetchFeedData() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getFeedContent("/v6/feed/detail?id=$id")
                .collect { state ->
                    if (state is LoadingState.Success) {
                        val response = state.response

                        feedUid = response.uid.orEmpty()
                        feedUsername = response.username.orEmpty()
                        replyCount = response.replynum.orEmpty()
                        feedTypeName = response.feedTypeName.orEmpty()
                        feedType = response.feedType.orEmpty()

                        if (response.messageRawOutput != "null") {
                            val listType =
                                object : TypeToken<List<FeedArticleContentBean?>?>() {}.type
                            val message: List<FeedArticleContentBean> =
                                Gson().fromJson(response.messageRawOutput, listType)

                            articleList = message.filter {
                                it.type in listOf("text", "image", "shareUrl")
                            }

                            if (!response.messageCover.isNullOrEmpty()) itemSize++
                            if (!response.title.isNullOrEmpty()) itemSize++
                            if (response.targetRow != null || !response.relationRows.isNullOrEmpty())
                                itemSize++
                            itemSize += articleList?.size ?: 0
                        }

                        if (!response.topReplyRows.isNullOrEmpty()) {
                            response.topReplyRows?.getOrNull(0)?.let { reply ->
                                topId = reply.id
                                val unameTag = when (reply.uid) {
                                    feedUid -> " [楼主]"
                                    else -> EMPTY_STRING
                                }
                                reply.username = "${reply.username}$unameTag [置顶]"
                                if (!reply.replyRows.isNullOrEmpty()) {
                                    reply.replyRows = reply.replyRows?.map {
                                        it.copy(
                                            message = generateMess(
                                                it,
                                                feedUid,
                                                reply.uid
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (!response.replyMeRows.isNullOrEmpty()) {
                            response.replyMeRows?.getOrNull(0)?.let { reply ->
                                meId = reply.id
                                if (!reply.replyRows.isNullOrEmpty()) {
                                    reply.replyRows = reply.replyRows?.map {
                                        it.copy(
                                            message = generateMess(
                                                it,
                                                feedUid,
                                                reply.uid
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 评论可能已并行加载（此时 feedUid 刚就绪）：重新标记 [楼主]/[层主]，幂等。
                        (loadingState as? LoadingState.Success)?.let { s ->
                            if (s.response.isNotEmpty()) {
                                loadingState = LoadingState.Success(handleResponse(s.response))
                            }
                        }

                        if (CookieUtil.recordHistory) {
                            recordHistory(
                                username = response.username.orEmpty(),
                                avatar = response.userAvatar.orEmpty(),
                                device = response.deviceTitle.orEmpty(),
                                message = with(response.message.orEmpty()) {
                                    if (this.length > 150) this.substring(0, 150) else this
                                },
                                pubDate = response.dateline.toString(),
                                title = response.title.orEmpty(),
                            )
                        }
                        isFav = historyFavoriteRepo.checkFavorite(id)
                        isBlocked = blackListRepo.checkUid(feedUid)
                    }
                    feedState = state
                    isRefreshing = false
                }
        }
    }

    lateinit var feedUid: String
    lateinit var feedUsername: String
    var feedTypeName by mutableStateOf(EMPTY_STRING)

    override suspend fun customFetchData() =
        networkRepo.getFeedContentReply(
            id, listType, page, firstItem, lastItem, discussMode,
            feedType, blockStatus, fromFeedAuthor
        )

    override fun handleResponse(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data> {
        // feedUid 可能尚未就绪（评论并行加载先于详情返回），此时不标记，详情回来后重标（幂等）
        val fu: String? = if (this::feedUid.isInitialized) feedUid else null
        response.forEach { item ->
            val baseName = item.username.orEmpty().replace(" [楼主]", "")
            item.username = if (fu != null && item.uid == fu) "$baseName [楼主]" else baseName
            if (!item.replyRows.isNullOrEmpty()) {
                item.replyRows = item.replyRows?.map {
                    it.copy(
                        message = if (fu != null) generateMess(it, fu, item.uid) else it.message
                    )
                }
            }
        }

        return response.distinctBy { it.entityId }
            .filterNot {
                if (listType == "lastupdate_desc")
                    it.id in listOf(topId, meId)
                else false
            }
    }

    var isPull = false
    override fun refresh() {
        if (!isRefreshing && !isLoadMore) {
            if (feedState is LoadingState.Success) {
                resetParams()
                isRefreshing = true
                fetchData()
            } else {
                if (isPull) {
                    isPull = false
                    viewModelScope.launch {
                        isRefreshing = true
                        delay(50)
                        isRefreshing = false
                    }
                }
                resetParams()
                feedState = LoadingState.Loading
                // 详情 + 评论并行
                fetchFeedData()
                fetchData()
            }
        }
    }

    private fun generateMess(
        reply: HomeFeedResponse.Data,
        feedUid: String?,
        uid: String?
    ): String = run {
        val replyTag =
            when (reply.uid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        val rReplyTag =
            when (reply.ruid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        val rReplyUser =
            when (reply.ruid) {
                uid -> EMPTY_STRING
                else -> """<a class="feed-link-uname" href="/u/${reply.ruid}">${reply.rusername}${rReplyTag}</a>"""
            }

        val replyPic =
            when (reply.pic) {
                EMPTY_STRING -> EMPTY_STRING
                else -> """ <a class=\"feed-forward-pic\" href=${reply.pic}>查看图片(${reply.picArr?.size})</a>"""
            }

        """<a class="feed-link-uname" href="/u/${reply.uid}">${reply.username}${replyTag}</a>回复${rReplyUser}: ${reply.message}${replyPic}"""

    }

    var isSheet: Boolean = false
    var frid: String? = null
    lateinit var replyId: String
    lateinit var replyUid: String
    lateinit var replyName: String
    private var replyPage = 1
    private var replyLastItem: String? = null
    var isEndReply = false
    private var isLoadMoreReply = false
    var replyLoadingState by mutableStateOf<LoadingState<List<HomeFeedResponse.Data>>>(LoadingState.Loading)
        private set
    var replyFooterState by mutableStateOf<FooterState>(FooterState.Success)
        private set

    private fun getReplyTop(): HomeFeedResponse.Data? {
        val replyTop =
            when (frid ?: replyId) {
                topId ->
                    (feedState as LoadingState.Success).response.topReplyRows?.getOrNull(0)

                meId ->
                    (feedState as LoadingState.Success).response.replyMeRows?.getOrNull(0)

                else ->
                    (loadingState as LoadingState.Success).response.find {
                        it.id == (frid ?: replyId)
                    }
            }
        return if (!frid.isNullOrEmpty())
            replyTop?.replyRows?.find { it.id == replyId }
        else replyTop
    }

    fun fetchTotalReply() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getReply2Reply(replyId, replyPage, replyLastItem)
                .collect { state ->
                    when (state) {
                        LoadingState.Empty -> {
                            if (replyLoadingState is LoadingState.Success && !isRefreshing)
                                replyFooterState = FooterState.End
                            else {
                                replyLoadingState = getReplyTop()?.let {
                                    LoadingState.Success(listOf(it))
                                } ?: state
                                replyFooterState = FooterState.End
                            }
                            isEndReply = true
                        }

                        is LoadingState.Error -> {
                            if (replyLoadingState is LoadingState.Success)
                                replyFooterState = FooterState.Error(state.errMsg)
                            else
                                replyLoadingState = state
                            isEndReply = true
                        }

                        LoadingState.Loading -> {
                            if (replyLoadingState is LoadingState.Success)
                                replyFooterState = FooterState.Loading
                            else
                                replyLoadingState = state
                        }

                        is LoadingState.Success -> {
                            replyPage++
                            var response = state.response.filter {
                                it.entityType == "feed_reply"
                            }
                            response = response.map { reply ->
                                reply.copy(
                                    username = generateName(reply, replyUid)
                                )
                            }
                            if (replyPage == 2) {
                                getReplyTop()?.let {
                                    response = listOf(it) + response
                                }
                            }
                            replyLastItem = response.lastOrNull()?.id
                            replyLoadingState =
                                if (isLoadMoreReply)
                                    LoadingState.Success(
                                        (((replyLoadingState as? LoadingState.Success)?.response
                                            ?: emptyList()) + response).distinctBy { it.entityId }
                                    )
                                else
                                    LoadingState.Success(response)
                            replyFooterState = FooterState.Success
                        }
                    }
                    isLoadMoreReply = false
                }
        }
    }

    private fun generateName(data: HomeFeedResponse.Data, uid: String): String = run {
        val replyTag =
            when (data.uid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        val rReplyTag =
            when (data.ruid) {
                feedUid -> " [楼主] "
                uid -> " [层主] "
                else -> EMPTY_STRING
            }

        if (data.ruid == "0")
            """<a class="feed-link-uname" href="/u/${data.uid}">${data.username}$replyTag</a>"""
        else
            """<a class="feed-link-uname" href="/u/${data.uid}">${data.username}$replyTag</a>回复<a class="feed-link-uname" href="/u/${data.rusername}">${data.rusername}$rReplyTag</a>"""
    }

    fun resetReplyState() {
        replyPage = 1
        isLoadMoreReply = false
        isEndReply = false
        replyLastItem = null
        frid = null
        replyLoadingState = LoadingState.Loading
        replyFooterState = FooterState.Success
    }

    fun loadMoreReply() {
        if (!isLoadMoreReply) {
            isEndReply = false
            isLoadMoreReply = true
            fetchTotalReply()
            if (replyLoadingState is LoadingState.Success) {
                replyFooterState = FooterState.Loading
            } else {
                replyLoadingState = LoadingState.Loading
            }
        }
    }

    override fun handleLikeResponse(id: String, like: Int, count: String?): Boolean? {
        return if (id in listOf(this.id, topId, meId)) {
            var response = (feedState as LoadingState.Success).response
            val isLike = if (like == 1) 0 else 1
            when (id) {
                this.id -> {
                    response = response.copy(
                        likenum = count,
                        userAction = HomeFeedResponse.UserAction(isLike)
                    )
                }

                topId -> {
                    response.topReplyRows?.getOrNull(0)?.let {
                        response = response.copy(
                            topReplyRows = listOf(
                                it.copy(
                                    likenum = count,
                                    userAction = HomeFeedResponse.UserAction(isLike)
                                )
                            )
                        )
                    }

                }

                meId -> {
                    response.replyMeRows?.getOrNull(0)?.let {
                        response = response.copy(
                            replyMeRows = listOf(
                                it.copy(
                                    likenum = count,
                                    userAction = HomeFeedResponse.UserAction(isLike)
                                )
                            )
                        )
                    }

                }
            }
            feedState = LoadingState.Success(response)
            true
        } else null
    }

    fun blockUser() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isBlocked)
                blackListRepo.deleteUid(feedUid)
            else
                blackListRepo.saveUid(feedUid)
            isBlocked = !isBlocked
        }
    }

    fun onFav() {
        if (isFav) removeLocalFavorite() else addLocalFavorite("")
    }

    /** 收藏到本地指定收藏夹（folder 为空 = 未分类） */
    fun addLocalFavorite(folder: String) {
        val response = (feedState as? LoadingState.Success)?.response ?: return
        viewModelScope.launch(Dispatchers.IO) {
            recordHistory(
                username = response.username.orEmpty(),
                avatar = response.userAvatar.orEmpty(),
                device = response.deviceTitle.orEmpty(),
                message = with(response.message.orEmpty()) {
                    if (this.length > 150) this.substring(0, 150) else this
                },
                pubDate = response.dateline.toString(),
                folder = folder,
                title = response.title.orEmpty(),
                type = HistoryType.FAV,
            )
            isFav = true
        }
    }

    fun removeLocalFavorite() {
        viewModelScope.launch(Dispatchers.IO) {
            historyFavoriteRepo.deleteFavorite(id)
            isFav = false
        }

    }

    /** 新建本地收藏夹并立即把当前帖子收进去 */
    fun addLocalFavoriteToNewFolder(folder: String) {
        val trimmed = folder.trim()
        if (trimmed.isEmpty()) return
        val response = (feedState as? LoadingState.Success)?.response ?: return
        viewModelScope.launch(Dispatchers.IO) {
            historyFavoriteRepo.createFolder(trimmed)
            recordHistory(
                username = response.username.orEmpty(),
                avatar = response.userAvatar.orEmpty(),
                device = response.deviceTitle.orEmpty(),
                message = with(response.message.orEmpty()) {
                    if (this.length > 150) this.substring(0, 150) else this
                },
                pubDate = response.dateline.toString(),
                folder = trimmed,
                title = response.title.orEmpty(),
                type = HistoryType.FAV,
            )
            isFav = true
        }
    }

    /** 拉取官方「我的收藏」收藏夹列表 */
    fun loadOfficialCollections() {
        if (officialLoading) return
        officialLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            // 必须与「我的收藏」页一致：showDefault = 0，否则只会返回默认收藏夹
            networkRepo.getFollowList(
                url = "/v6/collection/list",
                uid = null,
                id = null,
                showDefault = 0,
                page = 1,
                lastItem = null,
            ).collect { state ->
                when (state) {
                    is LoadingState.Success -> {
                        officialCollections = state.response
                        officialLoading = false
                    }

                    is LoadingState.Error, LoadingState.Empty -> {
                        officialLoading = false
                    }

                    else -> Unit
                }
            }
        }
    }

    /** 加入官方「我的收藏」（会同步到账号，换设备可恢复）
     * 官方接口 /v6/collection/addItem：id=收藏单id & cancelId= & targetId=帖子id & type=feed */
    fun addToOfficialCollection(
        collectionId: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf(
                "id" to collectionId,
                "cancelId" to "",
                "targetId" to id,
                "type" to "feed",
            )
            networkRepo.addToCollection(data).collect { result ->
                val response = result.getOrNull()
                val ok = response != null && response.error != 1
                onResult(ok, response?.message ?: if (ok) "已加入我的收藏" else "收藏失败")
            }
        }
    }

    /** 从收藏单移除单条动态（官方接口 /v6/collection/removeItem，itemId=collection_item_info.id） */
    fun removeFromOfficialCollection(
        itemId: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf(
                "itemId" to itemId,
            )
            networkRepo.removeFromCollection(data).collect { result ->
                val response = result.getOrNull()
                val ok = response != null && response.error != 1
                onResult(ok, response?.message ?: if (ok) "已取消我的收藏" else "操作失败")
            }
        }
    }

    /** 删除整个收藏单（官方接口 /v6/collection/delete，id=收藏单id） */
    fun deleteOfficialCollection(
        collectionId: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.deleteCollection(hashMapOf("id" to collectionId)).collect { result ->
                val response = result.getOrNull()
                val ok = response != null && response.error != 1
                onResult(ok, response?.message ?: if (ok) "已删除收藏单" else "删除失败")
            }
        }
    }

    /** 新建官方收藏夹 */
    fun createOfficialCollection(
        title: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.createCollection(hashMapOf("title" to trimmed)).collect { result ->
                val response = result.getOrNull()
                val ok = response != null && response.error != 1
                onResult(ok, response?.message ?: if (ok) "收藏夹已创建" else "创建失败")
                if (ok) {
                    delay(600)
                    loadOfficialCollections()
                }
            }
        }
    }
    override fun handleLoadMore(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data> {
        return response.distinctBy { it.entityId }
    }

    override fun handleBlockUser(
        uid: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        var feedResponse = (feedState as LoadingState.Success).response
        feedResponse.topReplyRows?.getOrNull(0)?.let { reply ->
            feedResponse = if (reply.uid == uid)
                feedResponse.copy(topReplyRows = null)
            else
                feedResponse.copy(
                    topReplyRows = listOf(
                        reply.copy(
                            replyRows = reply.replyRows?.filterNot { it.uid == uid }
                        )
                    )
                )
        }
        feedResponse.replyMeRows?.getOrNull(0)?.let { reply ->
            feedResponse = if (reply.uid == uid)
                feedResponse.copy(replyMeRows = null)
            else
                feedResponse.copy(
                    replyMeRows = listOf(
                        reply.copy(
                            replyRows = reply.replyRows?.filterNot { it.uid == uid }
                        )
                    )
                )
        }
        feedState = LoadingState.Success(feedResponse)
        return if (frid.isNullOrEmpty()) null
        else response.map { item ->
            if (item.id == frid) {
                item.copy(replyRows = item.replyRows?.filterNot { it.uid == uid })
            } else item
        }
    }

    fun onBlockReplyUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.saveUid(uid)

            if (replyLoadingState is LoadingState.Success) {
                val response =
                    (replyLoadingState as LoadingState.Success).response.filterNot { it.uid == uid }
                replyLoadingState = LoadingState.Success(response)
            }
        }
    }

    fun onDeleteRely(id: String, deleteType: LikeType) {
        val url = if (deleteType == LikeType.FEED) "/v6/feed/deleteFeed"
        else "/v6/feed/deleteReply"
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postDelete(url, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (!response.message.isNullOrEmpty()) {
                            toastText = response.message
                        } else if (response.data?.count == "删除成功") {
                            toastText = response.data.count
                            val dataList =
                                (replyLoadingState as LoadingState.Success).response.filterNot { it.id == id }
                            replyLoadingState = LoadingState.Success(dataList)
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "请求失败，请稍后重试"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun onLikeReply(id: String, like: Int, likeType: LikeType) {
        val targetLike = if (like == 1) 0 else 1 // 点击后目标状态
        val isLike = when (likeType) {
            LikeType.FEED -> if (like == 1) "unlike" else "like"
            LikeType.REPLY -> if (like == 1) "unLikeReply" else "likeReply"
        }
        val likeUrl = "/v6/feed/$isLike"

        // 乐观更新：点击立刻翻转回复点赞图标 + 全局缓存（跨页面即时同步）
        LikeStateSync.update(id, like, likeCountOf(id, like))
        optimisticUpdateReplyLike(id, targetLike)

        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postLike(likeUrl, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        if (response.data != null) {
                            if (!response.message.isNullOrEmpty()) {
                                // 服务端拒绝：回滚乐观状态
                                LikeStateSync.update(id, targetLike, null)
                                rollbackReplyLike(id, like)
                                toastText = response.message
                            } else {
                                // 成功：用真实 count 校正全局缓存
                                LikeStateSync.update(id, like, response.data.count)
                                if (handleLikeResponse(id, like, response.data.count) == null) {
                                    val dataList =
                                        (replyLoadingState as LoadingState.Success).response.map {
                                            if (it.id == id) {
                                                it.copy(
                                                    likenum = response.data.count,
                                                    userAction = it.userAction?.copy(like = if (like == 1) 0 else 1)
                                                )
                                            } else it
                                        }
                                    replyLoadingState = LoadingState.Success(dataList)
                                }
                            }
                        }
                    } else {
                        // 网络失败：回滚乐观状态
                        LikeStateSync.update(id, targetLike, null)
                        rollbackReplyLike(id, like)
                        toastText = result.exceptionOrNull()?.message ?: "请求失败，请稍后重试"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    /** 乐观翻转回复列表点赞图标（不等待网络）。 */
    private fun optimisticUpdateReplyLike(id: String, targetLike: Int) {
        if (replyLoadingState !is LoadingState.Success) return
        val dataList = (replyLoadingState as LoadingState.Success).response.map {
            if (it.id == id) {
                it.copy(
                    likenum = likeCountOf(id, 0, false),
                    userAction = it.userAction?.copy(like = targetLike)
                )
            } else it
        }
        replyLoadingState = LoadingState.Success(dataList)
    }

    /** 回滚回复列表点赞（请求失败时恢复原状态）。 */
    private fun rollbackReplyLike(id: String, like: Int) {
        if (replyLoadingState !is LoadingState.Success) return
        val dataList = (replyLoadingState as LoadingState.Success).response.map {
            if (it.id == id) {
                it.copy(
                    likenum = likeCountOf(id, 0, true),
                    userAction = it.userAction?.copy(like = like)
                )
            } else it
        }
        replyLoadingState = LoadingState.Success(dataList)
    }

    /** 计算乐观翻转后的回复点赞计数。like=1 已赞→取消减1；like=0 未赞→点赞加1。 */
    private fun likeCountOf(id: String, like: Int = 0, reverse: Boolean = false): String {
        val current = (LikeStateSync.get(id)?.count?.toIntOrNull())
            ?: (replyLoadingState as? LoadingState.Success)?.response
                ?.firstOrNull { it.id == id }?.likenum?.toIntOrNull()
            ?: 0
        val delta = if (reverse) -1 else 1
        val direction = if (like == 1) -delta else delta
        return (current + direction).coerceAtLeast(0).toString()
    }

    fun updateReply(data: HomeFeedResponse.Data) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isSheet) {
                isSheet = false
                val reply = data.copy(username = generateName(data, replyUid))
                var response = (replyLoadingState as? LoadingState.Success)?.response ?: emptyList()
                val position = response.map { it.id }.indexOf(replyId)
                response = response.toMutableList().also {
                    it.add(position + 1, reply)
                }
                replyLoadingState = LoadingState.Success(response)
            } else {
                val reply = data.copy(message = generateMess(data, feedUid, replyUid))
                if ((frid ?: replyId) in listOf(topId, meId)) {
                    var feedResponse = (feedState as LoadingState.Success).response
                    if ((frid ?: replyId) == topId)
                        feedResponse.topReplyRows?.getOrNull(0)?.let {
                            feedResponse = feedResponse.copy(
                                topReplyRows = listOf(
                                    it.copy(
                                        replyRows = (it.replyRows ?: emptyList()) + listOf(reply)
                                    )
                                )
                            )
                        }
                    else
                        feedResponse.replyMeRows?.getOrNull(0)?.let {
                            feedResponse = feedResponse.copy(
                                replyMeRows = listOf(
                                    it.copy(
                                        replyRows = (it.replyRows ?: emptyList()) + listOf(reply)
                                    )
                                )
                            )
                        }
                    feedState = LoadingState.Success(feedResponse)
                } else {
                    var response = (loadingState as? LoadingState.Success)?.response ?: emptyList()
                    response = if (replyType == "feed") {
                        listOf(data) + response
                    } else {
                        response.map { item ->
                            if (item.id == (frid ?: replyId)) {
                                item.copy(
                                    replyRows = (item.replyRows ?: emptyList()) + listOf(reply)
                                )
                            } else item
                        }
                    }
                    loadingState = LoadingState.Success(response)
                }
            }
        }
    }

    override fun handleDeleteResponse(
        id: String,
        response: List<HomeFeedResponse.Data>
    ): List<HomeFeedResponse.Data>? {
        return if (!frid.isNullOrEmpty()) {
            when (frid) {
                topId, meId -> {
                    var feedResponse = (feedState as LoadingState.Success).response
                    if (frid == topId)
                        feedResponse.topReplyRows?.getOrNull(0)?.let { reply ->
                            feedResponse = feedResponse.copy(
                                topReplyRows = listOf(
                                    reply.copy(replyRows = reply.replyRows?.filterNot { it.id == id })
                                )
                            )
                        }
                    else
                        feedResponse.replyMeRows?.getOrNull(0)?.let { reply ->
                            feedResponse = feedResponse.copy(
                                replyMeRows = listOf(
                                    reply.copy(replyRows = reply.replyRows?.filterNot { it.id == id })
                                )
                            )
                        }
                    feedState = LoadingState.Success(feedResponse)
                    null
                }

                else -> {
                    response.map { item ->
                        if (item.id == frid)
                            item.copy(replyRows = item.replyRows?.filterNot { it.id == id })
                        else item
                    }
                }
            }
        } else {
            if (id in listOf(topId, meId)) {
                var feedResponse = (feedState as LoadingState.Success).response
                feedResponse = if (id == topId)
                    feedResponse.copy(topReplyRows = null)
                else
                    feedResponse.copy(replyMeRows = null)
                feedState = LoadingState.Success(feedResponse)
                null
            } else super.handleDeleteResponse(id, response)
        }
    }

    fun refresh(id: String, isViewReply: Boolean) {
        this.id = id
        this.isViewReply = isViewReply
        resetParams()
        isRefreshing = false
        articleList = null
        itemSize = 2
        resetState()
        // 切换帖子时详情 + 评论并行
        fetchFeedData()
        fetchData()
    }

    private fun resetParams() {
        page = 1
        isEnd = false
        isLoadMore = false
        firstItem = null
        lastItem = null
    }

    fun resetState() {
        feedTypeName = EMPTY_STRING
        feedState = LoadingState.Loading
        loadingState = LoadingState.Loading
    }

}
