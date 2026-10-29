package com.kulipai.luahook.hook.entry

import android.util.Log
import com.kulipai.luahook.core.log.d
import com.kulipai.luahook.core.log.e
import com.kulipai.luahook.hook.api.LuaProject
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kulipai.luahook.ext.dexkit.registerDexKit
import io.github.kulipai.luahook.ext.layout.registerLayout
import io.github.kulipai.luahook.ext.nativelib.registerNative
import io.github.kulipai.luahook.hook.entry.LuaHookEngine
import org.json.JSONObject
import org.luaj.Globals
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.Varargs
import org.luaj.lib.OneArgFunction
import org.luaj.lib.TwoArgFunction
import org.luaj.lib.VarArgFunction
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

/**
 * 独立模块运行时引导器（运行于被 Hook 的目标宿主进程中）。
 *
 * 当模块 APK 内置了 `assets/standalone_project/` 时，自动接管 Hook 生命周期：
 * 1. 自动提取 `assets/standalone_project/` 下的全部脚本、子目录与资产至目标宿主的私有目录；
 * 2. 同时在内存中建立 `assets/standalone_project/` 虚拟文件系统映射，即使在无磁盘写权限的受限进程（如 `android` 系统进程）中也能 100% 正常加载；
 * 3. 全面适配 `getProjectDir`、`require`、`dofile`、`loadfile`、`loadDex`、`file.*`、`io.open` 与 `loadDrawableFromFile`：
 *    支持相对路径、点号模块名、子目录脚本，以及自动重定向硬编码的 `/data/local/tmp/LuaHook/Project/<projectName>/` 路径。
 */
object StandaloneHookRunner {

    const val ASSET_PREFIX = "assets/standalone_project/"
    const val INIT_LUA_ENTRY = "assets/standalone_project/init.lua"
    const val MAIN_LUA_ENTRY = "assets/standalone_project/main.lua"
    const val PACK_CONFIG_ENTRY = "assets/standalone_project/pack_config.json"

    private const val WORKSPACE_PROJECT_PREFIX = "/data/local/tmp/LuaHook/Project/"
    private const val TAG = "StandaloneHook"

    @Volatile
    private var cachedIsStandalone: Boolean? = null

    /**
     * 独立入口专用的顶层执行方法。
     */
    fun runStandaloneHook(
        hookInstance: Any,
        moduleApkPath: String,
        lpparam: XC_LoadPackage.LoadPackageParam
    ): Boolean {
        return tryRunStandalone(
            hookInstance = hookInstance,
            moduleApkPath = moduleApkPath,
            targetPackageName = lpparam.packageName,
            targetDataDir = lpparam.appInfo?.dataDir
        )
    }

