package com.kulipai.luahook.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kulipai.luahook.ui.theme.UiMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * @author kulipai
 * @date 2026/3/17
 */

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.dataStore

    val pageScaleFlow: Flow<Float>
        get() = dataStore.data.map { it[SettingsKeys.PAGE_SCALE] ?: 1f }

    val enableBlurFlow: Flow<Boolean>
        get() = dataStore.data.map { it[SettingsKeys.ENABLE_BLUR] ?: true }

    val uiModeFlow: Flow<String>
        get() = dataStore.data.map { it[SettingsKeys.UI_MODE] ?: UiMode.DEFAULT_VALUE }

    suspend fun setPageScale(value: Float) {
        dataStore.edit {
            it[SettingsKeys.PAGE_SCALE] = value
        }
    }

    suspend fun setEnableBlur(value: Boolean) {
        dataStore.edit {
            it[SettingsKeys.ENABLE_BLUR] = value
        }
    }

    suspend fun setUiMode(value: String) {
        dataStore.edit {
            it[SettingsKeys.UI_MODE] = value
        }
    }

}


object SettingsKeys {
    val PAGE_SCALE = floatPreferencesKey("page_scale")
    val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
    val ENABLE_FLOATING_BOTTOM_BAR = booleanPreferencesKey("enable_floating_bottom_bar")
    val ENABLE_FLOATING_BOTTOM_BAR_BLUR = booleanPreferencesKey("enable_floating_bottom_bar_blur")
    val UI_MODE = stringPreferencesKey("ui_mode")
}