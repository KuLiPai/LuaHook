package com.kulipai.luahook.hook.entry

import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.log.e
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kulipai.luahook.hook.entry.LuaHookEngine
import io.github.kulipai.luahook.ext.layout.registerLayout
import io.github.kulipai.luahook.ext.dexkit.registerDexKit
import io.github.kulipai.luahook.ext.nativelib.registerNative
import org.json.JSONArray
import org.luaj.Globals
import top.sacz.xphelper.XpHelper

/**
 * MainHook是用于xposed api小于100的hook主类
 * 加载lua脚本hook宿主
 */

class MainHook : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
        const val MODULE_PACKAGE = "com.kulipai.luahook"  // 模块包名
        const val PATH = "/data/local/tmp/LuaHook"
    }

    lateinit var luaScript: String
    lateinit var selectAppsString: String

    lateinit var selectAppsList: MutableList<String>
    lateinit var suparam: IXposedHookZygoteInit.StartupParam

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XpHelper.initZygote(startupParam)
        suparam = startupParam
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
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

    fun luaHookInit(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 读取luahook启用的app
        selectAppsString = WorkspaceFileManager.read("/apps.txt").replace("\n", "")
        // 读取全局脚本
        luaScript = WorkspaceFileManager.read("/global.lua")
        selectAppsList = if (selectAppsString.isNotEmpty() && selectAppsString != "") {
            selectAppsString.split(",").toMutableList()
        } else {
            mutableListOf()
        }

        // 全局脚本
        try {
            // 排除模块自己
            if (lpparam.packageName != MODULE_PACKAGE) {
                val globals = LuaHookEngine.load(luaScript, this, "[GLOBAL]")
                registerExtensions(globals)
            }
        } catch (e: Exception) {
            "${lpparam.packageName}:[GLOBAL]:${e.message}".e()
        }

        // app单独脚本
        if (lpparam.packageName in selectAppsList) {

            // 读取已保存的宿主app脚本的map
            for ((scriptName, v) in WorkspaceFileManager.readMap("/${WorkspaceFileManager.AppConf}/${lpparam.packageName}.txt")) {
                try {
                    if (v is Boolean) { // 兼容旧版luahook的存储格式
                        val globals = LuaHookEngine.load(WorkspaceFileManager.read("/${WorkspaceFileManager.AppScript}/${lpparam.packageName}/$scriptName.lua"), this, scriptName)
                        registerExtensions(globals)
                    } else if ((v is JSONArray)) { // 新的格式，包含是否启用，描述和版本信息
                        if (v.optBoolean(0, false)) {
                            val globals = LuaHookEngine.load(WorkspaceFileManager.read("/${WorkspaceFileManager.AppScript}/${lpparam.packageName}/$scriptName.lua"), this, scriptName)
                            registerExtensions(globals)
                        }
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

}