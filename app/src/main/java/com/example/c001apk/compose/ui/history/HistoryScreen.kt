package com.example.c001apk.compose.ui.history
import com.example.c001apk.compose.ui.component.Ios26TopBar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.Ios26SearchField
import com.example.c001apk.compose.ui.component.cards.HistoryCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.noRippleClickable

/**
 * Created by bggRGjQaUbCoE on 2024/6/17
 */

enum class HistoryType {
    FAV, HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    type: String,
    onViewUser: (String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onCopyText: (String?) -> Unit,
) {

    val viewModel =
        hiltViewModel<HistoryViewModel, HistoryViewModel.ViewModelFactory>(key = type) { factory ->
            factory.create(HistoryType.valueOf(type))
        }
    val dataList by viewModel.dataList.collectAsStateWithLifecycle(initialValue = emptyList())
    val folders by viewModel.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val isFav = type == HistoryType.FAV.name

    val layoutDirection = LocalLayoutDirection.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var deleteFolderName by remember { mutableStateOf<String?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var moveItemId by remember { mutableStateOf<String?>(null) }
    var searchInput by remember { mutableStateOf(TextFieldValue("")) }
    val pageTitle = when (type) {
        HistoryType.FAV.name -> "本地收藏"
        HistoryType.HISTORY.name -> "浏览历史"
        else -> type
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Ios26TopBar(
                rounded = true,
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                // 搜索框并入顶栏，顶栏不再显示标题文字（本地收藏 / 浏览历史都支持）
                title = {
                    Ios26SearchField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            viewModel.setQuery(it.text)
                        },
                        placeholder = if (isFav) "搜索收藏的帖子" else "搜索浏览过的帖子",
                        modifier = Modifier.fillMaxWidth(),
                        onSearch = { viewModel.setQuery(searchInput.text) },
                        imeAction = ImeAction.Search,
                    )
                },
                actions = {
                    AnimatedVisibility(
                        visible = dataList.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = {
                            showClearDialog = true
                        }) {
                            Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null)
                        }
                    }

                }
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {

            // 本地收藏已简化为单一默认分组：不再显示收藏夹筛选条与新建按钮

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = paddingValues.calculateLeftPadding(layoutDirection)),
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        top = 6.dp,
                        end = 10.dp,
                        bottom = 10.dp + paddingValues.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(dataList, key = { it.id }) {
                        HistoryCard(
                            data = it,
                            onViewUser = onViewUser,
                            onReport = onReport,
                            onDelete = { id ->
                                viewModel.delete(id)
                            },
                            onBlockUser = { uid ->
                                viewModel.blockUser(uid)
                            },
                            onOpenLink = onOpenLink,
                            onViewFeed = onViewFeed,
                            onCopyText = onCopyText,
                            onMoveFolder = if (isFav) {
                                { id -> moveItemId = id }
                            } else null,
                        )
                    }

                }

                if (dataList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingCard(
                            state = LoadingState.Empty,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            title = { Text("新建收藏夹", modifier = Modifier.fillMaxWidth()) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    placeholder = { Text("收藏夹名称") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createFolder(newFolderName)
                        showCreateFolder = false
                    }
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolder = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    moveItemId?.let { id ->
        AlertDialog(
            onDismissRequest = { moveItemId = null },
            title = { Text("移动到收藏夹", modifier = Modifier.fillMaxWidth()) },
            text = {
                Column {
                    FolderRow("未分类") {
                        viewModel.moveToFolder(id, "")
                        moveItemId = null
                    }
                    folders.forEach { folder ->
                        FolderRow(folder.name) {
                            viewModel.moveToFolder(id, folder.name)
                            moveItemId = null
                        }
                    }
                    if (folders.isEmpty()) {
                        Text(
                            text = "还没有收藏夹，可先点上方「＋ 新建」",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveItemId = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    // 长按收藏夹 → 删除确认（沿用字体缩放弹窗那套原生 AlertDialog 样式）
    deleteFolderName?.let { folderName ->
        AlertDialog(
            onDismissRequest = { deleteFolderName = null },
            title = {
                Text(
                    text = "删除收藏夹",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            },
            text = {
                Text(
                    text = "确定删除「$folderName」？\n收藏夹内的帖子会移到「未分类」，不会被删除。",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folderName)
                        if (selectedFolder == folderName) viewModel.setFolder("")
                        deleteFolderName = null
                    }
                ) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderName = null }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
        )
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
                        onClick = {
                            showClearDialog = false
                        }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
                title = {
                    Text(text = "确定清除全部？", modifier = Modifier.fillMaxWidth())
                }
            )
        }
    }

}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                0.6.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape,
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.noRippleClickable { onClick() }
                }
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderRow(name: String, onClick: () -> Unit) {
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}
