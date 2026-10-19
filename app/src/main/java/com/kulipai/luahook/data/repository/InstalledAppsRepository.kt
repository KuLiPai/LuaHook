package com.kulipai.luahook.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.kulipai.luahook.core.pm.PackageUtils
import com.kulipai.luahook.data.model.AppInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface InstalledAppsRepository {
    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo>
    suspend fun getAppInfoList(packageNames: List<String>): List<AppInfo>
}

class InstalledAppsRepositoryImpl(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : InstalledAppsRepository {

    private val mutex = Mutex()
    @Volatile
    private var cachedAppList: List<AppInfo>? = null

    override suspend fun getInstalledApps(forceRefresh: Boolean): List<AppInfo> {
        if (!forceRefresh) {
            cachedAppList?.let { return it }
        }
        return mutex.withLock {
            if (!forceRefresh) {
                cachedAppList?.let { return it }
            }
            withContext(ioDispatcher) {
                val apps = PackageUtils.getInstalledApps(context)
                cachedAppList = apps
                apps
            }
        }
    }

    override suspend fun getAppInfoList(packageNames: List<String>): List<AppInfo> =
        withContext(ioDispatcher) {
            val pm = context.packageManager
            val appInfoList = mutableListOf<AppInfo>()

            for (pkg in packageNames) {
                try {
                    val packageInfo = pm.getPackageInfo(pkg, 0)
                    val applicationInfo = pm.getApplicationInfo(pkg, 0)

                    val appName = pm.getApplicationLabel(applicationInfo).toString()
                    val versionName = packageInfo.versionName ?: "N/A"
                    val versionCode = packageInfo.longVersionCode

                    val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                            (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val firstInstallTime = packageInfo.firstInstallTime
                    val lastUpdateTime = packageInfo.lastUpdateTime

                    appInfoList.add(
                        AppInfo(
                            appName = appName,
                            packageName = pkg,
                            versionName = versionName,
                            versionCode = versionCode,
                            isSystemApp = isSystemApp,
                            firstInstallTime = firstInstallTime,
                            lastUpdateTime = lastUpdateTime
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
            appInfoList
        }
}
