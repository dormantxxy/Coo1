package com.example.c001apk.compose.ui.settings

import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.FollowType
import com.example.c001apk.compose.ThemeMode
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.base.PrefsViewModel
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.ParamsUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/5/31
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val networkRepo: NetworkRepo,
) : PrefsViewModel(userPreferencesRepository) {

    /**
     * 从酷安服务端实时拉取版本三字段（版本名 / API 版本 / 版本代码）。
     * 只有拉取成功才更新偏好 + CookieUtil 并连带重建 UA（保证 UA 与请求头一致）；
     * 拉取失败保留现有值，不做任何覆盖。
     * @param onSuccess 拉取成功后的回调（用于写 30 天缓存时间戳等）。
     */
    fun refreshVersionFromServer(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val v = ParamsUtils.fetchVersionInfo(networkRepo) ?: return@launch
            val current = userPreferencesRepository.data.first()
            // 用实时版本值连带重建 UA，保证 +CoolMarket/ 部分与 X-App-Version/X-App-Code 一致
            val ua = ParamsUtils.buildUserAgent(
                androidVersion = current.androidVersion,
                model = current.model,
                buildNumber = current.buildNumber,
                brand = current.brand,
                versionName = v.versionName,
                versionCode = v.versionCode,
            )
            userPreferencesRepository.setVersionName(v.versionName)
            userPreferencesRepository.setApiVersion(v.apiVersion)
            userPreferencesRepository.setVersionCode(v.versionCode)
            userPreferencesRepository.setUserAgent(ua)
            CookieUtil.versionName = v.versionName
            CookieUtil.apiVersion = v.apiVersion
            CookieUtil.versionCode = v.versionCode
            CookieUtil.userAgent = ua
            onSuccess?.invoke()
        }
    }

    fun setDarkTheme(value: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(value)
        }
    }

    fun setMaterialYou(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMaterialYou(value)
        }
    }

    fun setPureBlack(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPureBlack(value)
        }
    }

    fun setFontScale(value: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setFontScale(value)
        }
    }

    fun setContentScale(value: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setContentScale(value)
        }
    }

    fun setSZLMId(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSZLMId(value)
        }
    }

    fun setImageQuality(value: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setImageQuality(value)
        }
    }

    fun setImageFilter(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setImageFilter(value)
        }
    }

    fun setOpenInBrowser(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setOpenInBrowser(value)
        }
    }

    fun setShowSquare(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowSquare(value)
        }
    }

    fun setRecordHistory(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRecordHistory(value)
        }
    }

    fun setShowEmoji(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowEmoji(value)
        }
    }

    fun setCheckUpdate(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCheckUpdate(value)
        }
    }

    fun setCheckCount(value: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCheckCount(value)
        }
    }

    fun setVersionName(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setVersionName(value)
            CookieUtil.versionName = value
        }
    }

    fun setApiVersion(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setApiVersion(value)
            CookieUtil.apiVersion = value
        }
    }

    fun setVersionCode(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setVersionCode(value)
            CookieUtil.versionCode = value
        }
    }

    fun setManufacturer(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setManufacturer(value)
        }
    }

    fun setBrand(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBrand(value)
        }
    }

    fun setModel(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setModel(value)
        }
    }

    fun setBuildNumber(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBuildNumber(value)
        }
    }

    fun setSdkInt(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSdkInt(value)
        }
    }

    fun setAndroidVersion(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setAndroidVersion(value)
        }
    }

    fun setUserAgent(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserAgent(value)
            CookieUtil.userAgent = value
        }
    }

    fun setXAppDevice(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setXAppDevice(value)
        }
    }

    fun setXAppToken(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setXAppToken(value)
        }
    }

    fun setFollowType(value: FollowType) {
        viewModelScope.launch {
            userPreferencesRepository.setFollowType(value)
        }
    }

    fun setThemeType(value: ThemeType) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeType(value)
        }
    }

    fun setSeedColor(value: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSeedColor(value)
        }
    }

    fun setPaletteStyle(value: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setPaletteStyle(value)
        }
    }

}
