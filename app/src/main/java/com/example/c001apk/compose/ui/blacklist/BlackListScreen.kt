package com.example.c001apk.compose.ui.blacklist
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.model.StringEntity
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.Ios26CircleButton
import com.example.c001apk.compose.ui.component.Ios26SearchField
import com.example.c001apk.compose.ui.component.Ios26PillButton
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.noRippleClickable
import com.google.gson.Gson
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by bggRGjQaUbCoE on 2024/6/16
 */

enum class BlackListType {
    USER, TOPIC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackListScreen(
    onBackClick: () -> Unit,
    type: String,
    onViewUser: (String) -> Unit,
    onViewTopic: (String) -> Unit,
) {

    val viewModel =
        hiltViewModel<BlackListViewModel, BlackListViewModel.ViewModelFactory>(key = type) { factory ->
            factory.create(BlackListType.valueOf(type))
        }
    val blackList by viewModel.blackList.collectAsStateWithLifecycle(initialValue = emptyList())
    val userProfiles = viewModel.userProfiles

    val context = LocalContext.current
    val isUser = type == BlackListType.USER.name
    val pageTitle = if (isUser) "用户黑名单" else "话题黑名单"

    var textInput by remember { mutableStateOf(TextFieldValue(text = EMPTY_STRING)) }
    var showClearDialog by remember { mutableStateOf(false) }

    val query = textInput.text.trim()

    // 输入即筛选：UID / 用户名 / 话题名，大小写不敏感，支持任意位置模糊匹配。
    val filteredBlackList by remember(blackList, isUser) {
        derivedStateOf {
            val keyword = textInput.text.trim()
            if (keyword.isEmpty()) {
                blackList
            } else {
                blackList.filter { entity ->
                    entity.data.contains(keyword, ignoreCase = true) ||
                        (isUser && userProfiles[entity.data]?.username
                            ?.contains(keyword, ignoreCase = true) == true)
                }
            }
        }
    }
    val canAdd = if (isUser) {
        query.isNotEmpty() && query.all { it.isDigit() }
    } else {
        query.isNotEmpty()
    }

    fun addCurrent() {
        val value = textInput.text.trim()
        if (value.isEmpty()) return
        if (isUser && !value.all { it.isDigit() }) {
            context.makeToast("用户黑名单只能添加 UID")
            return
        }
        viewModel.save(value)
        textInput = TextFieldValue(EMPTY_STRING)
    }

    val backupSAFLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) backup@{ uri ->
            if (uri == null) return@backup
            try {
                context.contentResolver.openOutputStream(uri).use { output ->
                    if (output == null) {
                        context.makeToast("导出失败")
                    } else {
                        output.write(Gson().toJson(blackList.map { it.data }).toByteArray())
                        context.makeToast("导出成功")
                    }
                }
            } catch (e: Exception) {
                context.makeToast("导出失败")
                e.printStackTrace()
            }
        }

    fun onBackup() {
        if (blackList.isEmpty()) {
            context.makeToast("黑名单为空")
            return
        }
        try {
            val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.getDefault()).format(Date())
            backupSAFLauncher.launch("$pageTitle$date.json")
        } catch (e: Exception) {
            context.makeToast("导出失败")
            e.printStackTrace()
        }
    }

    val restoreSAFLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) restore@{ uri ->
            if (uri == null) return@restore
            try {
                val string = context.contentResolver
                    .openInputStream(uri)?.reader().use { it?.readText() }
                    ?: throw IOException("备份文件已损坏")
                val dataList: List<String> =
                    Gson().fromJson(string, Array<String>::class.java).toList()
                val currentList: List<String> = blackList.map { it.data }
                val newList: List<String> =
                    if (currentList.isEmpty()) dataList
                    else dataList.filter { it !in currentList }
                if (newList.isNotEmpty()) {
                    viewModel.insertList(newList.map { StringEntity(it) })
                }
                context.makeToast("导入成功")
            } catch (e: Exception) {
                context.makeToast("导入失败")
                e.printStackTrace()
            }
        }

    fun onRestore() {
        try {
            restoreSAFLauncher.launch("application/json")
        } catch (e: Exception) {
            context.makeToast("导入失败")
            e.printStackTrace()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Ios26TopBar(
                rounded = true,
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                // 搜索框并入顶栏，不显示标题（与本地搜索页风格一致）；回车可添加
                title = {
                    Ios26SearchField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = if (isUser) "搜索 UID 或用户名" else "搜索话题名称",
                        modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
                        onSearch = { addCurrent() },
                        imeAction = ImeAction.Done,
                    )
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(),
                )
        ) {
            // 搜索框已并入顶栏（回车添加），列表区域不再放置搜索/添加行

            if (filteredBlackList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (blackList.isEmpty()) "暂无内容" else "没有匹配的黑名单",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item(key = "header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (query.isEmpty()) {
                                    "共 ${blackList.size} 条"
                                } else {
                                    "找到 ${filteredBlackList.size} 条"
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Ios26PillButton(
                                text = "清除全部",
                                onClick = { showClearDialog = true },
                            )
                        }
                    }

                    items(filteredBlackList, key = { it.data }) { entity ->
                        if (isUser) {
                            UserBlackListItem(
                                uid = entity.data,
                                profile = userProfiles[entity.data],
                                onClick = { onViewUser(entity.data) },
                                onDelete = { viewModel.delete(entity.data) },
                            )
                        } else {
                            TopicBlackListItem(
                                topic = entity.data,
                                onClick = { onViewTopic(entity.data) },
                                onDelete = { viewModel.delete(entity.data) },
                            )
                        }
                    }
                }
            }
        }
    }

    when {
        showClearDialog -> {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAll()
                        }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearDialog = false }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
                title = {
                    Text(text = "确定清除全部黑名单？", modifier = Modifier.fillMaxWidth())
                }
            )
        }
    }

    viewModel.toastText?.let {
        viewModel.reset()
        context.makeToast(it)
    }
}

@Composable
private fun UserBlackListItem(
    uid: String,
    profile: UserProfile?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!profile?.avatar.isNullOrEmpty()) {
                CoilLoader(
                    url = profile?.avatar,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = profile?.username
                    ?.takeIf { it.isNotEmpty() }
                    ?: "用户 $uid",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "账号：$uid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Ios26CircleButton(
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = "移出黑名单",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
    }
}

@Composable
private fun TopicBlackListItem(
    topic: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Tag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = topic,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "已屏蔽话题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Ios26CircleButton(
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = "移出黑名单",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
    }
}
