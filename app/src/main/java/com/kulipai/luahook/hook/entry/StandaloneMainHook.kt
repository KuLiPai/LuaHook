package com.kulipai.luahook.hook.entry

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kulipai.luahook.hook.entry.LuaHookEngine
import top.sacz.xphelper.XpHelper

/**
 * 独立打包模块专属 Hook 入口。
 *
 * 声明于打包后 APK 的 `assets/xposed_init` 中，完全解耦于 LuaHook 主应用工作区：
 * 1. 绝不读取或依赖 `/data/local/tmp/LuaHook/`、`apps.txt` 或 `global.lua`；
 * 2. 纯粹从独立模块自身的 APK 内部 (`assets/standalone_project/`) 加载并执行项目脚本；
 * 3. 严格适配各种 Xposed 框架环境（LSPosed、EdXposed、VirtualXposed 等），支持 Zygote 启动与免 Zygote 注入。
 */
class StandaloneMainHook : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
        private const val TAG = "StandaloneMainHook"

        @Volatile
        var zygoteModulePath: String = ""
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        val path = startupParam.modulePath ?: ""
        zygoteModulePath = path
        XpHelper.moduleApkPath = path
        runCatching {
            XpHelper.initZygote(startupParam)
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 初始化基础 Hook 引擎上下文
            runCatching {
                LuaHookEngine.init(this, lpparam)
            }

            // 执行独立模块运行时
            StandaloneHookRunner.runStandaloneHook(
                hookInstance = this,
                moduleApkPath = zygoteModulePath,
                lpparam = lpparam
            )
        } catch (t: Throwable) {
            val errMsg = "[StandaloneMainHook] Fatal error in ${lpparam.packageName}: ${t.message}"
            Log.e(TAG, errMsg, t)
            runCatching { XposedBridge.log(errMsg) }
        }
    }
}
