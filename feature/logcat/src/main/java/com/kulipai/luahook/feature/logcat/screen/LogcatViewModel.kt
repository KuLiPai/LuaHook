package com.kulipai.luahook.feature.logcat.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kulipai.luahook.core.model.DemoContent
import com.kulipai.luahook.core.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LogcatUiState(
    val isLoading: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val mode: LogcatMode = LogcatMode.ROOT_LOGCAT,
    val filterTag: String = "",
    val filterLevel: String? = null,
)

enum class LogcatMode(val label: String) {
    ROOT_LOGCAT("Root Logcat"),
    APP_INTERNAL("应用内部目录"),
}

class LogcatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LogcatUiState())
    val uiState: StateFlow<LogcatUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // TODO: Replace with actual log loading
            val logs = withContext(Dispatchers.IO) {
                DemoContent.logEntries
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    logs = logs,
                )
            }
        }
    }

    fun setMode(mode: LogcatMode) {
        _uiState.update { it.copy(mode = mode) }
        load()
    }

    fun setFilterTag(tag: String) {
        _uiState.update { it.copy(filterTag = tag) }
    }

    fun setFilterLevel(level: String?) {
        _uiState.update { it.copy(filterLevel = level) }
    }
}
