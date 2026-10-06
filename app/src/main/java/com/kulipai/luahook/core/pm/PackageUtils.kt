package com.kulipai.luahook.core.pm

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.kulipai.luahook.data.model.AppInfo

object PackageUtils {
    fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }


    fun getAppVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode // 注意这里使用 longVersionCode，在旧版本中是 versionCode (Int)
        } catch (_: PackageManager.NameNotFoundException) {
            -1 // 或者其他表示未找到的数值
        }
    }



    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()
        val packages = pm.getInstalledPackages(0)

        for (packageInfo in packages) {
            val app = packageInfo.applicationInfo ?: try {
                pm.getApplicationInfo(packageInfo.packageName, 0)
            } catch (_: Exception) {
                null
            }

            val appName = if (app != null) {
                try {
                    pm.getApplicationLabel(app).toString()
                } catch (_: Exception) {
                    packageInfo.packageName
                }
            } else {
                packageInfo.packageName
            }

            val packageName = packageInfo.packageName
            val versionName = packageInfo.versionName ?: "N/A"
            val versionCode = packageInfo.longVersionCode
            val isSystemApp = if (app != null) {
                (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            } else {
                packageName == "android"
            }

            apps.add(
                AppInfo(
                    appName = appName,
                    packageName = packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    isSystemApp = isSystemApp,
                    firstInstallTime = packageInfo.firstInstallTime,
                    lastUpdateTime = packageInfo.lastUpdateTime
                )
            )
        }

        return apps.sortedBy { it.appName.lowercase() }
    }
}