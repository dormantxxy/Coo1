package com.example.c001apk.compose.ui.home

import android.content.Context

/**
 * 首页栏目显示偏好（长按 tab 编辑）。
 *
 * 只记录「隐藏」的栏目，不删除任何栏目 —— 这样即使把某个栏目隐藏了，
 * 下次编辑时它仍然出现在编辑面板里，可以再次勾选恢复。
 */
object HomeTabPrefs {
    private const val PREF = "home_tabs"
    private const val KEY_HIDDEN = "hidden_tabs"

    fun loadHidden(context: Context): Set<TabType> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_HIDDEN, null)
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { runCatching { TabType.valueOf(it) }.getOrNull() }
            .toSet()
    }

    fun saveHidden(context: Context, hidden: Set<TabType>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_HIDDEN, hidden.joinToString(",") { it.name })
            .apply()
    }
}
