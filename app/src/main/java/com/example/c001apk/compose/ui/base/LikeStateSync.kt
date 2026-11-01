package com.example.c001apk.compose.ui.base

import androidx.compose.runtime.mutableStateMapOf

/**
 * 全局点赞状态同步缓存。
 *
 * 问题：详情页（FeedViewModel）点赞/取消后，返回首页列表，列表项的点赞数/颜色不更新
 * （两个页面是独立的 ViewModel 各自持有 loadingState）。
 *
 * 方案：点赞接口成功后，把新状态写入这里（id -> Liked(val liked: Boolean, count: String?)）。
 * 所有列表/详情展示处（FeedCard / FeedReplyCard 等）在渲染时先查此缓存，
 * 命中则用缓存值覆盖 data 里的默认值；未命中用原数据 → 全局实时同步。
 */
data class LikedState(
    val liked: Boolean,
    val count: String?,
)

object LikeStateSync {

    // id -> 点赞后最新状态（覆盖 data.userAction.like / data.likenum）
    val overrides = mutableStateMapOf<String, LikedState>()

    /** 是否命中缓存（某 id 有点赞覆盖状态） */
    fun get(id: String?): LikedState? = id?.let { overrides[it] }

    /** 点赞接口成功后写入（like: 1 表示当前已赞，翻转为 0；like: 0 表示未赞，翻转为 1） */
    fun update(id: String?, like: Int, count: String?) {
        if (id.isNullOrEmpty()) return
        overrides[id] = LikedState(
            liked = like != 1,
            count = count
        )
    }

    /** 清空（退出登录等场景） */
    fun clear() = overrides.clear()
}