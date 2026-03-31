package com.kulipai.luahook.core.data.model

import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.ThemePreference
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.model.UserSettings
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

data class UserPreferences(
    val uiMode: UiMode = UiMode.Miuix,
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val useMiuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1f,
)

internal fun UserPreferences.asExternalModel(): UserSettings {
    return UserSettings(
        themePreference = ThemePreference(
            uiMode = uiMode,
            colorMode = colorMode,
            useMiuixMonet = useMiuixMonet,
            keyColor = keyColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            enableBlur = enableBlur,
            enableFloatingBottomBar = enableFloatingBottomBar,
            enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
        ),
        pageScale = pageScale,
    )
}
