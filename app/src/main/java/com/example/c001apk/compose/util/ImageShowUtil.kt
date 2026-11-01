package com.example.c001apk.compose.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.constant.Constants.SUFFIX_GIF
import com.example.c001apk.compose.constant.Constants.SUFFIX_THUMBNAIL
import com.example.c001apk.compose.util.NetWorkUtil
import com.example.c001apk.compose.view.CircleIndexIndicator
import com.example.c001apk.compose.view.NineGridImageView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.ext.mojito
import net.mikaelzero.mojito.impl.DefaultPercentProgress
import net.mikaelzero.mojito.impl.SimpleMojitoViewCallback
import java.io.File

object ImageShowUtil {

    fun startBigImgView(
        nineGridView: NineGridImageView,
        imageView: ImageView,
        urlList: List<String>,
        position: Int,
        cookie: String? = null,
        userAgent: String? = null,
    ) {
        val thumbnailList = urlList.map { it.http2https }
        val originList = urlList.map { it.replace(SUFFIX_THUMBNAIL, EMPTY_STRING).http2https }
        Mojito.start(imageView.context) {
            urls(thumbnailList, originList)
            position(position)
            progressLoader {
                DefaultPercentProgress()
            }
            if (urlList.size != 1)
                setIndicator(CircleIndexIndicator())
            views(nineGridView.getImageViews().toTypedArray())
            // 按图片画质设置决定是否自动加载原图：普清只显示缩略图，
            // 但长按保存仍然使用 originList 的高清地址。
            when (CookieUtil.imageQuality) {
                0 -> autoLoadTarget(NetWorkUtil.isWifiConnected())
                1 -> autoLoadTarget(true)
                else -> autoLoadTarget(false)
            }
            setOnMojitoListener(
                object : SimpleMojitoViewCallback() {
                    override fun onStartAnim(position: Int) {
                        nineGridView.getImageViewAt(position)?.apply {
                            postDelayed({
                                this.isVisible = false
                            }, 200)
                        }
                    }

                    override fun onMojitoViewFinish(pagePosition: Int) {
                        nineGridView.getImageViews().forEach {
                            it.isVisible = true
                        }
                    }

                    override fun onViewPageSelected(position: Int) {
                        nineGridView.getImageViews().forEachIndexed { index, imageView ->
                            imageView.isVisible = position != index
                        }
                    }

                    override fun onLongClick(
                        fragmentActivity: FragmentActivity?,
                        view: View,
                        x: Float,
                        y: Float,
                        position: Int
                    ) {
                        if (fragmentActivity != null) {
                            showSaveImgDialog(
                                fragmentActivity,
                                originList[position],
                                originList,
                                userAgent,
                            )
                        } else {
                            Log.i("Mojito", "fragmentActivity is null, skip save image")
                        }
                    }
                },
            )
        }

    }

    fun startBigImgViewSimple(
        context: Context,
        urlList: List<String>,
        cookie: String? = null,
        userAgent: String? = null,
    ) {
        val originList = urlList.map { it.replace(SUFFIX_THUMBNAIL, EMPTY_STRING).http2https }
        val thumbnailList = originList.map { "$it$SUFFIX_THUMBNAIL" }
        Mojito.start(context) {
            urls(thumbnailList, originList)
            // 0=网络自适应、1=原图、2=普清；普清时只加载缩略图。
            when (CookieUtil.imageQuality) {
                0 -> autoLoadTarget(NetWorkUtil.isWifiConnected())
                1 -> autoLoadTarget(true)
                else -> autoLoadTarget(false)
            }
            progressLoader {
                DefaultPercentProgress()
            }
            if (urlList.size > 1)
                setIndicator(CircleIndexIndicator())
            setOnMojitoListener(object : SimpleMojitoViewCallback() {
                override fun onLongClick(
                    fragmentActivity: FragmentActivity?,
                    view: View,
                    x: Float,
                    y: Float,
                    position: Int
                ) {
                    if (fragmentActivity != null) {
                        showSaveImgDialog(
                            fragmentActivity,
                            originList[position],
                            originList,
                            userAgent
                        )
                    } else {
                        Log.i("Mojito", "fragmentActivity is null, skip save image")
                    }
                }
            })
        }
    }

