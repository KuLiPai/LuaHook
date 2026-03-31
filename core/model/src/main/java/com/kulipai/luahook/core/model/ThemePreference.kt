package com.kulipai.luahook.core.model

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

data class ThemePreference(
    val uiMode: UiMode = UiMode.Miuix,
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val useMiuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.Default,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
)