    /**
     * 检测当前模块 APK 是否为独立打包模块，若是则按独立模块逻辑执行并返回 true。
     * 若为普通 LuaHook 主模块，则立即返回 false。
     */
    fun tryRunStandalone(
        hookInstance: Any,
        moduleApkPath: String,
        targetPackageName: String,
        targetDataDir: String? = null
    ): Boolean {
        val resolvedApkPath = resolveModuleApkPath(moduleApkPath, hookInstance)
        if (resolvedApkPath.isBlank()) return false

        val apkFile = File(resolvedApkPath)
        if (!apkFile.exists()) return false

        try {
            ZipFile(apkFile).use { zip ->
                val initEntry = zip.getEntry(INIT_LUA_ENTRY)
                val mainEntry = zip.getEntry(MAIN_LUA_ENTRY)
                if (initEntry == null && mainEntry == null) {
                    cachedIsStandalone = false
                    return false
                }
                cachedIsStandalone = true

                // 读取所有 assets/standalone_project/ 下的项目文件到内存映射，兼顾磁盘释放与零磁盘内存兜底
                val projectAssets = readAllProjectAssets(zip)

                val configJson = projectAssets["pack_config.json"]?.toString(StandardCharsets.UTF_8)
                val configObj = configJson?.takeIf { it.isNotBlank() }?.let {
                    runCatching { JSONObject(it) }.getOrNull()
                }

                val modulePackage = configObj?.optString("packageName", "") ?: ""
                var projectName = configObj?.optString("projectName", "")?.takeIf { it.isNotBlank() } ?: "Standalone"

                val configuredScopes = mutableListOf<String>()
                val scopesArr = configObj?.optJSONArray("scopes")
                if (scopesArr != null) {
                    for (i in 0 until scopesArr.length()) {
                        val s = scopesArr.optString(i, "").trim()
                        if (s.isNotEmpty()) configuredScopes.add(s)
                    }
                }

                // 1. 默认排除模块自身包名（除非用户在打包作用域中显式指定了模块自身）
                if (modulePackage.isNotBlank() && targetPackageName == modulePackage) {
                    if (!configuredScopes.contains(modulePackage)) {
                        return true
                    }
                }

                // 2. 检查作用域匹配：若配置了特定包名范围（非空且不包含 all / *），且当前目标应用不在其中，则直接跳过
                if (configuredScopes.isNotEmpty() && !configuredScopes.contains("all") && !configuredScopes.contains("*")) {
                    if (!configuredScopes.contains(targetPackageName)) {
                        val skipMsg = "[StandaloneHook] Target '$targetPackageName' not in configured scopes $configuredScopes, skipping."
                        runCatching { XposedBridge.log(skipMsg) }
                        Log.d(TAG, skipMsg)
                        return true
                    }
                }

                val initScript = projectAssets["init.lua"]?.toString(StandardCharsets.UTF_8).orEmpty()
                val rawMainScript = projectAssets["main.lua"]?.toString(StandardCharsets.UTF_8)
                if (rawMainScript.isNullOrBlank()) {
                    return true
                }

                // 将项目资源释放到宿主应用私有目录，保证 getProjectDir、require、loadDex 及文件读写正常工作
                val extractedDir = extractProjectAssetsIfNeeded(
                    projectAssets = projectAssets,
                    apkFile = apkFile,
                    targetPackageName = targetPackageName,
                    targetDataDir = targetDataDir,
                    projectName = projectName,
                    modulePackageName = modulePackage
                )
                val absProjectDir = extractedDir?.absolutePath ?: ""

                // 解析 init.lua 中的项目名称与作用域声明（若存在）
                if (initScript.isNotBlank()) {
                    runCatching {
                        val tempGlobals = LuaHookEngine.load(hookInstance, projectName)
                        configureStandaloneLuaEnv(
                            globals = tempGlobals,
                            projectName = projectName,
                            absProjectDir = absProjectDir,
                            projectAssets = projectAssets
                        )
                        LuaHookEngine.run(tempGlobals, initScript)
                        val luaName = tempGlobals.get("name")
                        if (luaName.isstring() && luaName.tojstring().isNotBlank()) {
                            projectName = luaName.tojstring()
                        }

                        // 若 init.lua 中定义了 scope 表，按需过滤
                        val luaScope = tempGlobals.get("scope")
                        if (!luaScope.isnil()) {
                            var scopeMatched = false
                            if (luaScope.isstring()) {
                                val s = luaScope.tojstring()
                                if (s == "all" || s == "*" || s == targetPackageName) {
                                    scopeMatched = true
                                }
                            } else if (luaScope.istable()) {
                                val len = luaScope.length()
                                for (i in 1..len) {
                                    if (luaScope.get(i).tojstring() == targetPackageName) {
                                        scopeMatched = true
                                        break
                                    }
                                }
                            }
                            if (!scopeMatched) {
                                val skipMsg = "[StandaloneHook] Target '$targetPackageName' not in init.lua scope table, skipping."
                                runCatching { XposedBridge.log(skipMsg) }
                                Log.d(TAG, skipMsg)
                                return true
                            }
                        }
                    }
                }

                // 初始化主运行环境并注入完整项目目录与模块加载适配
                val globals: Globals = LuaHookEngine.load(hookInstance, projectName)
                configureStandaloneLuaEnv(
                    globals = globals,
                    projectName = projectName,
                    absProjectDir = absProjectDir,
                    projectAssets = projectAssets
                )

                // 先在主环境中静默装载 init.lua 定义的元数据全局变量（如 version、author 等），再运行 main.lua
                if (initScript.isNotBlank()) {
                    runCatching { globals.load(initScript, "@init.lua").call() }
                }

                val logMsg = "[StandaloneHook] Running project '$projectName' in $targetPackageName (dir=$absProjectDir)"
                runCatching { XposedBridge.log(logMsg) }
                Log.i(TAG, logMsg)
                logMsg.d()

                LuaHookEngine.run(globals, rawMainScript)
                return true
            }
        } catch (e: Throwable) {
            val errMsg = "[StandaloneHook] | Package: $targetPackageName | Error: ${e.message}"
            runCatching { XposedBridge.log(errMsg) }
            Log.e(TAG, errMsg, e)
            errMsg.e()
            return cachedIsStandalone == true
        }
    }