    fun startBigImgViewSimple(
        imageView: ImageView,
        url: String,
        cookie: String? = null,
        userAgent: String? = null,
    ) {
        val originUrl = url.replace(SUFFIX_THUMBNAIL, EMPTY_STRING).http2https
        val displayUrl = when {
            originUrl.endsWith(SUFFIX_GIF) -> originUrl
            CookieUtil.imageQuality == 1 -> originUrl
            CookieUtil.imageQuality == 0 && NetWorkUtil.isWifiConnected() -> originUrl
            else -> "$originUrl$SUFFIX_THUMBNAIL"
        }
        imageView.mojito(
            url = displayUrl,
            builder = {
                progressLoader {
                    DefaultPercentProgress()
                }
                setOnMojitoListener(object : SimpleMojitoViewCallback() {
                    override fun onLongClick(
                        fragmentActivity: FragmentActivity?,
                        view: View,
                        x: Float,
                        y: Float,
                        position: Int
                    ) {
                        if (fragmentActivity != null) {
                            showSaveImgDialog(fragmentActivity, originUrl, null, userAgent)
                        } else {
                            Log.i("Mojito", "fragmentActivity is null, skip save image")
                        }
                    }
                })
            },
        )
    }

    /**
     * iOS/原生 多选列表弹窗（首页栏目编辑等复用）。
     *
     * 用系统 AlertDialog（与「长按图片保存」同一套窗口实现），
     * 状态栏沉浸表现与此前已验证完美的大图长按弹窗一致。
     */
    fun showMultiCheckDialog(
        context: Context,
        title: String,
        items: List<String>,
        checked: BooleanArray,
        onConfirm: (BooleanArray) -> Unit,
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMultiChoiceItems(
                items.toTypedArray(),
                checked
            ) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onConfirm(checked)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showSaveImgDialog(
        context: Context,
        url: String,
        urlList: List<String>?,
        userAgent: String?,
    ) {
        val items = arrayOf("保存图片", "保存全部图片", "图片分享", "复制图片地址")
        AlertDialog.Builder(context).apply {
            setItems(items) { _: DialogInterface?, position: Int ->
                when (position) {
                    0 -> CoroutineScope(Dispatchers.IO).launch {
                        checkImageExist(context, url, true, userAgent)
                    }

                    1 -> CoroutineScope(Dispatchers.IO).launch {
                        if (urlList.isNullOrEmpty()) {
                            checkImageExist(context, url, true, userAgent)
                        } else {
                            urlList.forEachIndexed { index, url ->
                                checkImageExist(context, url, index == urlList.lastIndex, userAgent)
                            }
                        }
                    }

                    2 -> CoroutineScope(Dispatchers.IO).launch {
                        val index = url.lastIndexOf('/')
                        val filename = url.substring(index + 1)
                        if (checkShareImageExist(context, filename)) {
                            shareImage(
                                context,
                                File(context.externalCacheDir, "imageShare/$filename"),
                                null
                            )
                        } else {
                            ImageDownloadUtil.downloadImage(
                                context, url, filename,
                                isEnd = true,
                                isShare = true,
                                userAgent = userAgent,
                            )
                        }
                    }

                    3 -> context.copyText(url)
                }
            }
            show()
        }
    }

    private fun checkShareImageExist(context: Context, filename: String): Boolean {
        val imageCheckDir = File(context.externalCacheDir, "imageShare/$filename")
        return imageCheckDir.exists()
    }

    private suspend fun checkImageExist(
        context: Context,
        url: String,
        isEnd: Boolean,
        userAgent: String?,
    ) {
        val filename = url.substring(url.lastIndexOf('/') + 1)
        val path = "${context.getString(R.string.app_name)}/$filename"
        val imageFile = if (SDK_INT >= 29) File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), path
        )
        else File(Environment.getExternalStorageDirectory().toString(), path)
        if (imageFile.exists()) {
            if (isEnd)
                withContext(Dispatchers.Main) {
                    context.makeToast("文件已存在")
                }
        } else {
            ImageDownloadUtil.downloadImage(context, url, filename, isEnd, userAgent = userAgent)
        }
    }

    private fun getFileProvider(context: Context, file: File): Uri {
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun shareImage(context: Context, file: File, title: String?) {
        try {
            val contentUri = getFileProvider(context, file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ContextCompat.startActivity(context, Intent.createChooser(intent, title), null)
        } catch (e: ActivityNotFoundException) {
            context.makeToast("图片分享失败")
            e.printStackTrace()
        }
    }

    fun getImageLp(url: String): Pair<Int, Int> {
        var imgWidth = 1
        var imgHeight = 1
        val at = url.lastIndexOf("@")
        val x = url.lastIndexOf("x")
        val dot = url.lastIndexOf(".")
        if (at != -1 && x != -1 && dot != -1) {
            imgWidth = url.substring(at + 1, x).toInt()
            imgHeight = url.substring(x + 1, dot).toInt()
        }
        return Pair(imgWidth, imgHeight)
    }

}