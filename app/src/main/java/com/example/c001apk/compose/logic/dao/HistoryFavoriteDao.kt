package com.example.c001apk.compose.logic.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.c001apk.compose.logic.model.FavoriteFolder
import com.example.c001apk.compose.logic.model.FeedEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface HistoryFavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: FeedEntity)

    @Query("SELECT * FROM FeedEntity ORDER BY time DESC")
    suspend fun loadAllList(): List<FeedEntity>

    @Query("SELECT * FROM FeedEntity ORDER BY time DESC")
    fun loadAllListLive(): LiveData<List<FeedEntity>>

    @Query("SELECT * FROM FeedEntity ORDER BY time DESC")
    fun loadAllListFlow(): Flow<List<FeedEntity>>

    /** 浏览历史模糊搜索：正文 / 标题 / 用户名 / id */
    @Query(
        "SELECT * FROM FeedEntity WHERE " +
            "(message LIKE '%' || :keyword || '%' OR title LIKE '%' || :keyword || '%' " +
            "OR uname LIKE '%' || :keyword || '%' OR id = :keyword) " +
            "ORDER BY time DESC"
    )
    fun searchHistoryFlow(keyword: String): Flow<List<FeedEntity>>

    @Query("SELECT 1 FROM FeedEntity WHERE id = :id LIMIT 1")
    suspend fun isExist(id: String): Boolean

    @Query("DELETE FROM FeedEntity WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM FeedEntity")
    suspend fun deleteAll()

    @Query("DELETE FROM FeedEntity WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    // ---------- 本地收藏夹 ----------

    @Query("UPDATE FeedEntity SET folder = :folder WHERE id = :id")
    suspend fun updateFolder(id: String, folder: String)

    @Query("SELECT * FROM FeedEntity WHERE folder = :folder ORDER BY time DESC")
    fun loadFavoriteByFolderFlow(folder: String): Flow<List<FeedEntity>>

    @Query(
        "SELECT * FROM FeedEntity WHERE " +
            "(message LIKE '%' || :keyword || '%' OR title LIKE '%' || :keyword || '%' " +
            "OR uname LIKE '%' || :keyword || '%' OR id = :keyword) " +
            "ORDER BY time DESC"
    )
    fun searchFavoriteFlow(keyword: String): Flow<List<FeedEntity>>

    @Query(
        "SELECT * FROM FeedEntity WHERE folder = :folder AND " +
            "(message LIKE '%' || :keyword || '%' OR title LIKE '%' || :keyword || '%' " +
            "OR uname LIKE '%' || :keyword || '%' OR id = :keyword) " +
            "ORDER BY time DESC"
    )
    fun searchFavoriteInFolderFlow(keyword: String, folder: String): Flow<List<FeedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FavoriteFolder)

    @Query("SELECT * FROM FavoriteFolder ORDER BY createdAt ASC")
    fun loadFoldersFlow(): Flow<List<FavoriteFolder>>

    @Query("SELECT * FROM FavoriteFolder ORDER BY createdAt ASC")
    suspend fun loadFolders(): List<FavoriteFolder>

    @Query("DELETE FROM FavoriteFolder WHERE name = :name")
    suspend fun deleteFolder(name: String)

    @Query("UPDATE FeedEntity SET folder = '' WHERE folder = :name")
    suspend fun clearFolderFromItems(name: String)

    @Query("UPDATE FeedEntity SET folder = :newName WHERE folder = :oldName")
    suspend fun renameFolderItems(oldName: String, newName: String)

    @Query("DELETE FROM FavoriteFolder")
    suspend fun deleteAllFolders()
}
