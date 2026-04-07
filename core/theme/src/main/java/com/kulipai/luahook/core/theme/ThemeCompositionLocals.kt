package com.kulipai.luahook.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.data.model.ThemeSettings

val LocalThemePreference = staticCompositionLocalOf { ThemeSettings() }

@Composable
@ReadOnlyComposable
fun currentThemePreference(): ThemeSettings = LocalThemePreference.current

@Composable
@ReadOnlyComposable
fun currentUiMode() = LocalThemePreference.current.uiMode

@Composable
@ReadOnlyComposable
fun currentColorMode(): ColorMode = LocalThemePreference.current.resolvedColorMode()

@Composable
@ReadOnlyComposable
fun currentEnableBlur(): Boolean = LocalThemePreference.current.enableBlur

@Composable
@ReadOnlyComposable
fun currentEnableFloatingBottomBar(): Boolean =
    LocalThemePreference.current.enableFloatingBottomBar

@Composable
@ReadOnlyComposable
fun currentEnableFloatingBottomBarBlur(): Boolean =
    LocalThemePreference.current.enableFloatingBottomBarBlur
