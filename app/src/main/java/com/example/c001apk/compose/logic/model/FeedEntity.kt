package com.example.c001apk.compose.logic.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FeedEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val uid: String,
    val uname: String,
    val avatar: String,
    val device: String,
    val message: String,
    val pubDate: String,
    val time: String = System.currentTimeMillis().toString(),
    /** 收藏夹名称，空字符串表示未分类（仅本地收藏使用） */
    @ColumnInfo(defaultValue = "''")
    val folder: String = "",
    /** 帖子标题，用于本地收藏精准搜索 */
    @ColumnInfo(defaultValue = "''")
    val title: String = "",
)
