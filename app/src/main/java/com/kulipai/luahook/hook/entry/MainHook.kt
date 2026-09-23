package com.kulipai.luahook.hook.entry

import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.log.e
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kulipai.luahook.ext.dexkit.registerDexKit
import io.github.kulipai.luahook.ext.layout.registerLayout
import io.github.kulipai.luahook.ext.nativelib.registerNative
import io.github.kulipai.luahook.hook.entry.LuaHookEngine
import org.json.JSONArray
import org.luaj.Globals
import top.sacz.xphelper.XpHelper

/**
 * API 100 以下的入口，类名写在 assets/xposed_init。
 * LSPosed 1.9.2（约 API 93）和 JingMatrix 1.11.0（API 100）都走这里。
 * [NewHook] 是 API 102 的无参类，不注册，避免管理器提示模块为较新版本设计。
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

    /** Zygote 阶段记下启动参数，后面加载脚本要模块路径。 */
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XpHelper.initZygote(startupParam)
        suparam = startupParam
    }

    /** 每个被勾选的包都会进来。先接上引擎，再按工作区决定跑哪些脚本。 */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        LuaHookEngine.init(this, lpparam, suparam)
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
     * 读 /global.lua、/apps.txt、AppConf 和 /Project/info.json。
     * 全局脚本排除本模块。读出来是空的会打日志，用来区分 SELinux 没读到和脚本本身为空。
     * 应用脚本只在包名位于 apps.txt，且配置为启用时执行。
     */
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
                if (luaScript.isNotBlank()) {
                    val globals = LuaHookEngine.load(this, "[GLOBAL]")
                    registerExtensions(globals)
                    LuaHookEngine.run(globals, luaScript)
                }
            }
        } catch (e: Exception) {
            "${lpparam.packageName}:[GLOBAL]:${e.message}".e()
        }

        // app单独脚本
        if (lpparam.packageName in selectAppsList) {

            // 读取已保存的宿主app脚本的map
            for ((scriptName, v) in WorkspaceFileManager.readMap("/${WorkspaceFileManager.AppConf}/${lpparam.packageName}.txt")) {
                try {
                    val luaScript =
                        WorkspaceFileManager.read("/${WorkspaceFileManager.AppScript}/${lpparam.packageName}/$scriptName.lua")
                    if (v is Boolean) { // 兼容旧版luahook的存储格式
                        val globals = LuaHookEngine.load(this, scriptName)
                        registerExtensions(globals)
                        LuaHookEngine.run(globals, luaScript)

                    } else if ((v is JSONArray)) { // 新的格式，包含是否启用，描述和版本信息
                        if (v.optBoolean(0, false)) {
                            val globals = LuaHookEngine.load(this, scriptName)
                            registerExtensions(globals)
                            LuaHookEngine.run(globals, luaScript)
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


                            val globals = LuaHookEngine.load( this, projectName)
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