package com.example.c001apk.compose.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.util.makeToast

/**
 * 「收藏到收藏单」选择弹窗：列出官方收藏单，点击加入；可新建收藏单并加入。
 * @param targetId 帖子 id
 */
@Composable
fun CollectionPickDialog(
    targetId: String,
    onDismiss: () -> Unit,
    onCollected: () -> Unit = {},
) {
    val viewModel = hiltViewModel<CollectionPickViewModel>()
    val context = LocalContext.current
    var createMode by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCollections()
    }

    LaunchedEffect(viewModel.toastText) {
        viewModel.toastText?.let {
            viewModel.resetToast()
            context.makeToast(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (createMode) "新建收藏单" else "收藏到收藏单",
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            if (createMode) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("收藏单名称") },
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (viewModel.loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    if (!viewModel.loading && viewModel.collections.isEmpty()) {
                        Text(
                            text = "还没有官方收藏单，点下方「新建收藏单」创建一个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    viewModel.collections.forEach { collection ->
                        CollectionPickRow(collection.title.orEmpty()) {
                            viewModel.addToCollection(
                                collectionId = collection.id.orEmpty(),
                                targetId = targetId,
                            ) { ok, msg ->
                                context.makeToast(msg)
                                if (ok) onCollected()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (createMode) {
                TextButton(
                    enabled = newTitle.isNotBlank(),
                    onClick = {
                        viewModel.createAndAdd(newTitle, targetId) { ok, msg ->
                            context.makeToast(msg)
                            if (ok) onCollected()
                        }
                        createMode = false
                        newTitle = ""
                    }
                ) {
                    Text("新建并收藏")
                }
            } else {
                TextButton(onClick = {
                    createMode = true
                    newTitle = ""
                }) {
                    Text("新建收藏单")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun CollectionPickRow(name: String, onClick: () -> Unit) {
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}