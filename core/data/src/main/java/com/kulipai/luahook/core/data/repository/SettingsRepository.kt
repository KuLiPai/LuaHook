package com.kulipai.luahook.core.data.repository

import com.kulipai.luahook.core.data.model.UserSettings
import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.UiMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow
import org.intellij.lang.annotations.Language

interface SettingsRepository {
    val userSettings: Flow<UserSettings>


    // theme设置
    suspend fun setUiMode(uiMode: UiMode)
    suspend fun setColorMode(colorMode: ColorMode)
    suspend fun setUseMiuixMonet(useMiuixMonet: Boolean)
    suspend fun setKeyColor(keyColor: Int)
    suspend fun setPaletteStyle(paletteStyle: PaletteStyle)
    suspend fun setColorSpec(colorSpec: ColorSpec.SpecVersion)
    suspend fun setEnableBlur(enableBlur: Boolean)
    suspend fun setEnableFloatingBottomBar(enableFloatingBottomBar: Boolean)
    suspend fun setEnableFloatingBottomBarBlur(enableFloatingBottomBarBlur: Boolean)
    suspend fun setPageScale(pageScale: Float)

    // app设置
    suspend fun setLanguage(language: String)
}
