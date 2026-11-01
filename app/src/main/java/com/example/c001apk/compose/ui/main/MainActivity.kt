package com.example.c001apk.compose.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.settings.SettingsViewModel
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.example.c001apk.compose.util.CookieUtil.apiVersion
import com.example.c001apk.compose.util.CookieUtil.imageFilter
import com.example.c001apk.compose.util.CookieUtil.imageQuality
import com.example.c001apk.compose.util.CookieUtil.isDarkMode
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.CookieUtil.materialYou
import com.example.c001apk.compose.util.CookieUtil.openInBrowser
import com.example.c001apk.compose.util.CookieUtil.recordHistory
import com.example.c001apk.compose.util.CookieUtil.sdkInt
import com.example.c001apk.compose.util.CookieUtil.showEmoji
import com.example.c001apk.compose.util.CookieUtil.showSquare
import com.example.c001apk.compose.util.CookieUtil.szlmId
import com.example.c001apk.compose.util.CookieUtil.token
import com.example.c001apk.compose.util.CookieUtil.uid
import com.example.c001apk.compose.util.CookieUtil.userAgent
import com.example.c001apk.compose.util.CookieUtil.username
import com.example.c001apk.compose.util.CookieUtil.versionCode
import com.example.c001apk.compose.util.CookieUtil.versionName
import com.example.c001apk.compose.util.CookieUtil.xAppDevice
import com.example.c001apk.compose.util.FontManager
import com.example.c001apk.compose.util.SzlmIdFetcher
import com.example.c001apk.compose.util.TokenDeviceUtils.encode
import com.example.c001apk.compose.util.Utils.randomMacAddress
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/5/29
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject
    lateinit var networkRepo: NetworkRepo
    private lateinit var navController: NavHostController
    private val viewModel by viewModels<MainViewModel>()
    // 直接复用参数页面的版本拉取方法（refreshVersionFromServer）
    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onResume() {
        super.onResume()
        // 兜底：部分机型从输入页面返回时输入法不会自动收起，
        // 回到本页统一隐藏一次，避免键盘挂在内容上方。
        runCatching {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.let {
            navController.onOpenLink(
                context = this,
                url = it.toString(),
                title = null,
                needConvert = true
            )
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 恢复本地自定义字体（全局应用）
        FontManager.currentPath = FontManager.getFontPath(this)

        setContent {
            navController = rememberNavController()

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(
                    initialValue = null,
                    lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                )

            // 冷启动优化：DataStore 首帧通常还没读完，先画一层主题背景，等数据到达再渲染。
            val preferences = userPreferences
            if (preferences == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {}
                return@setContent
            }

            // 设备参数（机型 + DUID）是否真正就绪：四字段齐全才允许渲染主页/发请求。
            // 首次安装时这些都是异步生成的，未就绪就拉首页会得到 response body is null。
            val deviceReady = preferences.manufacturer.isNotBlank() &&
                preferences.model.isNotBlank() &&
                preferences.xAppDevice.isNotBlank() &&
                preferences.szlmId.isNotBlank()

            // 首屏 bootstrap：严格串行，DUID → 生成含 DUID 的机型参数 → 完成。
            // 避免 regenerateParams 与 DUID 拉取并发竞态，导致 xAppDevice 首段（DUID）为空。
            LaunchedEffect(Unit) {
                if (deviceReady) return@LaunchedEffect
                val autoPrefs = getSharedPreferences("szlm_auto", MODE_PRIVATE)
                // 1) 拉 DUID（首次安装 / szlmId 为空 / 账号变化）
                val firstFetch = !autoPrefs.getBoolean("first_fetch_done", false)
                val uidChanged = preferences.uid.isNotEmpty() &&
                    autoPrefs.getString("last_uid", "") != preferences.uid
                var duid = preferences.szlmId
                if (firstFetch || preferences.szlmId.isEmpty() || uidChanged) {
                    val fetched = withContext(Dispatchers.IO) {
                        SzlmIdFetcher.fetch(this@MainActivity)
                    }
                    if (!fetched.isNullOrEmpty()) {
                        duid = fetched
                        com.example.c001apk.compose.util.CookieUtil.szlmId = fetched
                        userPreferencesRepository.setSZLMId(fetched)
                        autoPrefs.edit().putBoolean("first_fetch_done", true).apply()
                        if (preferences.uid.isNotEmpty()) {
                            autoPrefs.edit().putString("last_uid", preferences.uid).apply()
                        }
                    }
                }
                // 2) 生成/重建机型参数（此时 CookieUtil.szlmId 已是最新 DUID，xAppDevice 首段正确）
                val latest = userPreferencesRepository.data.first()
                if (latest.manufacturer.isBlank() || latest.model.isBlank() ||
                    latest.androidVersion.isBlank() || latest.xAppDevice.isBlank()
                ) {
                    viewModel.regenerateParamsAndWait(random = false)
                } else if (duid.isNotEmpty()) {
                    // 参数已有，但确保 xAppDevice 用当前 DUID 重建（防止旧版首段为空）
                    userPreferencesRepository.setXAppDevice(
                        encode("$duid; ; ; ${randomMacAddress()}; ${latest.manufacturer}; ${latest.brand}; ${latest.model}; ${latest.buildNumber}; null")
                    )
                }
            }

            // 图片画质默认“网络自适应”；若用户已在设置里显式选择过，则保留其选择。
            LaunchedEffect(Unit) {
                val autoPrefs = getSharedPreferences("szlm_auto", MODE_PRIVATE)
                if (!autoPrefs.getBoolean("image_quality_default_v3", false)) {
                    if (!autoPrefs.getBoolean("image_quality_user_set", false)) {
                        userPreferencesRepository.setImageQuality(0)
                    }
                    autoPrefs.edit().putBoolean("image_quality_default_v3", true).apply()
                }
            }

            // 版本三字段 + UA：设备就绪后、30 天一次，复用参数页面的 refreshVersionFromServer()。
            LaunchedEffect(deviceReady) {
                if (!deviceReady) return@LaunchedEffect
                val autoPrefs = getSharedPreferences("szlm_auto", MODE_PRIVATE)
                val lastFetch = autoPrefs.getLong("last_version_fetch", 0L)
                val interval = 30L * 24 * 60 * 60 * 1000
                if (lastFetch > 0L && System.currentTimeMillis() - lastFetch < interval) {
                    return@LaunchedEffect
                }
                settingsViewModel.refreshVersionFromServer {
                    autoPrefs.edit()
                        .putLong("last_version_fetch", System.currentTimeMillis())
                        .apply()
                }
            }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences
            ) {
                isLogin = preferences.isLogin
                szlmId = preferences.szlmId
                xAppDevice = preferences.xAppDevice
                uid = preferences.uid
                username = preferences.username
                token = preferences.token
                userAgent = preferences.userAgent
                sdkInt = preferences.sdkInt
                // 请求头版本三字段：优先用已拉取的实时值，未拉取时回退常量默认
                versionName = preferences.versionName.ifBlank {
                    com.example.c001apk.compose.constant.Constants.VERSION_NAME
                }
                versionCode = preferences.versionCode.ifBlank {
                    com.example.c001apk.compose.constant.Constants.VERSION_CODE
                }
                apiVersion = preferences.apiVersion.ifBlank {
                    com.example.c001apk.compose.constant.Constants.API_VERSION
                }
                imageQuality = preferences.imageQuality
                showEmoji = preferences.showEmoji
                showSquare = preferences.showSquare
                openInBrowser = preferences.openInBrowser
                imageFilter = preferences.imageFilter
                recordHistory = preferences.recordHistory
                materialYou = preferences.materialYou
                isDarkMode = preferences.isDarkMode()

                C001apkComposeTheme(
                    darkTheme = preferences.isDarkMode(),
                    themeType = preferences.themeType,
                    seedColor = preferences.seedColor,
                    materialYou = preferences.materialYou,
                    pureBlack = preferences.pureBlack,
                    paletteStyle = preferences.paletteStyle,
                    fontScale = preferences.fontScale,
                    contentScale = preferences.contentScale,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // 设备参数未就绪时不渲染主页（避免 HomeFeedViewModel 过早拉首页）。
                        // 非首次安装时已固化，立即可渲染（不影响秒开）。
                        if (deviceReady) {
                            MainNavigation(
                                navController = navController,
                                badge = viewModel.badge,
                                resetBadge = viewModel::resetBadge,
                                widthSizeClass = widthSizeClass,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }
            }

            LaunchedEffect(key1 = navController) {
                if (savedInstanceState == null) {
                    handleIntent(intent)
                }
            }

        }

    }
}
