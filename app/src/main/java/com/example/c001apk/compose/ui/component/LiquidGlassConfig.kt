package com.example.c001apk.compose.ui.component

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * 全局液态玻璃（Liquid Glass）效果配置。
 *
 * 三种模式：
 * - 性能模式（PERFORMANCE）：底栏 / 顶栏 / 列表弹窗全部模糊全开
 * - 省电模式（BATTERY）：只保留底栏模糊，顶栏 / 列表弹窗回到纯色背景（周边无线条）
 * - 自定义模式（CUSTOM）：底栏 / 顶栏 / 列表分别独立调节强度（支持单项关闭）
 */
enum class LiquidGlassMode(val label: String) {
    PERFORMANCE("性能模式"),
    BATTERY("省电模式"),
    CUSTOM("自定义")
}

/** 三项区域各自的模糊强度（0f = 关闭，1f = 满）。 */
data class LiquidGlassLevels(
    val bottomBar: Float = 1f,
    val topBar: Float = 1f,
    val menus: Float = 1f,
)

/**
 * 全局液态玻璃运行时状态（跨组件同步，mutableStateOf 驱动重组）。
 */
object LiquidGlassState {

    var mode by mutableStateOf(LiquidGlassMode.PERFORMANCE)

    var levels by mutableStateOf(LiquidGlassLevels())

    /** 当前是否应启用底栏模糊。 */
    val bottomBarEnabled: Boolean
        get() = levels.bottomBar > 0f

    /** 当前是否应启用顶栏模糊（省电模式关闭）。 */
    val topBarEnabled: Boolean
        get() = when (mode) {
            LiquidGlassMode.BATTERY -> false
            else -> levels.topBar > 0f
        }

    /** 当前是否应启用列表弹窗 / 菜单模糊（省电模式关闭）。 */
    val menusEnabled: Boolean
        get() = when (mode) {
            LiquidGlassMode.BATTERY -> false
            else -> levels.menus > 0f
        }

    /** 省电模式下仅底栏有模糊。 */
    val isBatteryMode: Boolean
        get() = mode == LiquidGlassMode.BATTERY
}

/** 持久化（SharedPreferences 简单存取）。 */
object LiquidGlassPrefs {
    private const val PREFS = "liquid_glass"
    private const val KEY_MODE = "mode"
    private const val KEY_BOTTOM = "bottom"
    private const val KEY_TOP = "top"
    private const val KEY_MENUS = "menus"

    fun load(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        LiquidGlassState.mode =
            LiquidGlassMode.entries.getOrElse(sp.getInt(KEY_MODE, 0)) { LiquidGlassMode.PERFORMANCE }
        LiquidGlassState.levels = LiquidGlassLevels(
            bottomBar = sp.getFloat(KEY_BOTTOM, 1f),
            topBar = sp.getFloat(KEY_TOP, 1f),
            menus = sp.getFloat(KEY_MENUS, 1f),
        )
    }

    fun save(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit()
            .putInt(KEY_MODE, LiquidGlassState.mode.ordinal)
            .putFloat(KEY_BOTTOM, LiquidGlassState.levels.bottomBar)
            .putFloat(KEY_TOP, LiquidGlassState.levels.topBar)
            .putFloat(KEY_MENUS, LiquidGlassState.levels.menus)
            .apply()
    }

    fun setMode(context: Context, mode: LiquidGlassMode) {
        LiquidGlassState.mode = mode
        save(context)
    }

    fun setLevel(context: Context, levels: LiquidGlassLevels) {
        LiquidGlassState.levels = levels
        save(context)
    }
}