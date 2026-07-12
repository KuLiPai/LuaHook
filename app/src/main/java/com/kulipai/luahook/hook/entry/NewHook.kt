package com.kulipai.luahook.hook.entry

import android.annotation.SuppressLint
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.log.e
import de.robv.android.xposed.IXposedHookZygoteInit
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.kulipai.luahook.hook.entry.LuaHookEngine
import io.github.kulipai.luahook.ext.layout.registerLayout
import io.github.kulipai.luahook.ext.dexkit.registerDexKit
import io.github.kulipai.luahook.ext.nativelib.registerNative
import org.json.JSONArray
import org.luaj.Globals
import top.sacz.xphelper.XpHelper

/**
 * api101专用新hook入口
 */

class NewHook : XposedModule() {
    companion object {
        const val MODULE_PACKAGE = "com.kulipai.luahook"  // 模块包名
        const val PATH = "/data/local/tmp/LuaHook"
    }

    lateinit var luaScript: String
    lateinit var selectAppsString: String
    lateinit var selectAppsList: MutableList<String>
    lateinit var suparam: IXposedHookZygoteInit.StartupParam

    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageReady(lpparam: XposedModuleInterface.PackageReadyParam) {
        super.onPackageReady(lpparam)
        // LPParam_processName = lpparam.applicationInfo.processName ?: lpparam.packageName
        suparam = createStartupParam(this.moduleApplicationInfo.sourceDir)
        XpHelper.initZygote(suparam)

        LuaHookEngine.init(this, lpparam, suparam)
        luaHookInit(lpparam)
    }

    private fun registerExtensions(globals: Globals, projectName: String = "") {
        globals.registerLayout()
        globals.registerDexKit()
        globals.registerNative()
        if (projectName.isNotEmpty()) {
            com.kulipai.luahook.hook.api.LuaProject(projectName).registerTo(globals)
        }
    }

    fun luaHookInit(lpparam: XposedModuleInterface.PackageReadyParam) {

        selectAppsString = WorkspaceFileManager.read("/apps.txt").replace("\n", "")
        luaScript = WorkspaceFileManager.read("/global.lua")

        selectAppsList = if (selectAppsString.isNotEmpty() && selectAppsString != "") {
            selectAppsString.split(",").toMutableList()
        } else {
            mutableListOf()
        }

        //全局脚本
        try {
            //排除自己
            if (lpparam.packageName != MODULE_PACKAGE) {
                val globals = LuaHookEngine.load(luaScript, this, "[GLOBAL]")
                registerExtensions(globals)
            }
        } catch (e: Exception) {
            "${lpparam.packageName}:[GLOBAL]:${e.message}".e()
        }

        // app单独脚本
        if (lpparam.packageName in selectAppsList) {
            for ((scriptName, v) in WorkspaceFileManager.readMap("/${WorkspaceFileManager.AppConf}/${lpparam.packageName}.txt")) {
                try {
                    val scriptText = WorkspaceFileManager.read("/${WorkspaceFileManager.AppScript}/${lpparam.packageName}/$scriptName.lua")
                    if (v is Boolean && v) {
                        val globals = LuaHookEngine.load(scriptText, this, scriptName)
                        registerExtensions(globals)
                    } else if (v is JSONArray && v.optBoolean(0, false)) {
                        val globals = LuaHookEngine.load(scriptText, this, scriptName)
                        registerExtensions(globals)
                    }
                } catch (e: Exception) {
                    ("[Error] | Package: ${lpparam.packageName} | Script: $scriptName | Message: ${e.message}").e()
                }
            }
        }

        // Project Hooks
        try {
            val projectInfo = WorkspaceFileManager.readMap("/Project/info.json")
            for ((projectName, isEnabled) in projectInfo) {
                if (isEnabled == true) {
                    try {
                        val projectDir = "/Project/$projectName"
                        val initScript = WorkspaceFileManager.read("$projectDir/init.lua")

                        val tempGlobals = LuaHookEngine.load(initScript, this, projectName)

                        val scope = tempGlobals.get("scope")
                        var shouldRun = false

                        if (scope.isstring() && scope.tojstring() == "all") {
                            shouldRun = true
                        } else if (scope.istable()) {
                            val len = scope.length()
                            for (i in 1..len) {
                                if (scope.get(i).tojstring() == lpparam.packageName) {
                                    shouldRun = true
                                    break
                                }
                            }
                        }

                        if (shouldRun) {
                            val rawScript = WorkspaceFileManager.read("$projectDir/main.lua")
                            val absProjectDir = WorkspaceFileManager.DIR + projectDir
                            val wrappedScript = """
                                package.path = package.path .. ';${absProjectDir}/?.lua'
                                local oldLoadDex = loadDex
                                if oldLoadDex then
                                    loadDex = function(path)
                                        if string.sub(path, 1, 1) ~= "/" then
                                            path = "${absProjectDir}/" .. path
                                        end
                                        return oldLoadDex(path)
                                    end
                                end
                            """.trimIndent() + "\n" + rawScript

                            val globals = LuaHookEngine.load(wrappedScript, this, projectName)
                            registerExtensions(globals, projectName)
                        }
                    } catch (e: Exception) {
                        "${lpparam.packageName}:[Project:$projectName]:${e.message}".e()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createStartupParam(modulePath: String): IXposedHookZygoteInit.StartupParam {
        val clazz = IXposedHookZygoteInit.StartupParam::class.java
        val constructor = clazz.getDeclaredConstructor()
        constructor.isAccessible = true
        val instance = constructor.newInstance()

        // 设置字段值
        val fieldModulePath = clazz.getDeclaredField("modulePath")
        fieldModulePath.isAccessible = true
        fieldModulePath.set(instance, modulePath)

        return instance
    }
}