    internal fun resolveModuleApkPath(moduleApkPath: String, hookInstance: Any): String {
        if (moduleApkPath.isNotBlank() && File(moduleApkPath).exists()) {
            return moduleApkPath
        }
        val xpHelperPath = runCatching { top.sacz.xphelper.XpHelper.moduleApkPath }.getOrNull()
        if (!xpHelperPath.isNullOrBlank() && File(xpHelperPath).exists()) {
            return xpHelperPath
        }

        val cl = hookInstance.javaClass.classLoader
        if (cl != null) {
            val fromElements = findApkFromClassLoader(cl)
            if (!fromElements.isNullOrBlank() && File(fromElements).exists()) {
                return fromElements
            }

            val clStr = cl.toString()
            val match = Regex("""(/[^"':\s\n\r\t]+\.apk)""").find(clStr)
            if (match != null) {
                val candidate = match.groupValues[1]
                if (File(candidate).exists()) return candidate
            }
        }
        return moduleApkPath
    }

    private fun findApkFromClassLoader(cl: ClassLoader): String? {
        return runCatching {
            var currentCl: ClassLoader? = cl
            while (currentCl != null) {
                val pathListField = runCatching {
                    currentCl.javaClass.getDeclaredField("pathList")
                }.getOrNull() ?: runCatching {
                    currentCl.javaClass.superclass?.getDeclaredField("pathList")
                }.getOrNull()

                if (pathListField != null) {
                    pathListField.isAccessible = true
                    val pathList = pathListField.get(currentCl)
                    if (pathList != null) {
                        val elementsField = pathList.javaClass.getDeclaredField("dexElements")
                        elementsField.isAccessible = true
                        val elements = elementsField.get(pathList) as? Array<*>
                        if (elements != null) {
                            for (element in elements) {
                                if (element == null) continue
                                for (fieldName in listOf("path", "file")) {
                                    val f = runCatching {
                                        val field = element.javaClass.getDeclaredField(fieldName)
                                        field.isAccessible = true
                                        field.get(element) as? File
                                    }.getOrNull()
                                    if (f != null && f.name.endsWith(".apk", ignoreCase = true) && f.exists()) {
                                        return f.absolutePath
                                    }
                                }
                                val dexFile = runCatching {
                                    val field = element.javaClass.getDeclaredField("dexFile")
                                    field.isAccessible = true
                                    field.get(element)
                                }.getOrNull()
                                if (dexFile != null) {
                                    val getNameMethod = runCatching {
                                        dexFile.javaClass.getMethod("getName")
                                    }.getOrNull()
                                    val name = getNameMethod?.invoke(dexFile) as? String
                                    if (!name.isNullOrBlank() && name.endsWith(".apk", ignoreCase = true) && File(name).exists()) {
                                        return name
                                    }
                                }
                            }
                        }
                    }
                }
                currentCl = currentCl.parent
            }
            null
        }.getOrNull()
    }

