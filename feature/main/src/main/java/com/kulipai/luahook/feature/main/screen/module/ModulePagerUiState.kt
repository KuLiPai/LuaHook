package com.kulipai.luahook.feature.main.screen.module

import androidx.compose.runtime.Immutable
import com.kulipai.luahook.core.ui.component.searchstatus.SearchStatus
import com.kulipai.luahook.core.model.DemoModule

@Immutable
data class ModulePagerUiState(
    val isRefreshing: Boolean = false,
    val moduleList: List<DemoModule> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus(""),
    val searchResults: List<DemoModule> = emptyList(),
    val showSystemApps: Boolean = false,
    val error: Throwable? = null
)
