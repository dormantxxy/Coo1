package com.example.c001apk.compose.util

import com.example.c001apk.compose.constant.Constants
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.state.LoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 机型参数：版本三字段（versionName / apiVersion / versionCode）实时获取工具。
 *
 * 来源：官方酷安应用详情接口 getAppInfo("com.coolapk.market")，返回
 * apkversionname（如 16.6.5）与 apkversioncode（如 2610162）。
 *
 * - versionName = apkversionname
 * - versionCode = apkversioncode
 * - apiVersion  = apkversionname 第一个小数点前的数字（16.6.5 → 16）
 *
 * 拉取失败返回 null（调用方保留旧值，不覆盖）。
 */
object ParamsUtils {

    const val DEFAULT_VERSION_NAME = "16.6.6"
    const val DEFAULT_API_VERSION = "16"
    const val DEFAULT_VERSION_CODE = "2610261"

    data class VersionInfo(
        val versionName: String,
        val apiVersion: String,
        val versionCode: String,
    )

    /** 拉取酷安 App 的实时版本三字段（失败返回 null）。 */
    suspend fun fetchVersionInfo(networkRepo: NetworkRepo): VersionInfo? =
        withContext(Dispatchers.IO) {
            try {
                runCatching {
                    networkRepo.getAppInfo("com.coolapk.market")
                        .first { it !is LoadingState.Loading }
                }.getOrNull().let { state ->
                    val data = (state as? LoadingState.Success<HomeFeedResponse.Data>)?.response
                    if (data == null) return@let null
                    val vName = data.apkversionname.orEmpty()
                    if (vName.isBlank()) return@let null
                    VersionInfo(
                        versionName = vName,
                        apiVersion = vName.substringBefore(".").ifBlank { vName },
                        versionCode = data.apkversioncode.orEmpty().ifBlank {
                            DEFAULT_VERSION_CODE
                        },
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

    /** 返回要写入的版本三字段：拉取成功用实时值，失败用默认值。 */
    suspend fun resolveVersion(
        networkRepo: NetworkRepo,
        fallbackVersionName: String = DEFAULT_VERSION_NAME,
        fallbackApiVersion: String = DEFAULT_API_VERSION,
        fallbackVersionCode: String = DEFAULT_VERSION_CODE,
    ): VersionInfo = fetchVersionInfo(networkRepo) ?: VersionInfo(
        versionName = fallbackVersionName,
        apiVersion = fallbackApiVersion,
        versionCode = fallbackVersionCode,
    )

    /**
     * 统一生成 CoolMarket User-Agent。
     * 版本三字段与机型参数均由调用方传入，避免与请求头脱节。
     */
    fun buildUserAgent(
        androidVersion: String,
        model: String,
        buildNumber: String,
        brand: String,
        versionName: String,
        versionCode: String,
    ): String =
        "Dalvik/2.1.0 (Linux; U; Android $androidVersion; $model $buildNumber) " +
            "(#Build; $brand; $model; $buildNumber; $androidVersion) " +
            "+CoolMarket/$versionName-$versionCode-${Constants.MODE}"
}
