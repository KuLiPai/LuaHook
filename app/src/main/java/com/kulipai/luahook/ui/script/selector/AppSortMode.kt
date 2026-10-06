package com.kulipai.luahook.ui.script.selector

import android.content.Context
import android.content.SharedPreferences
import com.kulipai.luahook.data.model.AppInfo

enum class AppSortMode(val value: Int) {
    NAME(0),
    INSTALL_TIME(1),
    UPDATE_TIME(2),
    PACKAGE_NAME(3);

    companion object {
        fun fromValue(value: Int): AppSortMode {
            return entries.find { it.value == value } ?: NAME
        }
    }
}

object SelectorPrefs {
    private const val PREFS_NAME = "selector_settings"
    const val PREF_SHOW_SYSTEM_APPS = "show_system_apps"
    const val PREF_SORT_MODE = "sort_mode"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isShowSystemApps(context: Context): Boolean {
        return getPrefs(context).getBoolean(PREF_SHOW_SYSTEM_APPS, false)
    }

    fun setShowSystemApps(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(PREF_SHOW_SYSTEM_APPS, show).apply()
    }

    fun getSortMode(context: Context): AppSortMode {
        val value = getPrefs(context).getInt(PREF_SORT_MODE, AppSortMode.NAME.value)
        return AppSortMode.fromValue(value)
    }

    fun setSortMode(context: Context, mode: AppSortMode) {
        getPrefs(context).edit().putInt(PREF_SORT_MODE, mode.value).apply()
    }
}

fun List<AppInfo>.sortApps(mode: AppSortMode): List<AppInfo> {
    return when (mode) {
        AppSortMode.NAME -> sortedWith(
            compareBy({ it.appName.lowercase() }, { it.packageName.lowercase() })
        )
        AppSortMode.INSTALL_TIME -> sortedWith(
            compareByDescending<AppInfo> { it.firstInstallTime }
                .thenBy { it.appName.lowercase() }
        )
        AppSortMode.UPDATE_TIME -> sortedWith(
            compareByDescending<AppInfo> { it.lastUpdateTime }
                .thenBy { it.appName.lowercase() }
        )
        AppSortMode.PACKAGE_NAME -> sortedWith(
            compareBy({ it.packageName.lowercase() }, { it.appName.lowercase() })
        )
    }
}
