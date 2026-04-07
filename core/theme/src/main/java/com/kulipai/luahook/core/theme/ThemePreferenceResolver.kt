package com.kulipai.luahook.core.theme

import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.data.model.ThemeSettings
import com.kulipai.luahook.core.model.UiMode

internal fun ThemeSettings.resolvedColorMode(): ColorMode {
    if (uiMode != UiMode.Miuix) {
        return colorMode
    }

    return when {
        !useMiuixMonet && colorMode.isMonet -> ColorMode.fromValue(colorMode.toNonMonetMode())
        useMiuixMonet && !colorMode.isMonet -> ColorMode.fromValue(colorMode.toMonetMode())
        else -> colorMode
    }
}
