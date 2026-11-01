package com.example.c001apk.compose.ui.base

import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.c001Application
import com.example.c001apk.compose.constant.Constants
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.util.CookieUtil.szlmId
import com.example.c001apk.compose.util.ParamsUtils
import com.example.c001apk.compose.util.SzlmIdFetcher
import com.example.c001apk.compose.util.TokenDeviceUtils.encode
import com.example.c001apk.compose.util.Utils.randomMacAddress
import com.example.c001apk.compose.util.Utils.randomRecentDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by bggRGjQaUbCoE on 2024/6/15
 */
open class PrefsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    fun regenerateParams(random: Boolean = false) {
        viewModelScope.launch {
            regenerateParamsAndWait(random)
        }
    }

    /**
     * 与 regenerateParams 相同，但可 await（suspend）。
     * 用于首屏 bootstrap：先拿到 DUID（已写入 CookieUtil.szlmId）再生成含 DUID 的 X-App-Device，
     * 避免并发竞态导致 xAppDevice 首段为空。
     */
    suspend fun regenerateParamsAndWait(random: Boolean = false) {
        val context = c001Application

        val manufacturer: String
        val brand: String
        val model: String
        val buildNumber: String
        val sdkInt: String
        val androidVersion: String
        val deviceId: String

        if (random) {
            // 一次取出成套机型参数，厂商/品牌/型号/构建号/安卓版本严格对应。
            val device = randomRecentDevice()
            manufacturer = device.manufacturer
            brand = device.brand
            model = device.model
            buildNumber = device.buildNumber
            sdkInt = device.sdkInt
            androidVersion = device.androidVersion
            deviceId = randomMacAddress()
        } else {
            manufacturer = Build.MANUFACTURER.orEmpty()
            brand = Build.BRAND.orEmpty()
            model = Build.MODEL.orEmpty()
            buildNumber = Build.ID.orEmpty()
            sdkInt = Build.VERSION.SDK_INT.toString()
            androidVersion = Build.VERSION.RELEASE.orEmpty()
            deviceId = runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull().orEmpty().ifEmpty { randomMacAddress() }
        }

        // 版本三字段：优先用已存值（实时拉取过的），否则常量默认，保证 UA 与请求头一致。
        val current = userPreferencesRepository.data.first()
        val vName = current.versionName.ifBlank { Constants.VERSION_NAME }
        val vCode = current.versionCode.ifBlank { Constants.VERSION_CODE }

        val userAgent = ParamsUtils.buildUserAgent(
            androidVersion = androidVersion,
            model = model,
            buildNumber = buildNumber,
            brand = brand,
            versionName = vName,
            versionCode = vCode,
        )
        val xAppDevice =
            encode("$szlmId; ; ; $deviceId; $manufacturer; $brand; $model; $buildNumber; null")

        // 一次 updateData 写完，避免连续写入产生中间态。
        userPreferencesRepository.updateDeviceParams(
            manufacturer = manufacturer,
            brand = brand,
            model = model,
            buildNumber = buildNumber,
            sdkInt = sdkInt,
            androidVersion = androidVersion,
            userAgent = userAgent,
            xAppDevice = xAppDevice,
        )
    }

    /**
     * 恢复默认本机参数：直接用这台手机真实的 Build 信息覆盖随机机型。
     */
    fun restoreDeviceParams() {
        regenerateParams(random = false)
    }

    /**
     * 刷新数字联盟 ID：清掉本地身份三值重新生成，成功后再用新 DUID 重建 X-App-Device。
     * 失败时保留原有 szlmId，不会把设备标识写成空。
     */
    fun refreshSzlmId(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val context = c001Application
            SzlmIdFetcher.resetIdentity(context)
            val duid = withContext(Dispatchers.IO) {
                SzlmIdFetcher.fetch(context)
            }
            if (duid.isNullOrEmpty()) {
                onResult(false)
                return@launch
            }

            szlmId = duid
            userPreferencesRepository.setSZLMId(duid)

            val current = userPreferencesRepository.data.first()
            userPreferencesRepository.updateDeviceParams(
                manufacturer = current.manufacturer,
                brand = current.brand,
                model = current.model,
                buildNumber = current.buildNumber,
                sdkInt = current.sdkInt,
                androidVersion = current.androidVersion,
                userAgent = current.userAgent,
                xAppDevice = encode(
                    "$duid; ; ; ${randomMacAddress()}; ${current.manufacturer}; ${current.brand}; ${current.model}; ${current.buildNumber}; null"
                ),
            )
            onResult(true)
        }
    }
}
