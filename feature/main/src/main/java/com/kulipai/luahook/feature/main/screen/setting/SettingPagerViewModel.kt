package com.kulipai.luahook.feature.main.screen.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.data.repository.SettingsRepository
import com.kulipai.luahook.core.model.UiMode
import com.kulipai.luahook.feature.main.screen.setting.model.SettingsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingPagerViewModel(
    private val repo: SettingsRepository
) : ViewModel() {
    val settingsUiState = repo.userSettings
        .map { settings ->
            SettingsUiState(
                themePreference = settings.theme,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun setUiMode(mode: UiMode) {
        viewModelScope.launch {
            repo.setUiMode(mode)
        }
    }
}
