package com.kulipai.luahook.hook.entry

import android.content.Context
import android.content.pm.ApplicationInfo
import com.kulipai.luahook.hook.api.HookLib
import com.kulipai.luahook.hook.api.LuaActivity
import com.kulipai.luahook.hook.api.LuaImport
import com.kulipai.luahook.hook.api.LuaProject
import com.kulipai.luahook.hook.api.LuaUtil
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.libxposed.api.XposedModuleInterface
import org.luaj.Globals
import org.luaj.lib.jse.CoerceJavaToLua
import org.luaj.lib.jse.JsePlatform
import org.luckypray.dexkit.DexKitBridge
import top.sacz.xphelper.XpHelper
import top.sacz.xphelper.dexkit.DexFinder

/**
 * 统一抽象化api101和低于101的hook的部分接口
 */

lateinit var LPParam_processName: String

interface LPParam {
    val packageName: String
    val classLoader: ClassLoader
    val appInfo: ApplicationInfo
    val isFirstApplication: Boolean
    val processName: String
}

class LoadPackageParamWrapper(val origin: LoadPackageParam) : LPParam {
    override val packageName: String get() = origin.packageName
    override val classLoader: ClassLoader get() = origin.classLoader
    override val appInfo: ApplicationInfo get() = origin.appInfo
    override val processName: String get() = origin.processName
    override val isFirstApplication: Boolean get() = origin.isFirstApplication
}

class ModuleInterfaceParamWrapper(val origin: XposedModuleInterface.PackageReadyParam) : LPParam {
    override val packageName get() = origin.packageName
    override val classLoader get() = origin.classLoader
    override val appInfo: ApplicationInfo get() = origin.applicationInfo
    override val processName: String get() = LPParam_processName
    override val isFirstApplication: Boolean get() = origin.isFirstPackage

}


// 创建一个Hook的lua环境

fun createGlobals(
    context: Any,
    lpparam: LPParam,
    suparam: IXposedHookZygoteInit.StartupParam,
    scriptName: String = "",
    projectName: String = ""
): Globals {
    val globals: Globals = JsePlatform.standardGlobals()

    //加载Lua模块
    globals["this"] = CoerceJavaToLua.coerce(context)
    globals["suparam"] = CoerceJavaToLua.coerce(suparam)
    LuaActivity(null).registerTo(globals)
    HookLib(lpparam, scriptName).registerTo(globals)

    LuaImport(lpparam.classLoader, context::class.java.classLoader!!).registerTo(
        globals,
        lpparam.packageName,
        projectName
    )
    LuaUtil.loadBasicLib(globals)

    LuaProject(projectName).registerTo(globals)



    return globals
}