package com.kulipai.luahook.core.theme

import com.kulipai.luahook.core.data.repository.SettingsRepository
import com.kulipai.luahook.core.model.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ThemePreferenceManager(
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _themePreference = MutableStateFlow(ThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    val themeMode: StateFlow<ThemePreference> = themePreference

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        scope.launch {
            settingsRepository.userSettings
                .map { settings -> settings.themePreference }
                .collect { preference ->
                    _themePreference.value = preference
                    _isReady.value = true
                }
        }
    }
}
