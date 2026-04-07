package com.kulipai.luahook.core.data.model

data class UserSettings(
    val theme: ThemeSettings = ThemeSettings(),
    val app: AppSettings = AppSettings(),
)