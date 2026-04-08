package com.kulipai.luahook.core.data.repository

import com.kulipai.luahook.core.model.AppInfo

interface AppsRepository {
    suspend fun getAppList(): Result<Pair<List<AppInfo>, List<Int>>>
    suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>>

}