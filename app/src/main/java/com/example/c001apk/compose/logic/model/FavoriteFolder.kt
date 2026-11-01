package com.example.c001apk.compose.logic.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地收藏夹。名称唯一，支持空收藏夹持久化。
 */
@Entity(tableName = "FavoriteFolder")
data class FavoriteFolder(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
