package com.kulipai.luahook.core.theme

import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.ThemePreference
import com.kulipai.luahook.core.model.UiMode

internal fun ThemePreference.resolvedColorMode(): ColorMode {
    if (uiMode != UiMode.Miuix) {
        return colorMode
    }

    return when {
        !useMiuixMonet && colorMode.isMonet -> ColorMode.fromValue(colorMode.toNonMonetMode())
        useMiuixMonet && !colorMode.isMonet -> ColorMode.fromValue(colorMode.toMonetMode())
        else -> colorMode
    }
}
