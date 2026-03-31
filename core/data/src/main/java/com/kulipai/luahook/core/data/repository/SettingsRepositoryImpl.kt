package com.kulipai.luahook.core.data.repository

import com.kulipai.luahook.core.data.datastore.UserPreferencesDataSource
import com.kulipai.luahook.core.data.model.asExternalModel
import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.core.model.UserSettings
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val userPreferencesDataSource: UserPreferencesDataSource,
) : SettingsRepository {

    override val userSettings: Flow<UserSettings> = userPreferencesDataSource.userPreferences
        .map { preferences -> preferences.asExternalModel() }
        .distinctUntilChanged()

    override suspend fun setUiMode(uiMode: UiMode) {
        userPreferencesDataSource.setUiMode(uiMode)
    }

    override suspend fun setColorMode(colorMode: ColorMode) {
        userPreferencesDataSource.setColorMode(colorMode)
    }

    override suspend fun setUseMiuixMonet(useMiuixMonet: Boolean) {
        userPreferencesDataSource.setUseMiuixMonet(useMiuixMonet)
    }

    override suspend fun setKeyColor(keyColor: Int) {
        userPreferencesDataSource.setKeyColor(keyColor)
    }

    override suspend fun setPaletteStyle(paletteStyle: PaletteStyle) {
        userPreferencesDataSource.setPaletteStyle(paletteStyle)
    }

    override suspend fun setColorSpec(colorSpec: ColorSpec.SpecVersion) {
        userPreferencesDataSource.setColorSpec(colorSpec)
    }

    override suspend fun setEnableBlur(enableBlur: Boolean) {
        userPreferencesDataSource.setEnableBlur(enableBlur)
    }

    override suspend fun setEnableFloatingBottomBar(enableFloatingBottomBar: Boolean) {
        userPreferencesDataSource.setEnableFloatingBottomBar(enableFloatingBottomBar)
    }

    override suspend fun setEnableFloatingBottomBarBlur(enableFloatingBottomBarBlur: Boolean) {
        userPreferencesDataSource.setEnableFloatingBottomBarBlur(enableFloatingBottomBarBlur)
    }

    override suspend fun setPageScale(pageScale: Float) {
        userPreferencesDataSource.setPageScale(pageScale)
    }
}
