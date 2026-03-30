package com.kulipai.luahook.core.model

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
)
