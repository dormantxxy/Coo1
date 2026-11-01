package com.example.c001apk.compose.ui.component

import android.graphics.Typeface
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.method.LinkMovementMethodCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.util.FontManager
import com.example.c001apk.compose.util.ImageShowUtil
import com.example.c001apk.compose.view.LinkTextView

/**
 * Created by bggRGjQaUbCoE on 2024/6/2
 */
@Composable
fun LinkText(
    modifier: Modifier = Modifier,
    text: String?,
    textSize: Float = 15f,
    isBold: Boolean = false,
    lineSpacingMultiplier: Float = 1.0f,
    maxLines: Int? = null,
    ellipsize: TextUtils.TruncateAt = TextUtils.TruncateAt.END,
    color: Int? = null,
    onOpenLink: ((String, String?) -> Unit)? = null,
    isReply: Boolean = false,
    onShowTotalReply: (() -> Unit)? = null,
    imgList: List<String>? = null,
) {
    val contentColor = LocalContentColor.current
    val userPreference = LocalUserPreferences.current

    val primary = MaterialTheme.colorScheme.primary.toArgb()
    // 帖子正文是 Android TextView，单独应用本地自定义字体
    val customTypeface = FontManager.currentPath?.let { FontManager.createTypeface(it) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LinkTextView(context).apply {
                setParams(
                    isReply = isReply,
                    size = textSize,
                    fontScale = userPreference.fontScale,
                )
                when {
                    customTypeface != null && isBold ->
                        setTypeface(Typeface.create(customTypeface, Typeface.BOLD))
                    customTypeface != null -> setTypeface(customTypeface)
                    isBold -> setTypeface(typeface, Typeface.BOLD)
                }
                setLineSpacing(0.0f, lineSpacingMultiplier)
                maxLines?.let {
                    this.maxLines = it
                    this.ellipsize = ellipsize
                }
                setTextColor(contentColor.toArgb())
                color?.let {
                    setTextColor(it)
                }
            }
        },
        update = { textView ->
            customTypeface?.let { tf ->
                textView.typeface =
                    if (isBold) Typeface.create(tf, Typeface.BOLD) else tf
            }
            textView.setSpText(
                text = text.orEmpty(),
                color = primary,
                onOpenLink = { url, title ->
                    onOpenLink?.let { it(url, title) }
                },
                onShowTotalReply = onShowTotalReply,
                onShowImages = { url ->
                    ImageShowUtil.startBigImgViewSimple(
                        textView.context,
                        imgList ?: listOf(url),
                        userAgent = userPreference.userAgent,
                    )
                }
            )
        }
    )
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val contentColor = LocalContentColor.current.toArgb()
    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val customTypeface = FontManager.currentPath?.let { FontManager.createTypeface(it) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethodCompat.getInstance()
                setTextColor(contentColor)
                setLinkTextColor(primary)
                customTypeface?.let { typeface = it }
                text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            }
        }
    )
}