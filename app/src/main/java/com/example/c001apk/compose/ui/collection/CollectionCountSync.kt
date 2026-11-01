package com.example.c001apk.compose.ui.collection

import androidx.compose.runtime.mutableStateMapOf

/**
 * 收藏单条目数实时同步缓存。
 *
 * 问题：在「我的收藏」里进入某个收藏单，取消收藏里面的帖子后返回列表，
 * 收藏单卡片上的「N个内容」还是旧值，必须自己下拉刷新才更新
 * （列表页和收藏单内容页是两个独立 ViewModel，各持有自己的数据）。
 *
 * 方案：移除成功后，把该收藏单的条目数变化量记到这张全局表（id -> -n）；
 * CollectionCard 渲染时把变化量叠加到服务端返回的 itemNum 上，
 * 因为是 snapshot state，写进来的瞬间卡片上的数字就会自己变。
 * 收藏单列表重新从服务端拉取（首次加载 / 下拉刷新）时清空，避免重复扣减。
 */
object CollectionCountSync {

    /** 收藏单 id -> 本地累计变化量（负数表示比服务端少了几条） */
    val deltas = mutableStateMapOf<String, Int>()

    /** 从某个收藏单移除了一条收藏：该收藏单条目数 -1 */
    fun onRemoved(collectionId: String?, count: Int = 1) {
        if (collectionId.isNullOrEmpty()) return
        deltas[collectionId] = (deltas[collectionId] ?: 0) - count
    }

    /** 服务端返回的 itemNum 叠加本地变化量（结果不会小于 0） */
    fun apply(collectionId: String?, itemNum: String?): String? {
        val base = itemNum?.toIntOrNull() ?: return itemNum
        val delta = collectionId?.let { deltas[it] } ?: 0
        return (base + delta).coerceAtLeast(0).toString()
    }

    /** 列表已从服务端重新拉取，清空本地变化量 */
    fun clear() = deltas.clear()
}
