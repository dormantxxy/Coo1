package com.example.c001apk.compose.ui.component

import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.c001apk.compose.constant.Constants.SUFFIX_GIF
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.http2https
import jp.wasabeef.transformers.coil.ColorFilterTransformation

/**
 * 通用网络图片（Coil）。
 *
 * [longPressSave] = true 时支持长按弹出保存/分享面板（与全屏预览长按同一套弹窗）。
 */
@Composable
fun CoilLoader(
    modifier: Modifier = Modifier,
    url: String?,
    colorFilter: Long? = null,
    longPressSave: Boolean = false,
    saveUrls: List<String>? = null,
) {
    val prefs = LocalUserPreferences.current
    val context = LocalContext.current
    var showActions by remember { mutableStateOf(false) }

    url?.let { raw ->
        val imageUrl = raw.http2https
        val composed = if (longPressSave) {
            modifier.pointerInput(raw) {
                detectTapGestures(onLongPress = { showActions = true })
            }
        } else modifier

        AsyncImage(
            model = ImageRequest.Builder(context)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey(imageUrl)
                .diskCacheKey(imageUrl)
                .data(imageUrl)
                .addHeader("User-Agent", prefs.userAgent)
                .apply {
                    if (raw.endsWith(SUFFIX_GIF)) {
                        decoderFactory(
                            if (Build.VERSION.SDK_INT >= 28) {
                                ImageDecoderDecoder.Factory()
                            } else {
                                GifDecoder.Factory()
                            }
                        )
                    }
                    colorFilter?.let {
                        transformations(ColorFilterTransformation(Color(colorFilter).toArgb()))
                    }
                    if (!raw.endsWith(SUFFIX_GIF) && colorFilter == null &&
                        prefs.isDarkMode() && CookieUtil.imageFilter
                    ) {
                        transformations(ColorFilterTransformation(Color(0x2D000000).toArgb()))
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = composed,
        )

        if (showActions) {
            Ios26ImageActionsDialog(
                url = imageUrl,
                allUrls = saveUrls ?: listOf(imageUrl),
                userAgent = prefs.userAgent,
                onDismiss = { showActions = false },
            )
        }
    }
}
