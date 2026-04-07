package com.kulipai.luahook.core.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.kulipai.luahook.core.data.model.ThemeSettings
import com.kulipai.luahook.core.model.UiMode


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LuaHookTheme(
    themeSettings: ThemeSettings = LocalThemePreference.current,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalThemePreference provides themeSettings,
    ) {
        when (themeSettings.uiMode) {
            UiMode.Miuix -> MiuixLuaHookTheme(
                themePreference = themeSettings,
                content = content
            )

            UiMode.Material -> MaterialLuaHookTheme(
                themePreference = themeSettings,
                content = content
            )
        }
    }
}


@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (currentColorMode().value) {
        1, 4 -> false  // Force light mode
        2, 5, 6 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}
