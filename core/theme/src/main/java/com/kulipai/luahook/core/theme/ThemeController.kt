/*
 * This file is based on code from KernelSU
 * (https://github.com/tiann/KernelSU)
 *
 * Licensed under GPL-3.0
 *
 * Modifications by KuLiPai
 */

package com.kulipai.luahook.core.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.kulipai.luahook.core.model.AppSettings
import com.kulipai.luahook.core.model.ColorMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec



object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uiMode = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        var colorModeValue = prefs.getInt("color_mode", ColorMode.SYSTEM.value)

        if (uiMode == "miuix") {
            val miuixMonet = prefs.getBoolean("miuix_monet", false)
            val colorMode = ColorMode.fromValue(colorModeValue)
            colorModeValue = if (!miuixMonet && colorMode.isMonet) {
                colorMode.toNonMonetMode()
            } else if (miuixMonet && !colorMode.isMonet) {
                colorMode.toMonetMode()
            } else {
                colorModeValue
            }
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = prefs.getInt("key_color", 0)
        val paletteStyleStr = prefs.getString("color_style", PaletteStyle.TonalSpot.name)
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr!!)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val colorSpecStr = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name)
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr!!)
        } catch (_: Exception) {
            ColorSpec.SpecVersion.Default
        }

        return AppSettings(colorMode, keyColor, paletteStyle, colorSpec)
    }
}


val LocalColorMode = staticCompositionLocalOf { 0 }

val LocalEnableBlur = staticCompositionLocalOf { true }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }
