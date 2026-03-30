package com.kulipai.luahook.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.kulipai.luahook.core.model.AppSettings


@Composable
fun LuaHookTheme(
    appSettings: AppSettings? = null,
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)

    when (uiMode) {
        UiMode.Miuix -> MiuixKernelSUTheme(
            appSettings = currentAppSettings,
            content = content
        )

        UiMode.Material -> MaterialKernelSUTheme(
            appSettings = currentAppSettings,
            content = content
        )
    }
}


@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (LocalColorMode.current) {
        1, 4 -> false  // Force light mode
        2, 5, 6 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}
