package com.kulipai.luahook.hook.entry

import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.log.e
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
 * 按 API 102 无参构造编译的入口。不写进 assets/xposed_init，也没有 java_init.list。
 *
 * JingMatrix 1.11.0 是 API 100，用的是带参构造 XposedModule(XposedInterface, ModuleLoadedParam)。
 * LSPosed 1.9.2 大约是 API 93。这两边都不会构造这个无参类。
 * module.prop 的 targetApiVersion 高于框架接口时，管理器会提示「为较新的 Xposed 版本设计」，
 * 所以 min/target 保持 53，和 AndroidManifest 的 xposedminversion 一样。
 * 102 以下实际加载的是 [MainHook]。框架能构造本类时，脚本路径和 MainHook 相同。
 */
class NewHook : XposedModule() {
    companion object {
        const val MODULE_PACKAGE = "com.kulipai.luahook"  // 模块包名
        const val PATH = "/data/local/tmp/LuaHook"
    }

    lateinit var luaScript: String
    lateinit var selectAppsString: String
    lateinit var selectAppsList: MutableList<String>

    /** API 102 的包就绪回调。记下模块 apk 路径后，按工作区加载全局脚本、应用脚本和项目。 */
    override fun onPackageReady(lpparam: XposedModuleInterface.PackageReadyParam) {
        super.onPackageReady(lpparam)
        XpHelper.moduleApkPath = moduleApplicationInfo.sourceDir
        LuaHookEngine.init(this, lpparam)
        luaHookInit(lpparam)
    }

    /** 给这次脚本补上布局、DexKit、Native。带项目名时再注册 LuaProject。 */
    private fun registerExtensions(globals: Globals, projectName: String = "") {
        globals.registerLayout()
        globals.registerDexKit()
        globals.registerNative()
        if (projectName.isNotEmpty()) {
            com.kulipai.luahook.hook.api.LuaProject(projectName).registerTo(globals)
        }
    }

    /**
     * 和 [MainHook.luaHookInit] 同一套规则，参数类型是 PackageReadyParam。
     * 全局脚本排除本模块。应用脚本只在包名位于 /apps.txt 且 AppConf 启用时执行。
     * 项目看 /Project/info.json 的开关，再按 init.lua 的 scope 决定要不要跑 main.lua。
     */
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
                val globals = LuaHookEngine.load(this, "[GLOBAL]")
                registerExtensions(globals)
                LuaHookEngine.run(globals,luaScript)
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
                        val globals = LuaHookEngine.load(this, scriptName)
                        registerExtensions(globals)
                        LuaHookEngine.run(globals,scriptText)
                    } else if (v is JSONArray && v.optBoolean(0, false)) {
                        val globals = LuaHookEngine.load(this, scriptName)
                        registerExtensions(globals)
                        LuaHookEngine.run(globals,scriptText)
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

                        val tempGlobals = LuaHookEngine.load(this, projectName)
                        LuaHookEngine.run(tempGlobals,initScript)
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
                            """.trimIndent()


                            val globals = LuaHookEngine.load(this, projectName)
                            LuaHookEngine.run(globals,wrappedScript)
                            registerExtensions(globals, projectName)
                            LuaHookEngine.run(globals,rawScript)
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