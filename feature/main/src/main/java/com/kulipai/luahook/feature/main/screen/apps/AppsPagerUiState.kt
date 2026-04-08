package com.kulipai.luahook.feature.main.screen.apps

import androidx.compose.runtime.Immutable
import com.kulipai.luahook.core.model.AppInfo
import com.kulipai.luahook.feature.main.components.searchstatus.SearchStatus

@Immutable
data class GroupedApps(
    val uid: Int,
    val apps: List<AppInfo>,
    val primary: AppInfo,
//    val anyAllowSu: Boolean,
//    val anyCustom: Boolean,
//    val shouldUmount: Boolean,
    val ownerName: String? = null,
)

data class AppsPagerUiState(
    val isRefreshing: Boolean = false,
    val appList: List<AppInfo> = emptyList(),
    val groupedApps: List<GroupedApps> = emptyList(),
    val userIds: List<Int> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus(""),
    val searchResults: List<AppInfo> = emptyList(),
    val showSystemApps: Boolean = false,
    val showOnlyPrimaryUserApps: Boolean = false,
    val error: Throwable? = null
)
