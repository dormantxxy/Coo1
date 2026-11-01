package com.example.c001apk.compose.logic.repository

import androidx.lifecycle.LiveData
import com.example.c001apk.compose.di.BrowseHistory
import com.example.c001apk.compose.di.FeedFavorite
import com.example.c001apk.compose.logic.dao.HistoryFavoriteDao
import com.example.c001apk.compose.logic.model.FavoriteFolder
import com.example.c001apk.compose.logic.model.FeedEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryFavoriteRepo @Inject constructor(
    @BrowseHistory
    private val browseHistoryDao: HistoryFavoriteDao,
    @FeedFavorite
    private val feedFavoriteDao: HistoryFavoriteDao,
) {

    fun loadAllHistoryListLive(): LiveData<List<FeedEntity>> {
        return browseHistoryDao.loadAllListLive()
    }

    fun loadAllHistoryListFlow(): Flow<List<FeedEntity>> {
        return browseHistoryDao.loadAllListFlow()
    }

    fun searchHistoryFlow(keyword: String): Flow<List<FeedEntity>> {
        return browseHistoryDao.searchHistoryFlow(keyword)
    }

    suspend fun insertHistory(history: FeedEntity) {
        browseHistoryDao.insert(history)
    }

    suspend fun checkHistory(id: String): Boolean {
        return browseHistoryDao.isExist(id)
    }

    /** 浏览历史：同一帖子再次浏览时刷新时间而不是直接跳过。 */
    suspend fun saveHistory(
        id: String,
        uid: String,
        uname: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String,
        title: String = "",
    ) {
        browseHistoryDao.insert(
            FeedEntity(
                id = id,
                uid = uid,
                uname = uname,
                avatar = avatar,
                device = device,
                message = message,
                pubDate = pubDate,
                title = title,
            )
        )
    }

    suspend fun deleteHistory(id: String) {
        browseHistoryDao.delete(id)
    }

    suspend fun deleteAllHistory() {
        browseHistoryDao.deleteAll()
    }

    // ---------- 本地收藏 ----------

    fun loadAllFavoriteListLive(): LiveData<List<FeedEntity>> {
        return feedFavoriteDao.loadAllListLive()
    }

    fun loadAllFavoriteListFlow(): Flow<List<FeedEntity>> {
        return feedFavoriteDao.loadAllListFlow()
    }

    fun loadFavoriteByFolderFlow(folder: String): Flow<List<FeedEntity>> {
        return feedFavoriteDao.loadFavoriteByFolderFlow(folder)
    }

    fun searchFavoriteFlow(keyword: String): Flow<List<FeedEntity>> {
        return feedFavoriteDao.searchFavoriteFlow(keyword)
    }

    fun searchFavoriteInFolderFlow(keyword: String, folder: String): Flow<List<FeedEntity>> {
        return feedFavoriteDao.searchFavoriteInFolderFlow(keyword, folder)
    }

    suspend fun insertFavorite(favorite: FeedEntity) {
        feedFavoriteDao.insert(favorite)
    }

    suspend fun checkFavorite(id: String): Boolean {
        return feedFavoriteDao.isExist(id)
    }

    suspend fun saveFavorite(
        id: String,
        uid: String,
        uname: String,
        avatar: String,
        device: String,
        message: String,
        pubDate: String,
        folder: String = "",
        title: String = "",
    ) {
        feedFavoriteDao.insert(
            FeedEntity(
                id = id,
                uid = uid,
                uname = uname,
                avatar = avatar,
                device = device,
                message = message,
                pubDate = pubDate,
                folder = folder,
                title = title,
            )
        )
    }

    suspend fun moveFavorite(id: String, folder: String) {
        feedFavoriteDao.updateFolder(id, folder)
    }

    suspend fun deleteFavorite(id: String) {
        feedFavoriteDao.delete(id)
    }

    suspend fun deleteAllFavorite() {
        feedFavoriteDao.deleteAll()
        feedFavoriteDao.deleteAllFolders()
    }

    suspend fun deleteHistoryByUid(uid: String) {
        browseHistoryDao.deleteByUid(uid)
    }

    suspend fun deleteFavByUid(uid: String) {
        feedFavoriteDao.deleteByUid(uid)
    }

    // ---------- 收藏夹 ----------

    fun loadFoldersFlow(): Flow<List<FavoriteFolder>> = feedFavoriteDao.loadFoldersFlow()

    suspend fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        feedFavoriteDao.insertFolder(FavoriteFolder(trimmed))
    }

    suspend fun deleteFolder(name: String) {
        feedFavoriteDao.clearFolderFromItems(name)
        feedFavoriteDao.deleteFolder(name)
    }

    suspend fun renameFolder(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == oldName) return
        if (feedFavoriteDao.loadFolders().any { it.name == trimmed }) return
        feedFavoriteDao.renameFolderItems(oldName, trimmed)
        feedFavoriteDao.insertFolder(FavoriteFolder(trimmed))
        feedFavoriteDao.deleteFolder(oldName)
    }
}
