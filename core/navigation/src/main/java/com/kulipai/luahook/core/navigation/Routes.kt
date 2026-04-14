/*
 * This file is based on code from KernelSU
 * (https://github.com/tiann/KernelSU)
 *
 * Licensed under GPL-3.0
 *
 * Modifications by KuLiPai
 */

package com.kulipai.luahook.core.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for Navigation3.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
sealed interface Route : NavKey, Parcelable {
    @Parcelize
    @Serializable
    data object Main : Route

    @Parcelize
    @Serializable
    data object Home : Route

    @Parcelize
    @Serializable
    data object Apps : Route

    @Parcelize
    @Serializable
    data object Module : Route

    @Parcelize
    @Serializable
    data object Settings : Route

    @Parcelize
    @Serializable
    data object About : Route

    @Parcelize
    @Serializable
    data class ScriptManager(val packageName: String) : Route

    @Parcelize
    @Serializable
    data class ScriptEditor(val packageName: String, val scriptId: String) : Route

    @Parcelize
    @Serializable
    data class ModuleEditor(val moduleId: String) : Route

    @Parcelize
    @Serializable
    data object Logcat : Route

    @Parcelize
    @Serializable
    data object ColorPalette : Route

    @Parcelize
    @Serializable
    data object AppProfileTemplate : Route


    @Parcelize
    @Serializable
    data class AppProfile(val uid: Int, val packageName: String) : Route

    @Parcelize
    @Serializable
    data object Install : Route


    @Parcelize
    @Serializable
    data object ModuleRepo : Route


    @Parcelize
    @Serializable
    data class ExecuteModuleAction(val moduleId: String) : Route
}
