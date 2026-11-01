package com.example.c001apk.compose.ui.settings
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.ParamsUtils
import com.example.c001apk.compose.util.TokenDeviceUtils.encode
import com.example.c001apk.compose.util.Utils.randomMacAddress
import com.example.c001apk.compose.util.makeToast

/**
 * Created by bggRGjQaUbCoE on 2024/6/1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {

    val rememberScrollState = rememberScrollState()
    val prefs = LocalUserPreferences.current
    val context = LocalContext.current

    // 进入参数页：自动拉取最新版本三字段并填充（模拟手动输入，连带更新 UA）
    LaunchedEffect(Unit) {
        viewModel.refreshVersionFromServer()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Ios26TopBar(
                rounded = true,
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = { Text(text = "参数") },
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState)
        ) {

            ParamsItem(
                title = "版本名称",
                data = prefs.versionName.ifEmpty { null }
            ) {
                viewModel.setVersionName(it)
                val ua = ParamsUtils.buildUserAgent(
                    androidVersion = prefs.androidVersion,
                    model = prefs.model,
                    buildNumber = prefs.buildNumber,
                    brand = prefs.brand,
                    versionName = it,
                    versionCode = prefs.versionCode,
                )
                viewModel.setUserAgent(ua)
                CookieUtil.userAgent = ua
            }

            ParamsItem(
                title = "API 版本",
                data = prefs.apiVersion.ifEmpty { null }
            ) {
                viewModel.setApiVersion(it)
                CookieUtil.apiVersion = it
            }

            ParamsItem(
                title = "版本代码",
                data = prefs.versionCode.ifEmpty { null }
            ) {
                viewModel.setVersionCode(it)
                val ua = ParamsUtils.buildUserAgent(
                    androidVersion = prefs.androidVersion,
                    model = prefs.model,
                    buildNumber = prefs.buildNumber,
                    brand = prefs.brand,
                    versionName = prefs.versionName,
                    versionCode = it,
                )
                viewModel.setUserAgent(ua)
                CookieUtil.userAgent = ua
                CookieUtil.versionCode = it
            }

            ParamsItem(
                title = "厂商",
                data = prefs.manufacturer.ifEmpty { null }
            ) {
                viewModel.setManufacturer(it)
                viewModel.setXAppDevice(
                    encode("${prefs.szlmId}; ; ; ${randomMacAddress()}; $it; ${prefs.brand}; ${prefs.model}; ${prefs.buildNumber}; null")
                )
            }

            ParamsItem(
                title = "品牌",
                data = prefs.brand.ifEmpty { null }
            ) {
                viewModel.setBrand(it)
                viewModel.setXAppDevice(
                    encode("${prefs.szlmId}; ; ; ${randomMacAddress()}; ${prefs.manufacturer}; $it; ${prefs.model}; ${prefs.buildNumber}; null")
                )
                val ua = ParamsUtils.buildUserAgent(
                    androidVersion = prefs.androidVersion,
                    model = prefs.model,
                    buildNumber = prefs.buildNumber,
                    brand = it,
                    versionName = prefs.versionName,
                    versionCode = prefs.versionCode,
                )
                viewModel.setUserAgent(ua)
                CookieUtil.userAgent = ua
            }

            ParamsItem(
                title = "型号",
                data = prefs.model.ifEmpty { null }
            ) {
                viewModel.setModel(it)
                viewModel.setXAppDevice(
                    encode("${prefs.szlmId}; ; ; ${randomMacAddress()}; ${prefs.manufacturer}; ${prefs.brand}; $it; ${prefs.buildNumber}; null")
                )
                val ua = ParamsUtils.buildUserAgent(
                    androidVersion = prefs.androidVersion,
                    model = it,
                    buildNumber = prefs.buildNumber,
                    brand = prefs.brand,
                    versionName = prefs.versionName,
                    versionCode = prefs.versionCode,
                )
                viewModel.setUserAgent(ua)
                CookieUtil.userAgent = ua
            }

            ParamsItem(
                title = "构建号",
                data = prefs.buildNumber.ifEmpty { null }
            ) {
                viewModel.setBuildNumber(it)
                viewModel.setXAppDevice(
                    encode("${prefs.szlmId}; ; ; ${randomMacAddress()}; ${prefs.manufacturer}; ${prefs.brand}; ${prefs.model}; $it; null")
                )
                val ua = ParamsUtils.buildUserAgent(
                    androidVersion = prefs.androidVersion,
                    model = prefs.model,
                    buildNumber = it,
                    brand = prefs.brand,
                    versionName = prefs.versionName,
                    versionCode = prefs.versionCode,
                )
                viewModel.setUserAgent(ua)
                CookieUtil.userAgent = ua
            }

            ParamsItem(
                title = "SDK 版本",
                data = prefs.sdkInt.ifEmpty { null }
            ) {
                viewModel.setSdkInt(it)
                CookieUtil.sdkInt = it
            }

            ParamsItem(
                title = "Android 版本",
                data = prefs.androidVersion.ifEmpty { null }
            ) {
                viewModel.setAndroidVersion(it)
                val ua = ParamsUtils.buildUserAgent(
                    androidVersion = it,
                    model = prefs.model,
                    buildNumber = prefs.buildNumber,
                    brand = prefs.brand,
                    versionName = prefs.versionName,
                    versionCode = prefs.versionCode,
                )
                viewModel.setUserAgent(ua)
                CookieUtil.userAgent = ua
            }

            ParamsItem(
                title = "User-Agent",
                data = prefs.userAgent.ifEmpty { null }
            )

            ParamsItem(
                title = "X-App-Device",
                data = prefs.xAppDevice.ifEmpty { null }
            )

            BasicListItem(
                headlineText = "重新生成参数",
                supportingText = "生成一套新的随机机型参数（版本三字段同步实时获取）"
            ) {
                viewModel.regenerateParams(random = true)
                // 重新生成时也实时获取版本名称 / API 版本 / 版本代码
                viewModel.refreshVersionFromServer()
                context.makeToast("已重新生成机型参数")
            }

            BasicListItem(
                headlineText = "恢复默认本机参数",
                supportingText = "用当前手机真实的机型信息覆盖随机参数"
            ) {
                viewModel.restoreDeviceParams()
                // 恢复本机参数时也同步实时版本三字段
                viewModel.refreshVersionFromServer()
                context.makeToast("已恢复本机参数")
            }

            BasicListItem(
                headlineText = "刷新数字联盟 ID",
                supportingText = "重新生成设备身份并同步 X-App-Device"
            ) {
                viewModel.refreshSzlmId { success ->
                    context.makeToast(if (success) "数字联盟 ID 已刷新" else "刷新失败，请稍后重试")
                }
            }
            // 给最后一个条目留出可点击距离，避免贴到手势/导航栏边缘。
            Spacer(modifier = Modifier.height(120.dp))

        }
    }

}

@Composable
fun ParamsItem(
    title: String,
    data: String?,
    setData: ((String) -> Unit)? = null
) {

    var showDialog by remember {
        mutableStateOf(false)
    }

    BasicListItem(
        headlineText = title,
        supportingText = data
    ) {
        setData?.let {
            showDialog = true
        }
    }

    when {
        showDialog -> {
            EditTextDialog(
                data = data.orEmpty(),
                title = title,
                onDismiss = {
                    showDialog = false
                },
                setData = {
                    if (setData != null) {
                        setData(it)
                    }
                }
            )
        }
    }

}
