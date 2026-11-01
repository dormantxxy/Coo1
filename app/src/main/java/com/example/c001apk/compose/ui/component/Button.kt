package com.example.c001apk.compose.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.util.composeClick

/**
 * iOS 26 风格返回按钮：按压缩放 + 按压高亮，替代原来的 IconButton 水波纹。
 */
@Composable
fun BackButton(
    onBackClick: () -> Unit
) {
    Ios26IconAction(
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        contentDescription = "返回",
        onClick = composeClick { onBackClick() },
        tint = MaterialTheme.colorScheme.onSurface,
        size = 36.dp,
        glass = false,
    )
}
