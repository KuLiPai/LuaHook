package com.kulipai.luahook.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kulipai.luahook.core.data.model.AppSettings
import com.kulipai.luahook.core.data.model.ThemeSettings
import com.kulipai.luahook.core.data.model.UserSettings
import com.kulipai.luahook.core.model.ColorMode
import com.kulipai.luahook.core.model.UiMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val SETTINGS_DATASTORE_NAME = "settings"
private const val LEGACY_SHARED_PREFERENCES_NAME = "settings"

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_SHARED_PREFERENCES_NAME))
    },
)

class UserSettingsDataSource(
    private val context: Context,
) {
    private val dataStore = context.userPreferencesDataStore

    val userSettings: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(Preferences::asUserSettings)

    suspend fun setUiMode(uiMode: UiMode) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.UI_MODE] = uiMode.value
        }
    }

    suspend fun setColorMode(colorMode: ColorMode) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.COLOR_MODE] = colorMode.value
        }
    }

    suspend fun setUseMiuixMonet(useMiuixMonet: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.MIUIX_MONET] = useMiuixMonet
        }
    }

    suspend fun setKeyColor(keyColor: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.KEY_COLOR] = keyColor
        }
    }

    suspend fun setPaletteStyle(paletteStyle: PaletteStyle) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.COLOR_STYLE] = paletteStyle.name
        }
    }

    suspend fun setColorSpec(colorSpec: ColorSpec.SpecVersion) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.COLOR_SPEC] = colorSpec.name
        }
    }

    suspend fun setEnableBlur(enableBlur: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.ENABLE_BLUR] = enableBlur
        }
    }

    suspend fun setEnableFloatingBottomBar(enableFloatingBottomBar: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.ENABLE_FLOATING_BOTTOM_BAR] =
                enableFloatingBottomBar
        }
    }

    suspend fun setEnableFloatingBottomBarBlur(enableFloatingBottomBarBlur: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.ENABLE_FLOATING_BOTTOM_BAR_BLUR] =
                enableFloatingBottomBarBlur
        }
    }

    suspend fun setPageScale(pageScale: Float) {
        dataStore.edit { preferences ->
            preferences[SettingsPreferencesKeys.PAGE_SCALE] = pageScale
        }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit {
            it[SettingsPreferencesKeys.LANGUAGE] = language
        }
    }
}
private fun Preferences.asUserSettings(): UserSettings {
    val default = UserSettings()

    val theme = ThemeSettings(
        uiMode = UiMode.fromValue(
            this[SettingsPreferencesKeys.UI_MODE] ?: UiMode.DEFAULT_VALUE
        ),
        colorMode = ColorMode.fromValue(
            this[SettingsPreferencesKeys.COLOR_MODE] ?: ColorMode.SYSTEM.value
        ),
        useMiuixMonet = this[SettingsPreferencesKeys.MIUIX_MONET]
            ?: default.theme.useMiuixMonet,
        keyColor = this[SettingsPreferencesKeys.KEY_COLOR]
            ?: default.theme.keyColor,
        paletteStyle = this[SettingsPreferencesKeys.COLOR_STYLE]
            ?.let(::parsePaletteStyle)
            ?: default.theme.paletteStyle,
        colorSpec = this[SettingsPreferencesKeys.COLOR_SPEC]
            ?.let(::parseColorSpec)
            ?: default.theme.colorSpec,
        enableBlur = this[SettingsPreferencesKeys.ENABLE_BLUR]
            ?: default.theme.enableBlur,
        enableFloatingBottomBar = this[SettingsPreferencesKeys.ENABLE_FLOATING_BOTTOM_BAR]
            ?: default.theme.enableFloatingBottomBar,
        enableFloatingBottomBarBlur = this[SettingsPreferencesKeys.ENABLE_FLOATING_BOTTOM_BAR_BLUR]
            ?: default.theme.enableFloatingBottomBarBlur,
        pageScale = this[SettingsPreferencesKeys.PAGE_SCALE]
            ?: default.theme.pageScale,
    )

    val app = AppSettings(
        language = this[SettingsPreferencesKeys.LANGUAGE]
            ?: default.app.language
    )

    return UserSettings(
        theme = theme,
        app = app
    )
}

private fun parsePaletteStyle(value: String): PaletteStyle {
    return try {
        PaletteStyle.valueOf(value)
    } catch (_: IllegalArgumentException) {
        PaletteStyle.TonalSpot
    }
}

private fun parseColorSpec(value: String): ColorSpec.SpecVersion {
    return try {
        ColorSpec.SpecVersion.valueOf(value)
    } catch (_: IllegalArgumentException) {
        ColorSpec.SpecVersion.Default
    }
}

object SettingsPreferencesKeys {
    val PAGE_SCALE = floatPreferencesKey("page_scale")
    val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
    val ENABLE_FLOATING_BOTTOM_BAR = booleanPreferencesKey("enable_floating_bottom_bar")
    val ENABLE_FLOATING_BOTTOM_BAR_BLUR = booleanPreferencesKey("enable_floating_bottom_bar_blur")
    val UI_MODE = stringPreferencesKey("ui_mode")
    val COLOR_MODE = intPreferencesKey("color_mode")
    val MIUIX_MONET = booleanPreferencesKey("miuix_monet")
    val KEY_COLOR = intPreferencesKey("key_color")
    val COLOR_STYLE = stringPreferencesKey("color_style")
    val COLOR_SPEC = stringPreferencesKey("color_spec")
    val LANGUAGE = stringPreferencesKey("language")
}
