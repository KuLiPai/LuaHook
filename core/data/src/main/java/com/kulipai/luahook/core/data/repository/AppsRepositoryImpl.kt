package com.kulipai.luahook.core.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.kulipai.luahook.core.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsRepositoryImpl(
    private val context: Context
) : AppsRepository {

    override suspend fun getAppList(): Result<Pair<List<AppInfo>, List<Int>>> = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            // 获取所有已安装的包，包括卸载但保留数据的
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val appInfos = packages.mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null

                // 排除一些不需要显示的系统组件（可选，根据需求调整）
                // if (appInfo.packageName == "android") return@mapNotNull null

                AppInfo(
                    label = appInfo.loadLabel(pm).toString(),
                    packageInfo = packageInfo
                )
            }

            // 提取所有的用户 ID (UID / 100000)
            val userIds = appInfos.map { it.uid / 100000 }.distinct().sorted()

            Pair(appInfos, userIds)
        }
    }

    override suspend fun refreshProfiles(currentApps: List<AppInfo>): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        // 目前 AppInfo 中的 profile 字段已被注释掉，这里暂时返回原列表。
        // 如果后续需要更新 Profile 信息（例如从本地数据库或 Native 层读取），可以在这里实现。
        Result.success(currentApps)
    }
}