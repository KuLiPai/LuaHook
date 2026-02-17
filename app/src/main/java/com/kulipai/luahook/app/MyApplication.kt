package com.kulipai.luahook.app

import android.app.Application
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.lifecycle.Observer
import com.google.android.material.color.DynamicColors
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.core.crash.AppCrashHandler
import com.kulipai.luahook.core.language.LanguageUtils
import com.kulipai.luahook.core.pm.PackageUtils.getInstalledApps
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.core.xposed.XposedScope
import com.kulipai.luahook.data.model.AppInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全局函数和初始化shell等等
 */

class MyApplication : Application() {

    private var cachedAppList: List<AppInfo>? = null
    private var isLoading = false
    private val waiters = mutableListOf<CompletableDeferred<List<AppInfo>>>()

    companion object {
        @JvmStatic
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        AppCrashHandler.Companion.init(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
        instance = this
        LanguageUtils.applyLanguage(this)
        ShizukuApi.init()//在shell里init？
        // 预加载 shell，确保 MainActivity 能及时拿到状态
        ShellManager.init(applicationContext)

        // 当获取到 Root 或 Shizuku 权限时，清除缓存，以便重新加载完整应用列表
        // 使用一次性观察者，避免内存泄漏
        val modeObserver = object : Observer<ShellManager.Mode> {
            override fun onChanged(value: ShellManager.Mode) {
                // 在获取到权限时清除缓存
                if (value == ShellManager.Mode.ROOT || value == ShellManager.Mode.SHIZUKU || value == ShellManager.Mode.SHIZUKU_FALLBACK) {
                    cachedAppList = null
                }
                // 无论什么模式，第一次变化后都移除观察者
                ShellManager.mode.removeObserver(this)
            }
        }
        ShellManager.mode.observeForever(modeObserver)

        XposedScope.init()
    }


    // TODO)) 放进一个ViewMode里加载
    // 挂起函数：异步获取 AppInfo 列表
    suspend fun getAppInfoList(packageNames: List<String>): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = packageManager
            val appInfoList = mutableListOf<AppInfo>()

            for (pkg in packageNames) {
                try {
                    val packageInfo = pm.getPackageInfo(pkg, 0)
                    val applicationInfo = pm.getApplicationInfo(pkg, 0)

                    val appName = pm.getApplicationLabel(applicationInfo).toString()
                    pm.getApplicationIcon(applicationInfo)
                    val versionName = packageInfo.versionName ?: "N/A"
                    val versionCode =
                        packageInfo.longVersionCode

                    appInfoList.add(
                        AppInfo(
                            appName = appName,
                            packageName = pkg,
                            versionName = versionName,
                            versionCode = versionCode
                        )
                    )

                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }

            return@withContext appInfoList
        }


    // TODO)) 放进一个ViewMode里加载
    suspend fun getAppListAsync(): List<AppInfo> {
        if (cachedAppList != null) return cachedAppList!!

        val deferred = CompletableDeferred<List<AppInfo>>()
        waiters.add(deferred)

        if (!isLoading) {
            isLoading = true
            // 启动加载
            CoroutineScope(Dispatchers.IO).launch {
                val apps = getInstalledApps(applicationContext)
                withContext(Dispatchers.Main) {
                    cachedAppList = apps
                    isLoading = false
                    waiters.forEach { it.complete(apps) }
                    waiters.clear()
                }
            }
        }

        return deferred.await()
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LanguageUtils.applyLanguage(this)
    }

}