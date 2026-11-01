package com.example.c001apk.compose.ui.settings

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ImageAspectRatio
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.FollowType
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ThemeMode
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.blacklist.BlackListType
import com.example.c001apk.compose.ui.component.HtmlText
import com.example.c001apk.compose.ui.component.icons.Swatch
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.ui.component.settings.DropdownListItem
import com.example.c001apk.compose.ui.component.settings.SelectionItem
import com.example.c001apk.compose.ui.component.settings.StateDropdownListItem
import com.example.c001apk.compose.ui.component.settings.SwitchListItem
import com.example.c001apk.compose.util.CacheDataManager.clearAllCache
import com.example.c001apk.compose.util.CacheDataManager.getTotalCacheSize
import com.example.c001apk.compose.util.FontManager
import com.example.c001apk.compose.util.TokenDeviceUtils.encode
import com.example.c001apk.compose.util.Utils.randomMacAddress
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.openInBrowser
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.materialkolor.PaletteStyle
import java.util.Formatter

/**
 * Created by bggRGjQaUbCoE on 2024/5/30
 */
private val ThemeType.cnName: String
    get() = when (this) {
        ThemeType.Default -> "默认"
        ThemeType.Red -> "红色"
        ThemeType.Pink -> "粉色"
        ThemeType.Purple -> "紫色"
        ThemeType.Indigo -> "靛蓝"
        ThemeType.Blue -> "蓝色"
        ThemeType.LightBlue -> "浅蓝"
        ThemeType.Cyan -> "青色"
        ThemeType.Teal -> "蓝绿"
        ThemeType.Green -> "绿色"
        ThemeType.LightGreen -> "浅绿"
        ThemeType.Lime -> "青柠"
        ThemeType.Yellow -> "黄色"
        ThemeType.Amber -> "琥珀"
        ThemeType.Orange -> "橙色"
        ThemeType.DeepOrange -> "深橙"
        ThemeType.Brown -> "棕色"
        ThemeType.BlueGrey -> "蓝灰"
        ThemeType.Sakura -> "樱花"
        ThemeType.Custom -> "自定义"
        ThemeType.UNRECOGNIZED -> "未知"
    }

private val ThemeMode.cnName: String
    get() = when (this) {
        ThemeMode.FOLLOW_SYSTEM -> "跟随系统"
        ThemeMode.ALWAYS_ON -> "总是开启"
        ThemeMode.ALWAYS_OFF -> "总是关闭"
        ThemeMode.UNRECOGNIZED -> "未知"
    }

private val FollowType.cnName: String
    get() = when (this) {
        FollowType.ALL -> "全部"
        FollowType.USER -> "好友"
        FollowType.TOPIC -> "话题"
        FollowType.PRODUCT -> "数码"
        FollowType.APP -> "应用"
        FollowType.UNRECOGNIZED -> "未知"
    }

