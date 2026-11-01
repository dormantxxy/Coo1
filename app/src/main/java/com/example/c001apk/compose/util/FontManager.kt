package com.example.c001apk.compose.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * 全局自定义字体：选择本地 .ttf/.otf 字体文件后复制到应用私有目录，
 * 保存路径/名称到 SharedPreferences，供 Theme 全局应用。
 *
 * [currentPath] 是运行时状态（Compose state），选择/清除字体后更新它，
 * Theme 读取当前路径立即全局重组切换字体（无需重启）。
 */
object FontManager {

    private const val PREFS = "custom_font_pref"
    private const val KEY_PATH = "font_path"
    private const val KEY_NAME = "font_name"
    private const val DIR = "fonts"
    private const val PREFIX = "font_"
    private const val EXT = ".ttf"

    /** 运行时当前字体路径（null = 系统默认）。由 MainActivity 启动时同步、选择后更新。 */
    var currentPath: String? by mutableStateOf(null)

    /** 把选中的字体文件复制到私有目录并保存路径与名称。 */
    fun saveFont(context: Context, uri: Uri, name: String): Boolean {
        return try {
            val dir = File(context.filesDir, DIR)
            if (!dir.exists()) dir.mkdirs()
            // 每次保存用唯一文件名，避免覆盖正在被 Compose Font(File) 使用的字体文件导致二次应用闪退
            val dest = File(dir, PREFIX + System.currentTimeMillis() + EXT)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            getPrefs(context).edit()
                .putString(KEY_PATH, dest.absolutePath)
                .putString(KEY_NAME, name)
                .apply()
            // 同步运行时状态，Compose 主题自动重组立即生效（无需 recreate / 重启）
            currentPath = dest.absolutePath
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 持久化的字体文件绝对路径（未设置返回 null）。 */
    fun getFontPath(context: Context): String? =
        getPrefs(context).getString(KEY_PATH, null)?.takeIf { File(it).exists() }

    /** 当前自定义字体名称（未设置返回 null）。 */
    fun getFontName(context: Context): String? =
        getPrefs(context).getString(KEY_NAME, null)

    /** 从字体路径加载 Typeface（Android TextView / 富文本用），未设置或失败返回 null。 */
    fun createTypeface(path: String?): Typeface? =
        path?.takeIf { File(it).exists() }?.let {
            runCatching { Typeface.createFromFile(it) }.getOrNull()
        }

    /** 清除自定义字体（恢复系统默认）。 */
    fun clearFont(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PATH)
            .remove(KEY_NAME)
            .apply()
        currentPath = null
        runCatching { File(context.filesDir, DIR).deleteRecursively() }
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}