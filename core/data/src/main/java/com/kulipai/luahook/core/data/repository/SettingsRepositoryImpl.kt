package com.kulipai.luahook.core.data.repository

import com.kulipai.luahook.core.data.datastore.UserSettingsDataSource
import com.kulipai.luahook.core.data.model.UserSettings
import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.UiMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class SettingsRepositoryImpl(
    private val userSettingsDataSource: UserSettingsDataSource,
) : SettingsRepository {

    override val userSettings: Flow<UserSettings> = userSettingsDataSource.userSettings
        .distinctUntilChanged()

    override suspend fun setUiMode(uiMode: UiMode) {
        userSettingsDataSource.setUiMode(uiMode)
    }

    override suspend fun setColorMode(colorMode: ColorMode) {
        userSettingsDataSource.setColorMode(colorMode)
    }

    override suspend fun setUseMiuixMonet(useMiuixMonet: Boolean) {
        userSettingsDataSource.setUseMiuixMonet(useMiuixMonet)
    }

    override suspend fun setKeyColor(keyColor: Int) {
        userSettingsDataSource.setKeyColor(keyColor)
    }

    override suspend fun setPaletteStyle(paletteStyle: PaletteStyle) {
        userSettingsDataSource.setPaletteStyle(paletteStyle)
    }

    override suspend fun setColorSpec(colorSpec: ColorSpec.SpecVersion) {
        userSettingsDataSource.setColorSpec(colorSpec)
    }

    override suspend fun setEnableBlur(enableBlur: Boolean) {
        userSettingsDataSource.setEnableBlur(enableBlur)
    }

    override suspend fun setEnableFloatingBottomBar(enableFloatingBottomBar: Boolean) {
        userSettingsDataSource.setEnableFloatingBottomBar(enableFloatingBottomBar)
    }

    override suspend fun setEnableFloatingBottomBarBlur(enableFloatingBottomBarBlur: Boolean) {
        userSettingsDataSource.setEnableFloatingBottomBarBlur(enableFloatingBottomBarBlur)
    }

    override suspend fun setPageScale(pageScale: Float) {
        userSettingsDataSource.setPageScale(pageScale)
    }

    override suspend fun setLanguage(language: String) {
        userSettingsDataSource.setLanguage(language)
    }
}
