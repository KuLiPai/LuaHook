package com.kulipai.luahook.core.data.repository

import com.kulipai.luahook.core.model.AppInfo
import kotlinx.coroutines.flow.StateFlow

data class AppsRepositoryState(
    val apps: List<AppInfo> = emptyList(),
    val userIds: List<Int> = emptyList(),
    val isLoaded: Boolean = false,
    val error: Throwable? = null,
)

interface AppsRepository {
    val state: StateFlow<AppsRepositoryState>

    suspend fun loadApps(force: Boolean = false): Result<AppsRepositoryState>

    suspend fun refreshProfiles(): Result<AppsRepositoryState>
}