    private fun readAllProjectAssets(zip: ZipFile): Map<String, ByteArray> {
        val map = LinkedHashMap<String, ByteArray>()
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory) continue
            val name = entry.name
            if (!name.startsWith(ASSET_PREFIX)) continue
            val relPath = name.removePrefix(ASSET_PREFIX).trimStart('/')
            if (relPath.isBlank() || relPath.contains("..")) continue
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            map[relPath] = bytes
        }
        return map
    }

    /**
     * 规范化独立项目基准目录（始终以 `/` 结尾）。
     */
    internal fun normalizeBaseDir(projectName: String, absProjectDir: String): String {
        return if (absProjectDir.isNotEmpty()) {
            if (absProjectDir.endsWith("/")) absProjectDir else "$absProjectDir/"
        } else {
            "/data/user/0/luahook_standalone/$projectName/"
        }
    }

    /**
     * 统一路径解析器：将相对路径或硬编码的 `/data/local/tmp/LuaHook/Project/<name>/` 重定向至独立项目目录。
     */
    internal fun resolveProjectFilePath(
        rawPath: String,
        normalizedBaseDir: String,
        absProjectDir: String = normalizedBaseDir.trimEnd('/')
    ): String {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) return normalizedBaseDir

        if (trimmed.startsWith(normalizedBaseDir)) {
            return trimmed
        }
        if (absProjectDir.isNotEmpty() && trimmed == absProjectDir) {
            return absProjectDir
        }

        // 兼容作者在脚本中硬编码 /data/local/tmp/LuaHook/Project/<projectName>/xxx 的情况
        if (trimmed.startsWith(WORKSPACE_PROJECT_PREFIX)) {
            val afterPrefix = trimmed.removePrefix(WORKSPACE_PROJECT_PREFIX)
            val slashIdx = afterPrefix.indexOf('/')
            val rel = if (slashIdx >= 0) afterPrefix.substring(slashIdx + 1) else ""
            return normalizedBaseDir + rel.trimStart('/')
        }

        // 相对路径（如 "utils.lua" 或 "./sub/a.lua"）自动拼接到当前独立项目目录
        if (!trimmed.startsWith("/")) {
            val cleanRel = trimmed.removePrefix("./").trimStart('/')
            return normalizedBaseDir + cleanRel
        }

        return trimmed
    }

    /**
     * 从解析后的路径提取项目内相对路径（用于内存 assets 兜底查找）。
     */
    internal fun extractRelativeAssetKey(
        resolvedOrRawPath: String,
        normalizedBaseDir: String,
        absProjectDir: String = normalizedBaseDir.trimEnd('/')
    ): String? {
        val resolved = resolveProjectFilePath(resolvedOrRawPath, normalizedBaseDir, absProjectDir)
        if (resolved.startsWith(normalizedBaseDir)) {
            return resolved.removePrefix(normalizedBaseDir).trimStart('/').takeIf { it.isNotEmpty() }
        }
        if (!resolvedOrRawPath.startsWith("/")) {
            return resolvedOrRawPath.trim().removePrefix("./").trimStart('/').takeIf { it.isNotEmpty() }
        }
        return null
    }

    /**
     * 为 `require(modName)` 生成候选相对文件路径集合（支持点号分隔、斜杠分隔、带 `.lua` 后缀及 `init.lua`）。
     */
    internal fun buildRequireCandidatePaths(
        modName: String,
        normalizedBaseDir: String,
        absProjectDir: String = normalizedBaseDir.trimEnd('/')
    ): Set<String> {
        val candidates = linkedSetOf<String>()
        val cleaned = extractRelativeAssetKey(modName, normalizedBaseDir, absProjectDir)
            ?: modName.trim().removePrefix("./").trimStart('/')
        val withoutLuaExt = if (cleaned.endsWith(".lua", ignoreCase = true)) {
            cleaned.substring(0, cleaned.length - 4)
        } else {
            cleaned
        }
        val slashForm = withoutLuaExt.replace('.', '/')
        candidates.add("$slashForm.lua")
        candidates.add("$slashForm/init.lua")
        candidates.add("$withoutLuaExt.lua")
        candidates.add("$withoutLuaExt/init.lua")
        if (cleaned.endsWith(".lua", ignoreCase = true)) {
            candidates.add(cleaned)
        }
        return candidates
    }

    /**
     * 读取项目脚本源码（优先磁盘文件，其次内存 assets 兜底）。
     */
    internal fun loadProjectScriptSource(
        pathOrRel: String,
        normalizedBaseDir: String,
        absProjectDir: String,
        projectAssets: Map<String, ByteArray>
    ): Pair<String, String>? {
        val resolvedPath = resolveProjectFilePath(pathOrRel, normalizedBaseDir, absProjectDir)
        val diskFile = File(resolvedPath)
        if (diskFile.isFile && diskFile.canRead()) {
            val text = runCatching { diskFile.readText(StandardCharsets.UTF_8) }.getOrNull()
            if (text != null) return text to resolvedPath
        }
        val relKey = extractRelativeAssetKey(pathOrRel, normalizedBaseDir, absProjectDir)
        if (relKey != null) {
            val assetBytes = projectAssets[relKey]
            if (assetBytes != null) {
                return assetBytes.toString(StandardCharsets.UTF_8) to relKey
            }
        }
        return null
    }

    /**
     * 配置独立模块 Lua 全局环境：
     * - 注册 `getProjectDir` / `getProjectPath` / `projectDir`
     * - 适配 `require`（支持 `package.path`、子目录模块、`.lua` 后缀兼容及 APK 内存直接加载兜底）
     * - 适配 `dofile` / `loadfile`（支持相对路径、`getProjectDir` 路径、硬编码工作区路径重定向与内存兜底）
     * - 适配 `loadDex`、`file.*`、`io.open`、`loadDrawableFromFile`
     */
    internal fun configureStandaloneLuaEnv(
        globals: Globals,
        projectName: String,
        absProjectDir: String,
        projectAssets: Map<String, ByteArray>
    ) {
        runCatching { globals.registerLayout() }
        runCatching { globals.registerDexKit() }
        runCatching { globals.registerNative() }

        val normalizedBaseDir = normalizeBaseDir(projectName, absProjectDir)

        LuaProject(projectName, normalizedBaseDir).registerTo(globals)

        fun resolvePath(rawPath: String): String =
            resolveProjectFilePath(rawPath, normalizedBaseDir, absProjectDir)

        fun extractRelKey(rawPath: String): String? =
            extractRelativeAssetKey(rawPath, normalizedBaseDir, absProjectDir)

        fun loadSource(pathOrRel: String): Pair<String, String>? =
            loadProjectScriptSource(pathOrRel, normalizedBaseDir, absProjectDir, projectAssets)

        // 1. 配置 package.path
        val pkgVal = globals.get("package")
        if (pkgVal.istable()) {
            val currentPath = pkgVal.get("path").optjstring("")
            val extraPaths = "${normalizedBaseDir}?.lua;${normalizedBaseDir}?/init.lua;${normalizedBaseDir}?.luac"
            pkgVal.set("path", LuaValue.valueOf(if (currentPath.isNotEmpty()) "$extraPaths;$currentPath" else extraPaths))
        }

        // 2. 增强 require：支持 "mod"、"sub.mod"、"sub/mod"、"mod.lua"、getProjectDir("mod.lua") 以及无磁盘内存加载
        val oldRequire = globals.get("require")
        globals.set("require", object : OneArgFunction() {
            override fun call(modArg: LuaValue): LuaValue {
                val modName = modArg.checkjstring()
                val pkg = globals.get("package")
                val loaded = if (pkg.istable()) pkg.get("loaded") else LuaValue.NIL
                if (loaded.istable()) {
                    val existing = loaded.get(modName)
                    if (!existing.isnil()) {
                        return existing
                    }
                }

                val candidates = buildRequireCandidatePaths(modName, normalizedBaseDir, absProjectDir)
                for (candidate in candidates) {
                    val sourcePair = loadSource(candidate) ?: continue
                    val (scriptSource, chunkLabel) = sourcePair
                    val chunk = globals.load(scriptSource, "@$chunkLabel")
                    val result = chunk.call(LuaValue.valueOf(modName))
                    val finalVal = if (result.isnil()) LuaValue.TRUE else result
                    if (loaded.istable()) {
                        loaded.set(modName, finalVal)
                    }
                    return finalVal
                }

                if (oldRequire.isfunction()) {
                    return oldRequire.call(modArg)
                }
                return error("module '$modName' not found in standalone project")
            }
        })

        // 3. 增强 loadfile 与 dofile
        val oldLoadFile = globals.get("loadfile")
        globals.set("loadfile", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() == 0 || args.arg1().isnil()) {
                    return if (oldLoadFile.isfunction()) oldLoadFile.invoke(args) else LuaValue.NIL
                }
                val rawPath = args.checkjstring(1)
                val sourcePair = loadSource(rawPath)
                if (sourcePair != null) {
                    val (scriptSource, chunkLabel) = sourcePair
                    return try {
                        globals.load(scriptSource, "@$chunkLabel")
                    } catch (e: Throwable) {
                        LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "loadfile error"))
                    }
                }
                if (oldLoadFile.isfunction()) {
                    val resolved = resolvePath(rawPath)
                    return oldLoadFile.invoke(LuaValue.varargsOf(LuaValue.valueOf(resolved), args.subargs(2)))
                }
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("cannot open $rawPath: No such file"))
            }
        })

        val oldDoFile = globals.get("dofile")
        globals.set("dofile", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() == 0 || args.arg1().isnil()) {
                    return if (oldDoFile.isfunction()) oldDoFile.invoke(args) else LuaValue.NIL
                }
                val rawPath = args.checkjstring(1)
                val sourcePair = loadSource(rawPath)
                if (sourcePair != null) {
                    val (scriptSource, chunkLabel) = sourcePair
                    val chunk = globals.load(scriptSource, "@$chunkLabel")
                    return chunk.invoke()
                }
                if (oldDoFile.isfunction()) {
                    val resolved = resolvePath(rawPath)
                    return oldDoFile.invoke(LuaValue.valueOf(resolved))
                }
                return error("cannot open $rawPath: No such file")
            }
        })

        // 4. 增强 loadDex：支持相对路径、工作区路径重定向，并在找不到磁盘文件时按需从内存释放只读 DEX
        val oldLoadDex = globals.get("loadDex")
        if (oldLoadDex.isfunction()) {
            globals.set("loadDex", object : OneArgFunction() {
                override fun call(pathArg: LuaValue): LuaValue {
                    val rawPath = pathArg.checkjstring()
                    val resolved = resolvePath(rawPath)
                    val dexFile = File(resolved)
                    if (!dexFile.exists()) {
                        val relKey = extractRelKey(rawPath)
                        val bytes = relKey?.let { projectAssets[it] }
                        if (bytes != null) {
                            runCatching {
                                dexFile.parentFile?.mkdirs()
                                if (dexFile.exists()) dexFile.setWritable(true)
                                dexFile.writeBytes(bytes)
                                dexFile.setReadOnly()
                            }
                        }
                    } else {
                        // Android 14+ 要求动态加载的 DEX 必须为只读
                        runCatching { dexFile.setReadOnly() }
                    }
                    return oldLoadDex.call(LuaValue.valueOf(dexFile.absolutePath))
                }
            })
        }

        // 5. 增强 file.* 表（LuaFile API）与内存只读兜底
        val fileTable = globals.get("file")
        if (fileTable.istable()) {
            wrapLuaFileTable(
                fileTable = fileTable.checktable(),
                projectAssets = projectAssets,
                resolvePath = ::resolvePath,
                extractRelKey = ::extractRelKey
            )
        }

        // 6. 增强 io.open
        val ioTable = globals.get("io")
        if (ioTable.istable()) {
            val oldIoOpen = ioTable.get("open")
            if (oldIoOpen.isfunction()) {
                ioTable.set("open", object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        if (args.narg() >= 1 && args.arg1().isstring()) {
                            val resolved = resolvePath(args.checkjstring(1))
                            return oldIoOpen.invoke(
                                LuaValue.varargsOf(LuaValue.valueOf(resolved), args.subargs(2))
                            )
                        }
                        return oldIoOpen.invoke(args)
                    }
                })
            }
        }

        // 7. 增强 loadDrawableFromFile
        val oldLoadDrawable = globals.get("loadDrawableFromFile")
        if (oldLoadDrawable.isfunction()) {
            globals.set("loadDrawableFromFile", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    if (args.narg() >= 1 && args.arg1().isstring()) {
                        val resolved = resolvePath(args.checkjstring(1))
                        return oldLoadDrawable.invoke(
                            LuaValue.varargsOf(LuaValue.valueOf(resolved), args.subargs(2))
                        )
                    }
                    return oldLoadDrawable.invoke(args)
                }
            })
        }
    }

    private fun wrapLuaFileTable(
        fileTable: LuaTable,
        projectAssets: Map<String, ByteArray>,
        resolvePath: (String) -> String,
        extractRelKey: (String) -> String?
    ) {
        val oldIsFile = fileTable.get("isFile")
        if (oldIsFile.isfunction()) {
            fileTable.set("isFile", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    val raw = arg.checkjstring()
                    val res = oldIsFile.call(LuaValue.valueOf(resolvePath(raw)))
                    if (res.toboolean()) return res
                    val rel = extractRelKey(raw)
                    return LuaValue.valueOf(rel != null && projectAssets.containsKey(rel))
                }
            })
        }

        val oldIsExists = fileTable.get("isExists")
        if (oldIsExists.isfunction()) {
            fileTable.set("isExists", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    val raw = arg.checkjstring()
                    val res = oldIsExists.call(LuaValue.valueOf(resolvePath(raw)))
                    if (res.toboolean()) return res
                    val rel = extractRelKey(raw)
                    if (rel != null) {
                        val prefix = "${rel.trimEnd('/')}/"
                        val existsInAssets = projectAssets.containsKey(rel) ||
                            projectAssets.keys.any { it.startsWith(prefix) }
                        return LuaValue.valueOf(existsInAssets)
                    }
                    return res
                }
            })
        }

        val oldRead = fileTable.get("read")
        if (oldRead.isfunction()) {
            fileTable.set("read", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    val raw = arg.checkjstring()
                    val resolved = resolvePath(raw)
                    if (File(resolved).isFile) {
                        return oldRead.call(LuaValue.valueOf(resolved))
                    }
                    val rel = extractRelKey(raw)
                    val bytes = rel?.let { projectAssets[it] }
                    if (bytes != null) {
                        return LuaValue.valueOf(bytes.toString(StandardCharsets.UTF_8))
                    }
                    return oldRead.call(LuaValue.valueOf(resolved))
                }
            })
        }

        val oldReadBytes = fileTable.get("readBytes")
        if (oldReadBytes.isfunction()) {
            fileTable.set("readBytes", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    val raw = arg.checkjstring()
                    val resolved = resolvePath(raw)
                    if (File(resolved).isFile) {
                        return oldReadBytes.call(LuaValue.valueOf(resolved))
                    }
                    val rel = extractRelKey(raw)
                    val bytes = rel?.let { projectAssets[it] }
                    if (bytes != null) {
                        return LuaValue.valueOf(bytes)
                    }
                    return oldReadBytes.call(LuaValue.valueOf(resolved))
                }
            })
        }

        // 单路径参数方法包装
        for (fnName in listOf("isDir", "delete", "getName", "getSize")) {
            val oldFn = fileTable.get(fnName)
            if (oldFn.isfunction()) {
                fileTable.set(fnName, object : OneArgFunction() {
                    override fun call(arg: LuaValue): LuaValue {
                        return oldFn.call(LuaValue.valueOf(resolvePath(arg.checkjstring())))
                    }
                })
            }
        }

        // 路径 + 内容 / 双路径参数方法包装
        for (fnName in listOf("write", "writeBytes", "append", "appendBytes", "rename")) {
            val oldFn = fileTable.get(fnName)
            if (oldFn.isfunction()) {
                fileTable.set(fnName, object : TwoArgFunction() {
                    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                        val resolved = resolvePath(arg1.checkjstring())
                        File(resolved).parentFile?.mkdirs()
                        return oldFn.call(LuaValue.valueOf(resolved), arg2)
                    }
                })
            }
        }

        for (fnName in listOf("copy", "move")) {
            val oldFn = fileTable.get(fnName)
            if (oldFn.isfunction()) {
                fileTable.set(fnName, object : TwoArgFunction() {
                    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                        val src = resolvePath(arg1.checkjstring())
                        val dst = resolvePath(arg2.checkjstring())
                        File(dst).parentFile?.mkdirs()
                        return oldFn.call(LuaValue.valueOf(src), LuaValue.valueOf(dst))
                    }
                })
            }
        }
    }

    private fun extractProjectAssetsIfNeeded(
        projectAssets: Map<String, ByteArray>,
        apkFile: File,
        targetPackageName: String,
        targetDataDir: String?,
        projectName: String,
        modulePackageName: String = ""
    ): File? {
        val safeName = projectName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "module" }
        val expectedMarker = "${apkFile.length()}_${apkFile.lastModified()}"

        val candidateDirs = buildList {
            if (!targetDataDir.isNullOrBlank()) {
                add(File(targetDataDir, "files/luahook_project/$safeName"))
                add(File(targetDataDir, "cache/luahook_project/$safeName"))
            }
            add(File("/data/user/0/$targetPackageName/files/luahook_project/$safeName"))
            add(File("/data/data/$targetPackageName/files/luahook_project/$safeName"))
            add(File("/data/user/0/$targetPackageName/cache/luahook_project/$safeName"))
            add(File("/data/data/$targetPackageName/cache/luahook_project/$safeName"))
            if (modulePackageName.isNotBlank()) {
                add(File("/data/user/0/$modulePackageName/files/project"))
                add(File("/data/data/$modulePackageName/files/project"))
            }
            val tmpRoot = System.getProperty("java.io.tmpdir") ?: "/data/local/tmp"
            add(File(tmpRoot, "luahook_project_${targetPackageName}_$safeName"))
        }

        for (targetDir in candidateDirs) {
            try {
                val markerFile = File(targetDir, ".apk_version")
                val mainFile = File(targetDir, "main.lua")

                if (targetDir.exists() &&
                    mainFile.exists() &&
                    markerFile.exists() &&
                    runCatching { markerFile.readText().trim() }.getOrNull() == expectedMarker
                ) {
                    return targetDir
                }

                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    continue
                }

                for ((relPath, bytes) in projectAssets) {
                    if (relPath == "pack_config.json") continue
                    val outFile = File(targetDir, relPath)
                    outFile.parentFile?.mkdirs()
                    if (outFile.exists()) {
                        outFile.setWritable(true)
                    }
                    outFile.writeBytes(bytes)
                    // Android 14+ 强制要求动态加载的 DEX/JAR/APK 文件只读
                    if (relPath.endsWith(".dex", ignoreCase = true) ||
                        relPath.endsWith(".jar", ignoreCase = true) ||
                        relPath.endsWith(".apk", ignoreCase = true)
                    ) {
                        outFile.setReadOnly()
                    }
                }

                markerFile.writeText(expectedMarker)
                if (mainFile.exists()) {
                    return targetDir
                }
            } catch (_: Throwable) {
                // 当前候选目录不可写，继续尝试下一个候选目录
            }
        }
        return null
    }
}
