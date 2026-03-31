package com.kulipai.luahook.core.model

data class UserSettings(
    val themePreference: ThemePreference = ThemePreference(),
    val pageScale: Float = 1f,
)
