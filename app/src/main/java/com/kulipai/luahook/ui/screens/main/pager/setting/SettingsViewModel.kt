package com.kulipai.luahook.ui.screens.main.pager.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.data.repository.SettingsRepository
import com.kulipai.luahook.ui.theme.UiMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    // 使用 stateIn 将 Flow 转换为 StateFlow，并设置初始值为 null 或一个特殊状态
    // 这样我们可以判断数据是否已经从 DataStore 加载完成
    val uiMode = repo.uiModeFlow
        .map { UiMode.fromValue(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // 初始为 null 表示还在加载
        )

    fun setUiMode(mode: String) {
        viewModelScope.launch {
            repo.setUiMode(mode)
        }
    }
}
