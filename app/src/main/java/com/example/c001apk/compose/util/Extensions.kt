package com.example.c001apk.compose.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.constant.Constants.PREFIX_APP
import com.example.c001apk.compose.constant.Constants.PREFIX_FEED
import com.example.c001apk.compose.constant.Constants.PREFIX_TOPIC
import com.example.c001apk.compose.constant.Constants.PREFIX_USER
import com.example.c001apk.compose.constant.Constants.UTF8
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern

inline val Number.dp get() = (toFloat() * Resources.getSystem().displayMetrics.density).toInt()

inline val Number.sp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

inline val String.http2https: String
    get() = if (this.getOrElse(4) { 's' } == 's') this
    else StringBuilder(this).insert(4, 's').toString()

private var toastView: View? = null
private val toastHandler = Handler(Looper.getMainLooper())

/**
 * App 内建 iOS 26 风格提示条。
 *
 * 不使用系统 Toast：直接挂到当前 Activity 的 decorView 上，玻璃胶囊 + 轻阴影 +
 * 淡入上浮动画，颜色跟随明暗主题，失败类文案自动使用红色感叹号。
 */
fun Context.makeToast(text: String) {
    val activity = findActivity() ?: return
    activity.runOnUiThread {
        if (activity.isFinishing) return@runOnUiThread
        val decor = activity.window.decorView as? ViewGroup ?: return@runOnUiThread
        val density = activity.resources.displayMetrics.density
        val dp: (Int) -> Int = { (it * density).toInt() }
        val isDark = (activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isError = text.contains("失败") || text.contains("错误") ||
            text.contains("不存在") || text.contains("为空") ||
            text.contains("Failed", ignoreCase = true) || text.contains("error", ignoreCase = true)

        toastHandler.removeCallbacksAndMessages(null)
        toastView?.let { old -> (old.parent as? ViewGroup)?.removeView(old) }

        val accent = if (isError) 0xFFFF453A.toInt() else 0xFF0A84FF.toInt()
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(22).toFloat()
                setColor(if (isDark) 0xF22C2C2E.toInt() else 0xF2FFFFFF.toInt())
                setStroke(dp(1), if (isDark) 0x33FFFFFF else 0x1A000000)
            }
            elevation = dp(8).toFloat()
            alpha = 0f
            translationY = -dp(10).toFloat()
            isClickable = false
            isFocusable = false
        }

        val badge = TextView(activity).apply {
            setText(if (isError) "!" else "✓")
            setTextColor(accent)
            textSize = 12f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isError) 0x1FFF453A else 0x1F0A84FF)
            }
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                marginEnd = dp(8)
            }
        }

        val message = TextView(activity).apply {
            setText(text)
            setTextColor(if (isDark) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 3
            maxWidth = dp(300)
            includeFontPadding = false
        }

        container.addView(badge)
        container.addView(message)

        val statusBarId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarId > 0)
            activity.resources.getDimensionPixelSize(statusBarId) else 0
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = statusBarHeight + dp(12)
        }
        decor.addView(container, params)
        toastView = container

        container.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()

        toastHandler.postDelayed({
            container.animate()
                .alpha(0f)
                .translationY(-dp(8).toFloat())
                .setDuration(180)
                .withEndAction {
                    (container.parent as? ViewGroup)?.removeView(container)
                    if (toastView === container) toastView = null
                }
                .start()
        }, 2200)
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Modifier.noRippleToggleable(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
): Modifier = composed {
    toggleable(
        value = value,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onValueChange = onValueChange
    )
}

inline fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    crossinline onClick: () -> Unit
): Modifier = composed {
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = remember { MutableInteractionSource() }) {
        onClick()
    }
}

fun Context.copyText(text: String?, showToast: Boolean = true) {
    text?.let {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        ClipData.newPlainText("copy text", it)?.let { clipboardManager.setPrimaryClip(it) }
        if (showToast)
            makeToast("已复制到剪贴板")
    }
}

fun Context.shareText(text: String) {
    val title = "分享"
    try {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(Intent.createChooser(intent, title))
    } catch (e: Exception) {
        makeToast(e.message ?: "分享失败")
    }
}

enum class ShareType {
    FEED, APP, TOPIC, USER
}

fun getShareText(type: ShareType, id: String): String {
    val prefix = when (type) {
        ShareType.APP -> PREFIX_APP
        ShareType.FEED -> PREFIX_FEED
        ShareType.TOPIC -> PREFIX_TOPIC
        ShareType.USER -> PREFIX_USER
    }
    return "https://www.coolapk1s.com$prefix$id"
}

inline val String.getAllLinkAndText: String
    get() = if (isEmpty()) EMPTY_STRING else
        Pattern.compile("<a class=\"feed-link-url\"\\s+href=\"([^<>\"]*)\"[^<]*[^>]*>")
            .matcher(this).replaceAll(" $1 ")

// onDoubleClick = {} //双击时回调
// onPress = {} //按下时回调
// onLongPress = {} //长按时回调
// onTap = {} //轻触时回调(按下并抬起)
fun Modifier.doubleClick(onDoubleClick: (Offset) -> Unit): Modifier =
    pointerInput(this) {
        detectTapGestures(
            onDoubleTap = onDoubleClick
        )
    }

fun Modifier.longClick(onLongClick: (Offset) -> Unit): Modifier =
    pointerInput(this) {
        detectTapGestures(
            onLongPress = onLongClick
        )
    }

@Composable
inline fun composeClick(
    time: Int = 500,
    crossinline onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(value = 0L) }
    return {
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - lastClickTime >= time) {
            onClick()
            lastClickTime = currentTimeMillis
        }
    }
}

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

inline val String?.encode: String
    get() = URLEncoder.encode(this?.replace("%", "%25")?.replace("+", "%2B"), UTF8)
inline val String.decode: String
    get() = URLDecoder.decode(this, UTF8)

fun Context.openInBrowser(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        makeToast("打开失败")
        copyText(url)
        e.printStackTrace()
    }
}

@Suppress("DEPRECATION")
inline val PackageInfo.longVersionCodeCompat: Long
    get() {
        return if (SDK_INT >= 28)
            longVersionCode
        else
            versionCode.toLong()
    }

fun Document.createRequestHash(): String =
    this.getElementsByTag("Body").attr("data-request-hash")

fun createRandomNumber() = Math.random().toString().replace(".", "undefined")

fun Context.downloadApk(downloadUrl: String, name: String) {
    try {
        Utils.downloadApk(this, downloadUrl, name)
        copyText(downloadUrl)
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            openInBrowser(downloadUrl)
        } catch (e: Exception) {
            makeToast("下载失败")
            copyText(downloadUrl)
        }
    }
}

enum class ReportType {
    FEED, REPLY, USER
}

fun getReportUrl(id: String, type: ReportType): String =
    when (type) {
        ReportType.FEED -> "https://m.coolapk.com/mp/do?c=feed&m=report&type=feed&id=$id"
        ReportType.REPLY -> "https://m.coolapk.com/mp/do?c=feed&m=report&type=feed_reply&id=$id"
        ReportType.USER -> "https://m.coolapk.com/mp/do?c=user&m=report&id=$id"
    }