private fun paletteStyleCn(name: String): String = when (name.lowercase()) {
    "tonalspot" -> "柔和"
    "neutral" -> "中性"
    "vibrant" -> "鲜艳"
    "expressive" -> "表现"
    "rainbow" -> "彩虹"
    "fruitsalad" -> "缤纷"
    "monochrome" -> "单色"
    "fidelity" -> "保真"
    "content" -> "内容"
    "neutralvariant" -> "中性变体"
    else -> name
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onParamsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onViewBlackList: (String) -> Unit,
) {

    val prefs = LocalUserPreferences.current
    val rememberScrollState = rememberScrollState()
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current

    // 本地字体选择（全局应用）
    var fontName by remember { mutableStateOf(FontManager.getFontName(context)) }
    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = queryFontDisplayName(context, uri) ?: "本地字体"
            if (FontManager.saveFont(context, uri, name)) {
                fontName = name
                context.makeToast("字体已应用：$name")
            } else {
                context.makeToast("字体加载失败")
            }
        }
    }

    val imageQualityList by lazy { listOf("网络自适应", "原图", "普清") }
    val themeList by lazy { listOf("跟随系统", "总是开启", "总是关闭") }
    val followList by lazy { listOf("全部", "好友", "话题", "数码", "应用") }

    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSZLMIDDialog by remember { mutableStateOf(false) }
    var showFontScaleDialog by remember { mutableStateOf(false) }
    var showContentScaleDialog by remember { mutableStateOf(false) }
    var showImageQualityDialog by remember { mutableStateOf(false) }
    var showCleanCacheDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(getTotalCacheSize(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Ios26TopBar(
                rounded = true,
                title = {
                    Text(
                        text = "设置",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                actions = {
                    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { dropdownMenuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = Icons.Default.MoreHoriz.name
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownMenuExpanded,
                            onDismissRequest = { dropdownMenuExpanded = false }
                        ) {
                            listOf("反馈", "关于").forEachIndexed { index, menu ->
                                DropdownMenuItem(
                                    text = { Text(menu) },
                                    onClick = {
                                        dropdownMenuExpanded = false
                                        when (index) {
                                            0 -> context.openInBrowser("$URL_SOURCE_CODE/issues")

                                            1 -> showAboutDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                    end = paddingValues.calculateRightPadding(layoutDirection)
                )
                .verticalScroll(rememberScrollState)
        ) {

            BasicListItem(leadingText = "显示")
            /* 主题白/夜切换：跟随系统 / 深色 / 浅色 */
            DropdownListItem(
                leadingImageVector = Icons.Outlined.DarkMode,
                headlineText = "主题模式",
                value = prefs.themeMode.cnName,
                selections = themeList.mapIndexed { index, label ->
                    SelectionItem(label, ThemeMode.entries.getOrElse(index) { ThemeMode.UNRECOGNIZED }.name)
                },
                onValueChanged = { index, _ ->
                    viewModel.setDarkTheme(
                        ThemeMode.entries.getOrElse(index) { ThemeMode.UNRECOGNIZED }
                    )
                }
            )
            BasicListItem(
                leadingImageVector = Icons.Outlined.Block,
                headlineText = "用户黑名单",
            ) {
                onViewBlackList(BlackListType.USER.name)
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.Block,
                headlineText = "话题黑名单",
            ) {
                onViewBlackList(BlackListType.TOPIC.name)
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.TextFields,
                headlineText = "字体大小",
                supportingText = "${Formatter().format("%.2f", prefs.fontScale)}x"
            ) {
                showFontScaleDialog = true
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.ImageAspectRatio,
                headlineText = "内容大小",
                supportingText = "${Formatter().format("%.2f", prefs.contentScale)}x"
            ) {
                showContentScaleDialog = true
            }
            DropdownListItem(
                leadingImageVector = Icons.Outlined.AddCircleOutline,
                headlineText = "关注分组",
                value = prefs.followType.name,
                selections = followList.mapIndexed { index, label ->
                    SelectionItem(label, FollowType.entries[index].name)
                },
                onValueChanged = { index, _ ->
                    viewModel.setFollowType(FollowType.entries.getOrElse(index) { FollowType.UNRECOGNIZED })
                }
            )
            DropdownListItem(
                leadingImageVector = Icons.Outlined.Image,
                headlineText = "图片画质",
                value = prefs.imageQuality,
                selections = imageQualityList.mapIndexed { index, label ->
                    SelectionItem(label, index)
                },
                onValueChanged = { index, _ ->
                    viewModel.setImageQuality(index)
                    // 记录用户显式选择了画质，后续升级不再覆盖
                    context.getSharedPreferences("szlm_auto", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("image_quality_user_set", true).apply()
                }
            )
            /*BasicListItem(
                leadingImageVector = Icons.Outlined.Image,
                headlineText = "图片画质",
                supportingText = imageQualityList[userPreferences.imageQuality]
            ) {
                showImageQualityDialog = true
            }*/
            SwitchListItem(
                value = prefs.imageFilter,
                leadingImageVector = Icons.Outlined.PhotoLibrary,
                headlineText = "压暗图片",
            ) {
                viewModel.setImageFilter(it)
            }
            SwitchListItem(
                value = prefs.openInBrowser,
                leadingImageVector = Icons.Outlined.TravelExplore,
                headlineText = "使用外部浏览器打开链接",
            ) {
                viewModel.setOpenInBrowser(it)
            }
            SwitchListItem(
                value = prefs.showSquare,
                leadingImageVector = Icons.AutoMirrored.Outlined.Feed,
                headlineText = "头条显示广场",
            ) {
                viewModel.setShowSquare(it)
            }
            SwitchListItem(
                value = prefs.recordHistory,
                leadingImageVector = Icons.Outlined.History,
                headlineText = "记录浏览历史",
            ) {
                viewModel.setRecordHistory(it)
            }
            SwitchListItem(
                value = prefs.showEmoji,
                leadingImageVector = Icons.Outlined.EmojiEmotions,
                headlineText = "显示表情",
            ) {
                viewModel.setShowEmoji(it)
            }
            /*SwitchListItem(
                value = prefs.checkCount,
                leadingImageVector = Icons.Outlined.Notifications,
                headlineText = "检查通知",
            ) {
                viewModel.setCheckCount(it)
            }*/

            BasicListItem(leadingText = "其他")
            BasicListItem(
                leadingImageVector = Icons.Outlined.Smartphone,
                headlineText = "机型参数",
                supportingText = "默认从系统获取，可手动修改"
            ) {
                onParamsClick()
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.AllInclusive,
                headlineText = "关于",
                supportingText = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
            ) {
                onAboutClick()
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.CleaningServices,
                headlineText = "清理缓存",
                supportingText = cacheSize
            ) {
                cacheSize = getTotalCacheSize(context)
                showCleanCacheDialog = true
            }

            // 列表可以滑到底栏下方；留出距离保证最后一个项目仍能点击。
            Spacer(modifier = Modifier.height(190.dp))
        }

        when {
            showAboutDialog -> {
                AboutDialog {
                    showAboutDialog = false
                }
            }

            showImageQualityDialog -> {
                ListItemDialog(
                    title = "图片画质",
                    list = imageQualityList,
                    selected = prefs.imageQuality,
                    onDismiss = {
                        showImageQualityDialog = false
                    },
                    setData = {
                        showImageQualityDialog = false
                        viewModel.setImageQuality(it)
                    }
                )
            }

            showThemeTypeDialog -> {
                EditTextDialog(
                    hint = "6650A4",
                    maxLength = 6,
                    data = prefs.seedColor,
                    title = "自定义主题色",
                    onDismiss = {
                        showThemeTypeDialog = false
                    },
                    setData = {
                        viewModel.setSeedColor(it)
                    }
                )
            }

            showSZLMIDDialog -> {
                EditTextDialog(
                    data = prefs.szlmId,
                    title = "SZLM ID",
                    onDismiss = {
                        showSZLMIDDialog = false
                    },
                    setData = {
                        viewModel.setSZLMId(it)
                        viewModel.setXAppDevice(
                            encode("$it; ; ; ${randomMacAddress()}; ${prefs.manufacturer}; ${prefs.brand}; ${prefs.model}; ${prefs.buildNumber}; null")
                        )
                    }
                )
            }

            showFontScaleDialog -> {
                FontScaleDialog(
                    data = prefs.fontScale,
                    title = "字体缩放",
                    hint = "字体大小",
                    fontName = fontName,
                    onDismiss = {
                        showFontScaleDialog = false
                    },
                    setData = {
                        viewModel.setFontScale(it)
                    },
                    onPickFont = {
                        fontPicker.launch(
                            arrayOf(
                                "font/ttf",
                                "font/otf",
                                "application/x-font-ttf",
                                "application/vnd.ms-opentype",
                                "application/octet-stream"
                            )
                        )
                    },
                    onResetFont = {
                        FontManager.clearFont(context)
                        fontName = null
                        context.makeToast("已恢复系统字体")
                    }
                )
            }

            showContentScaleDialog -> {
                SliderDialog(
                    data = prefs.contentScale,
                    title = "内容缩放",
                    hint = "内容大小",
                    onDismiss = {
                        showContentScaleDialog = false
                    },
                    setData = {
                        viewModel.setContentScale(it)
                    }
                )
            }

            showCleanCacheDialog -> {
                CleanCacheDialog(
                    cacheSize = cacheSize,
                    onDismiss = {
                        showCleanCacheDialog = false
                    },
                    onClean = {
                        clearAllCache(context)
                        cacheSize = "0 B"
                    }
                )
            }

        }
    }

}

@Composable
fun CleanCacheDialog(
    cacheSize: String,
    onDismiss: () -> Unit,
    onClean: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onClean()
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        text = {
            Text(text = "当前缓存大小: $cacheSize")
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "清理缓存",
                textAlign = TextAlign.Center
            )
        }
    )
}

@Composable
fun ListItemDialog(
    title: String,
    list: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    setData: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {},
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            LazyColumn {
                itemsIndexed(list) { index, title ->
                    ListItemDialogItem(title, selected == index) {
                        setData(index)
                    }
                }
            }
        },
    )
}

@Composable
fun ListItemDialogItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() }
    ) {
        RadioButton(selected = selected, onClick = { onClick() })
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically),
            text = title,
        )
    }
}

