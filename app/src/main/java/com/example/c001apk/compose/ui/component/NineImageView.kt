package com.example.c001apk.compose.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.compose.constant.Constants.SUFFIX_GIF
import com.example.c001apk.compose.constant.Constants.SUFFIX_THUMBNAIL
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.util.ImageShowUtil.getImageLp
import com.example.c001apk.compose.view.NineGridImageView

/**
 * 九宫格图片。
 *
 * 长按任意一张 → 弹出与全屏预览同款的保存/分享面板。
 */
@Composable
fun NineImageView(
    modifier: Modifier = Modifier,
    pic: String?,
    picArr: List<String>?,
    feedType: String?,
    isSingle: Boolean = false
) {
    val prefs = LocalUserPreferences.current

    val primaryContainer = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()

    var showActionsUrl by remember { mutableStateOf<String?>(null) }
    var showActionsAll by remember { mutableStateOf<List<String>>(emptyList()) }

    AndroidView(
        modifier = modifier,
        factory = {
            NineGridImageView(it).apply {
                this.colorPrimaryContainer = primaryContainer
                this.colorOnPrimaryContainer = onPrimaryContainer
                this.isSingle = isSingle
                this.userAgent = prefs.userAgent
                this.onLongPressImage = { position, urlList ->
                    val origin = urlList.map { u -> u.replace(SUFFIX_THUMBNAIL, "") }
                    showActionsUrl = origin.getOrNull(position) ?: origin.firstOrNull()
                    showActionsAll = origin
                }
            }
        },
        update = { imageView ->
            if (!picArr.isNullOrEmpty()) {
                if (picArr.size == 1 || feedType in listOf("feedArticle", "trade")) {
                    val imageLp = getImageLp(pic ?: picArr[0])
                    imageView.imgWidth = imageLp.first
                    imageView.imgHeight = imageLp.second
                }
                imageView.apply {
                    val urlList: MutableList<String> = ArrayList()
                    // GIF 直接使用原图地址（原图能正常播放动图），其它图片仍用 .s.jpg 缩略图
                    fun String.picUrl(): String =
                        if (endsWith(SUFFIX_GIF, ignoreCase = true)) this
                        else "$this$SUFFIX_THUMBNAIL"

                    if (feedType in listOf("feedArticle", "trade") && imgWidth > imgHeight)
                        if (!pic.isNullOrEmpty()) urlList.add(pic.picUrl())
                        else urlList.add(picArr[0].picUrl())
                    else
                        urlList.addAll(picArr.map { it.picUrl() })
                    setUrlList(urlList)
                }
            }
        }
    )

    showActionsUrl?.let { url ->
        Ios26ImageActionsDialog(
            url = url,
            allUrls = showActionsAll.ifEmpty { listOf(url) },
            userAgent = prefs.userAgent,
            onDismiss = { showActionsUrl = null },
        )
    }
}
