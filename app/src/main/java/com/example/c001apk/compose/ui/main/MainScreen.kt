package com.example.c001apk.compose.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.c001apk.compose.ui.home.HomeScreen
import com.example.c001apk.compose.ui.message.MessageScreen
import com.example.c001apk.compose.ui.settings.SettingsScreen
import com.example.c001apk.compose.util.ReportType
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Created by bggRGjQaUbCoE on 2024/5/30
 */
@Composable
fun MainScreen(
    selectIndex: Int,
    setSelectIndex: (Int) -> Unit,
    badge: Int,
    resetBadge: () -> Unit,
    onParamsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onSearch: () -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onViewApp: (String) -> Unit,
    onLogin: () -> Unit,
    onViewFFFList: (String?, String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewNotice: (String) -> Unit,
    onViewBlackList: (String) -> Unit,
    onViewHistory: (String) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
) {

    val screens = remember {
        listOf(
            Router.HOME,
            Router.MESSAGE,
            Router.SETTINGS
        )
    }

    val savableStateHolder = rememberSaveableStateHolder()
    var refreshState by remember { mutableStateOf(false) }

    // 页面内容的液态玻璃 backdrop：底栏 drawBackdrop 引用它，从而模糊背后内容
    val pageBackdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop),
        ) {
            savableStateHolder.SaveableStateProvider(
                key = selectIndex,
                content = {
                    when (selectIndex) {
                        0 -> HomeScreen(
                            refreshState = refreshState,
                            onRefresh = {
                                refreshState = true
                            },
                            resetRefreshState = {
                                refreshState = false
                            },
                            onViewUser = onViewUser,
                            onViewFeed = onViewFeed,
                            onSearch = onSearch,
                            onOpenLink = onOpenLink,
                            onCopyText = onCopyText,
                            onViewApp = onViewApp,
                            onReport = onReport,
                        )

                        1 -> MessageScreen(
                            onLogin = onLogin,
                            onViewUser = onViewUser,
                            onViewFeed = onViewFeed,
                            onOpenLink = onOpenLink,
                            onCopyText = onCopyText,
                            onViewFFFList = onViewFFFList,
                            onReport = onReport,
                            onViewNotice = onViewNotice,
                            onViewHistory = onViewHistory,
                        )

                        2 -> SettingsScreen(
                            onParamsClick = onParamsClick,
                            onAboutClick = onAboutClick,
                            onViewBlackList = onViewBlackList,
                        )
                    }
                }
            )
        }

        // 底栏：官方同源作者（Kyant0/QWEA0）的 Compose 液态玻璃库（com.kyant.backdrop）
        // LiquidBottomTabs 效果：液态玻璃胶囊、背景实时模糊、半透明、拖拽阻尼、选中高亮。
        // 适配 Android 12~17（RenderEffect API31+ 硬件模糊）。
        LiquidGlassBottomBarCompose(
            screens = screens,
            selectedIndex = selectIndex,
            badge = badge,
            backdrop = pageBackdrop,
            onSelected = { index ->
                if (selectIndex == 0 && index == 0) {
                    refreshState = true
                } else if (index == 1 && badge != 0) {
                    resetBadge()
                }
                setSelectIndex(index)
            },
        )
    }

    BackHandler(enabled = selectIndex != 0) {
        setSelectIndex(0)
    }

}