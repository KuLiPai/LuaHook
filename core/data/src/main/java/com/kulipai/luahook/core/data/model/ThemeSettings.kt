package com.kulipai.luahook.core.data.model

import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.UiMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

data class ThemeSettings(
    val uiMode: UiMode = UiMode.Miuix,
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val useMiuixMonet: Boolean = DEFAULT_USE_MIUIX_MONET,
    val keyColor: Int = DEFAULT_KEY_COLOR,
    val paletteStyle: PaletteStyle = DEFAULT_PALETTE_STYLE,
    val colorSpec: ColorSpec.SpecVersion = DEFAULT_COLOR_SPEC,
    val enableBlur: Boolean = DEFAULT_ENABLE_BLUR,
    val enableFloatingBottomBar: Boolean = DEFAULT_ENABLE_FLOATING_BOTTOM_BAR,
    val enableFloatingBottomBarBlur: Boolean = DEFAULT_ENABLE_FLOATING_BOTTOM_BAR_BLUR,
    val pageScale: Float = DEFAULT_PAGE_SCALE,

) {
    companion object {
        const val DEFAULT_USE_MIUIX_MONET = false
        const val DEFAULT_KEY_COLOR = 0
        val DEFAULT_PALETTE_STYLE = PaletteStyle.TonalSpot
        val DEFAULT_COLOR_SPEC = ColorSpec.SpecVersion.Default
        const val DEFAULT_ENABLE_BLUR = true
        const val DEFAULT_ENABLE_FLOATING_BOTTOM_BAR = true
        const val DEFAULT_ENABLE_FLOATING_BOTTOM_BAR_BLUR = true
        const val DEFAULT_PAGE_SCALE = 1f

    }
}