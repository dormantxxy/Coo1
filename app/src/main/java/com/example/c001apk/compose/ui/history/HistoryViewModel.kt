package com.example.c001apk.compose.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.FavoriteFolder
import com.example.c001apk.compose.logic.model.FeedEntity
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.HistoryFavoriteRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * Created by bggRGjQaUbCoE on 2024/6/17
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = HistoryViewModel.ViewModelFactory::class)
class HistoryViewModel @AssistedInject constructor(
    @Assisted val type: HistoryType = HistoryType.HISTORY,
    private val blackListRepo: BlackListRepo,
    private val historyFavoriteRepo: HistoryFavoriteRepo
) : ViewModel() {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(type: HistoryType): HistoryViewModel
    }

    val query = MutableStateFlow("")

    /** 空字符串 = 全部收藏夹 */
    val selectedFolder = MutableStateFlow("")

    val folders: Flow<List<FavoriteFolder>> =
        if (type == HistoryType.FAV) historyFavoriteRepo.loadFoldersFlow() else flowOf(emptyList())

    val dataList: Flow<List<FeedEntity>> =
        if (type == HistoryType.HISTORY) {
            // 浏览历史也支持模糊搜索
            query.debounce(200).flatMapLatest { q ->
                val keyword = q.trim()
                if (keyword.isEmpty())
                    historyFavoriteRepo.loadAllHistoryListFlow()
                else
                    historyFavoriteRepo.searchHistoryFlow(keyword)
            }
        } else {
            combine(query, selectedFolder) { q, f -> q.trim() to f }
                .flatMapLatest { (keyword, folder) ->
                    when {
                        keyword.isEmpty() && folder.isEmpty() ->
                            historyFavoriteRepo.loadAllFavoriteListFlow()

                        keyword.isEmpty() ->
                            historyFavoriteRepo.loadFavoriteByFolderFlow(folder)

                        folder.isEmpty() ->
                            historyFavoriteRepo.searchFavoriteFlow(keyword)

                        else ->
                            historyFavoriteRepo.searchFavoriteInFolderFlow(keyword, folder)
                    }
                }
        }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFolder(folder: String) {
        selectedFolder.value = folder
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyFavoriteRepo.createFolder(name)
        }
    }

    fun moveToFolder(id: String, folder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyFavoriteRepo.moveFavorite(id, folder)
        }
    }

    fun deleteFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyFavoriteRepo.deleteFolder(name)
            if (selectedFolder.value == name) selectedFolder.value = ""
        }
    }

    fun blockUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!blackListRepo.checkUid(uid)) {
                blackListRepo.saveUid(uid)
            }
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteFavByUid(uid)
                HistoryType.HISTORY -> historyFavoriteRepo.deleteHistoryByUid(uid)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteFavorite(id)
                HistoryType.HISTORY -> historyFavoriteRepo.deleteHistory(id)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteAllFavorite()
                HistoryType.HISTORY -> historyFavoriteRepo.deleteAllHistory()
            }
        }
    }
}
