package com.kulipai.luahook.feature.main.screen.setting.model

import com.kulipai.luahook.core.data.model.ThemeSettings

data class SettingsUiState(
    val themePreference: ThemeSettings = ThemeSettings(),
    val pageScale: Float = 1f,
    val checkUpdate: Boolean = true,
    val checkModuleUpdate: Boolean = true,
    val enablePredictiveBack: Boolean = false,
    val enableWebDebugging: Boolean = false,
    val suCompatStatus: String = "",
    val suCompatMode: Int = 0,
    val isSuEnabled: Boolean = false,
    val kernelUmountStatus: String = "",
    val isKernelUmountEnabled: Boolean = false,
    val isDefaultUmountModules: Boolean = false,
    val isLkmMode: Boolean = false,
    val autoJailbreak: Boolean = false,
)
