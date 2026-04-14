package com.kulipai.luahook.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.kulipai.luahook.core.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppsRepositoryImpl(
    private val context: Context,
) : AppsRepository {

    private val repositoryMutex = Mutex()

    private val _state = MutableStateFlow(AppsRepositoryState())
    override val state: StateFlow<AppsRepositoryState> = _state.asStateFlow()

    override suspend fun loadApps(force: Boolean): Result<AppsRepositoryState> {
        return repositoryMutex.withLock {
            val currentState = _state.value
            if (!force && currentState.isLoaded) {
                Result.success(currentState)
            } else {
                loadInstalledApps()
                    .map { (apps, userIds) ->
                        AppsRepositoryState(
                            apps = apps,
                            userIds = userIds,
                            isLoaded = true,
                            error = null,
                        )
                    }
                    .onSuccess { newState ->
                        _state.value = newState
                    }
                    .onFailure { throwable ->
                        _state.update { it.copy(error = throwable) }
                    }
            }
        }
    }

    override suspend fun refreshProfiles(): Result<AppsRepositoryState> {
        return repositoryMutex.withLock {
            val currentState = _state.value
            if (!currentState.isLoaded) {
                loadApps()
            } else {
                refreshProfileData(currentState.apps)
                    .map { updatedApps ->
                        currentState.copy(
                            apps = updatedApps,
                            userIds = updatedApps
                                .map { appInfo -> appInfo.uid / 100000 }
                                .distinct()
                                .sorted(),
                            isLoaded = true,
                            error = null,
                        )
                    }
                    .onSuccess { newState ->
                        _state.value = newState
                    }
                    .onFailure { throwable ->
                        _state.update { it.copy(error = throwable) }
                    }
            }
        }
    }

    private suspend fun loadInstalledApps(): Result<Pair<List<AppInfo>, List<Int>>> = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val appInfos = packages.mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                AppInfo(
                    label = appInfo.loadLabel(pm).toString(),
                    packageInfo = packageInfo,
                )
            }

            val userIds = appInfos.map { it.uid / 100000 }.distinct().sorted()
            Pair(appInfos, userIds)
        }
    }

    private suspend fun refreshProfileData(currentApps: List<AppInfo>): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        Result.success(currentApps)
    }
}
