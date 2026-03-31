package com.kulipai.luahook.core.data.repository

import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.model.UserSettings
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userSettings: Flow<UserSettings>

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
}
