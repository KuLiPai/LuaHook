package com.kulipai.luahook.app

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import com.google.android.material.color.DynamicColors
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.crash.AppCrashHandler
import com.kulipai.luahook.core.language.LanguageUtils
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.core.xposed.XposedScope
import com.kulipai.luahook.data.model.AppInfo
import com.kulipai.luahook.feature.error.ErrorActivity

/**
 * 全局函数和应用初始化入口
 */
class MyApplication : Application() {

    companion object {
        @JvmStatic
        var instanceOrNull: MyApplication? = null
            private set

        @JvmStatic
        val instance: MyApplication
            get() = instanceOrNull ?: throw IllegalStateException("MyApplication is not initialized")

        val containerOrNull: AppContainer?
            get() = instanceOrNull?.appContainer

        val container: AppContainer
            get() = containerOrNull ?: throw IllegalStateException("AppContainer is not initialized")
    }

    lateinit var appContainer: AppContainer
        private set

    var isStandaloneModule: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        instanceOrNull = this

        isStandaloneModule = runCatching {
            assets.open("standalone_project/pack_config.json").use { true }
        }.getOrDefault(false)

        // 统一初始化应用级轻量依赖注入容器
        appContainer = AppContainer(this)

        // 统一初始化全局崩溃处理器，通过高阶函数解耦 ErrorActivity 依赖
        AppCrashHandler.init(this) { context, errorMessage, stackTrace ->
            val intent = Intent(context, ErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(ErrorActivity.EXTRA_ERROR_MESSAGE, errorMessage)
                putExtra(ErrorActivity.EXTRA_STACK_TRACE, stackTrace)
            }
            context.startActivity(intent)
        }

        DynamicColors.applyToActivitiesIfAvailable(this)
        LanguageUtils.applyLanguage(this)

        if (!isStandaloneModule) {
            ShizukuApi.init()
            // 启动容器编排与基础设施加载
            appContainer.initialize()
        }

        XposedScope.init()
    }

    @Deprecated(
        message = "Use AppContainer.instance.installedAppsRepository.getAppInfoList instead",
        replaceWith = ReplaceWith("AppContainer.instance.installedAppsRepository.getAppInfoList(packageNames)")
    )
    suspend fun getAppInfoList(packageNames: List<String>): List<AppInfo> =
        appContainer.installedAppsRepository.getAppInfoList(packageNames)

    @Deprecated(
        message = "Use AppContainer.instance.installedAppsRepository.getInstalledApps instead",
        replaceWith = ReplaceWith("AppContainer.instance.installedAppsRepository.getInstalledApps()")
    )
    suspend fun getAppListAsync(): List<AppInfo> =
        appContainer.installedAppsRepository.getInstalledApps()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LanguageUtils.applyLanguage(this)
    }
}