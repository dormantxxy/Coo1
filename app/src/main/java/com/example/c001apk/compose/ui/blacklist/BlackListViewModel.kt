package com.example.c001apk.compose.ui.blacklist

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.StringEntity
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/16
 */
data class UserProfile(
    val uid: String,
    val username: String,
    val avatar: String,
)

@HiltViewModel(assistedFactory = BlackListViewModel.ViewModelFactory::class)
class BlackListViewModel @AssistedInject constructor(
    @Assisted val type: BlackListType = BlackListType.USER,
    private val blackListRepo: BlackListRepo,
    private val networkRepo: NetworkRepo,
) : ViewModel() {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(type: BlackListType): BlackListViewModel
    }

    val blackList: Flow<List<StringEntity>> = when (type) {
        BlackListType.USER -> blackListRepo.loadAllUserListFlow()
        BlackListType.TOPIC -> blackListRepo.loadAllTopicListFlow()
    }

    /** uid -> 用户名/头像；黑名单里不再只显示一串数字。 */
    val userProfiles = mutableStateMapOf<String, UserProfile>()
    private val requestedProfiles = mutableSetOf<String>()

    init {
        if (type == BlackListType.USER) {
            viewModelScope.launch {
                blackList.collect { list ->
                    list.forEach { fetchProfile(it.data) }
                }
            }
        }
    }

    private fun fetchProfile(uid: String) {
        if (uid.isBlank() || requestedProfiles.add(uid).not()) return
        viewModelScope.launch {
            networkRepo.getProfile(uid).collect { result ->
                val data = result.getOrNull()?.data
                if (data != null) {
                    userProfiles[uid] = UserProfile(
                        uid = uid,
                        username = data.username.orEmpty(),
                        avatar = data.userAvatar.orEmpty(),
                    )
                }
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                BlackListType.USER -> blackListRepo.deleteAllUser()
                BlackListType.TOPIC -> blackListRepo.deleteAllTopic()
            }
        }
    }

    fun delete(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                BlackListType.USER -> blackListRepo.deleteUid(data)
                BlackListType.TOPIC -> blackListRepo.deleteTopic(data)
            }
        }
    }

    var toastText by mutableStateOf<String?>(null)
        private set

    fun save(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                BlackListType.USER -> {
                    if (blackListRepo.checkUid(data))
                        toast()
                    else
                        blackListRepo.insertUid(data)
                }

                BlackListType.TOPIC -> {
                    if (blackListRepo.checkTopic(data))
                        toast()
                    else
                        blackListRepo.insertTopic(data)
                }
            }
        }
    }

    private fun toast() {
        toastText = "已存在"
    }

    fun reset() {
        toastText = null
    }

    fun insertList(list: List<StringEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                BlackListType.USER -> blackListRepo.insertUidList(list)
                BlackListType.TOPIC -> blackListRepo.insertTopicList(list)
            }
        }
    }

}