@Composable
fun EditTextDialog(
    data: String,
    title: String,
    onDismiss: () -> Unit,
    setData: (String) -> Unit,
    hint: String? = null,
    maxLength: Int? = null,
) {
    var text by remember {
        mutableStateOf(
            TextFieldValue(
                text = data,
                selection = TextRange(data.length)
            )
        )
    }
    val focusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequest.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                }) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(text.text)
                }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequest),
                value = text,
                onValueChange = {
                    if (maxLength != null) {
                        if (it.text.matches(Regex("^[0-9a-fA-F]{0,6}$")))
                            text = it
                    } else {
                        text = it
                    }
                },
                placeholder = {
                    hint?.let {
                        Text(text = it)
                    }
                }
            )
        },
    )
}

@Composable
fun FontScaleDialog(
    data: Float,
    title: String,
    hint: String,
    fontName: String?,
    onDismiss: () -> Unit,
    setData: (Float) -> Unit,
    onPickFont: () -> Unit,
    onResetFont: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(data) }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(1.0f)
                }
            ) { Text("重置") }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(progress)
                }
            ) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0.8f..1.3f
                )
                Text(
                    text = "$hint: ${Formatter().format("%.2f", progress)}",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp * progress,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "当前字体：${fontName ?: "系统默认"}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onPickFont,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("选择本地字体") }
                if (fontName != null) {
                    TextButton(
                        onClick = onResetFont,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("恢复默认字体") }
                }
            }
        }
    )
}

/** 从文件选择返回的 Uri 读取显示名（文件名）。 */
private fun queryFontDisplayName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

@Composable
fun SliderDialog(
    data: Float,
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    setData: (Float) -> Unit
) {
    var progress by remember { mutableFloatStateOf(data) }
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(1.0f)
                }) {
                Text(text = "重置")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(progress)
                }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                    },
                    valueRange = 0.8f..1.3f
                )
                Text(
                    text = "$hint: ${Formatter().format("%.2f", progress)}",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp * progress,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = { onDismiss() }) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors()
                .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                val drawable = ResourcesCompat.getDrawable(
                    context.resources,
                    R.mipmap.ic_launcher,
                    context.theme
                )
                Image(
                    painter = rememberDrawablePainter(drawable),
                    contentDescription = "icon",
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {

                    Text(
                        stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HtmlText(
                        html = stringResource(
                            id = R.string.about_source_code,
                            "<b><a href=\"${URL_SOURCE_CODE}\">GitHub</a></b>"
                        )
                    )
                }
            }
        }
    }
}