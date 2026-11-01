package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.base.PrefsViewModel
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.CookieUtil.SESSID
import com.example.c001apk.compose.util.encode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/6/3
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
    private val userPreferencesRepository: UserPreferencesRepository,
) : PrefsViewModel(userPreferencesRepository) {

    init {
        getCheckLoginInfo()
    }

    var badge by mutableIntStateOf(0)
        private set

    private fun getCheckLoginInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.checkLoginInfo()
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        response.body()?.data?.let { login ->
                            badge = login.notifyCount.badge
                            CookieUtil.atme = login.notifyCount.atme
                            CookieUtil.atcommentme = login.notifyCount.atcommentme
                            CookieUtil.feedlike = login.notifyCount.feedlike
                            CookieUtil.contacts_follow = login.notifyCount.contactsFollow
                            CookieUtil.message = login.notifyCount.message

                            userPreferencesRepository.apply {
                                setUid(login.uid)
                                setUserAvatar(login.userAvatar)
                                setUsername(login.username.encode)
                                setToken(login.token)
                                setIsLogin(true)
                            }
                            // 同步 CookieUtil 静态变量，当前会话 AddCookiesInterceptor 立即携带登录 Cookie
                            CookieUtil.isLogin = true
                            CookieUtil.uid = login.uid
                            CookieUtil.username = login.username.encode
                            CookieUtil.token = login.token
                        }

                        if (response.body()?.message == "登录信息有误") {
                            userPreferencesRepository.apply {
                                setUid(EMPTY_STRING)
                                setUserAvatar(EMPTY_STRING)
                                setUsername(EMPTY_STRING)
                                setToken(EMPTY_STRING)
                                setIsLogin(false)
                            }
                            // 同步 CookieUtil 静态变量 + 清 CookieManager，当前会话立即退出登录态
                            CookieUtil.isLogin = false
                            CookieUtil.uid = EMPTY_STRING
                            CookieUtil.username = EMPTY_STRING
                            CookieUtil.token = EMPTY_STRING
                            runCatching {
                                android.webkit.CookieManager.getInstance().removeAllCookies { }
                                android.webkit.CookieManager.getInstance().flush()
                            }
                        }

                        try {
                            val session = response.headers().values("Set-Cookie")[0]
                            SESSID = session.substring(0, session.indexOf(";"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    fun resetBadge() {
        badge = 0
    }

}