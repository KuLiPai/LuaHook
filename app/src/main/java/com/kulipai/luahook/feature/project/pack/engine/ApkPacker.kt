package com.kulipai.luahook.feature.project.pack.engine

import com.kulipai.luahook.feature.project.pack.engine.axml.ManifestConfig
import com.kulipai.luahook.feature.project.pack.engine.axml.ManifestEditor
import com.kulipai.luahook.feature.project.pack.engine.signer.ApkSignerHelper
import com.kulipai.luahook.feature.project.pack.engine.signer.SignConfig
import com.kulipai.luahook.feature.project.pack.engine.signer.ZipAligner
import com.kulipai.luahook.feature.project.pack.engine.slimmer.ArscBuilder
import com.kulipai.luahook.feature.project.pack.engine.slimmer.DexSlimmer
import com.kulipai.luahook.hook.entry.StandaloneHookRunner
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

enum class PackStage {
    PREPARE,
    INJECT_PROJECT,
    REWRITE_MANIFEST,
    ZIPALIGN,
    SIGN,
    COMPLETE
}

data class PackRequest(
    val projectName: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val description: String,
    val author: String,
    val scopes: List<String>,
    val showLauncherIcon: Boolean = true,
    val customIconPngBytes: ByteArray? = null,
    val customIconResId: Int? = null,
    val customIconEntryPath: String? = null,
    val abiFilter: Set<String> = setOf("arm64-v8a", "armeabi-v7a"),
    val signConfig: SignConfig = SignConfig(),
    val baseApkFile: File,
    val projectFiles: Map<String, ByteArray>,
    val workDir: File,
    val outputApkFile: File
)

data class PackResult(
    val apkFile: File,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val fileSizeBytes: Long,
    val sha256: String
)

/**
 * 独立模块 APK 打包流水线调度器。
 *
 * 采用深度精简策略：
 * 1. 通过 [DexSlimmer] 剔除纯 UI 分包并裁剪冗余 IDE/UI 类字节码，将 DEX 以 `DEFLATED` 高压缩写入；
 * 2. 通过 [ArscBuilder] 生成仅 556 字节的极简 `resources.arsc` 与单一 `res/icon.png`，彻底剥离原包 1.4MB 资源表与上千个 UI 资源文件；
 * 3. 通过 [ManifestEditor] 裁剪非必要四大组件与敏感权限，并重写 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 解决共存安装冲突。
 */
object ApkPacker {

    // 1x1 透明兜底 PNG（供单元测试或无图标环境兜底）
    private val FALLBACK_ICON_PNG: ByteArray by lazy {
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        )
    }

    suspend fun pack(
        request: PackRequest,
        onProgress: suspend (stage: PackStage, percent: Int) -> Unit = { _, _ -> }
    ): PackResult {
        onProgress(PackStage.PREPARE, 10)

        require(request.baseApkFile.exists() && request.baseApkFile.isFile) {
            "Base template APK not found: ${request.baseApkFile.absolutePath}"
        }
        require(request.projectFiles.containsKey("init.lua") || request.projectFiles.containsKey("main.lua")) {
            "Project must contain init.lua or main.lua"
        }

        request.workDir.mkdirs()
        val rawUnsignedFile = File(request.workDir, "unsigned_raw.apk")
        val alignedFile = File(request.workDir, "aligned.apk")

        try {
            onProgress(PackStage.INJECT_PROJECT, 30)
            buildModifiedUnsignedApk(request, rawUnsignedFile) {
                onProgress(PackStage.REWRITE_MANIFEST, 55)
            }

            onProgress(PackStage.ZIPALIGN, 75)
            ZipAligner.alignZip(rawUnsignedFile, alignedFile)

            onProgress(PackStage.SIGN, 90)
            ApkSignerHelper.signApk(alignedFile, request.outputApkFile, request.signConfig)

            onProgress(PackStage.COMPLETE, 100)
            val sha256 = computeSha256(request.outputApkFile)

            return PackResult(
                apkFile = request.outputApkFile,
                appName = request.appName,
                packageName = request.packageName,
                versionName = request.versionName,
                versionCode = request.versionCode,
                fileSizeBytes = request.outputApkFile.length(),
                sha256 = sha256
            )
        } finally {
            rawUnsignedFile.delete()
            alignedFile.delete()
        }
    }

    private suspend fun buildModifiedUnsignedApk(
        request: PackRequest,
        outFile: File,
        onManifestRewrite: suspend () -> Unit
    ) {
        val manifestConfig = ManifestConfig(
            appName = request.appName,
            packageName = request.packageName,
            versionName = request.versionName,
            versionCode = request.versionCode,
            description = request.description,
            showLauncherIcon = request.showLauncherIcon,
            customIconResId = ArscBuilder.ICON_RESOURCE_ID,
            standaloneSlimMode = true
        )

        var scopeWritten = false
        val scopeListContent = if (request.scopes.contains("all")) {
            ""
        } else {
            request.scopes.filter { it.isNotBlank() }.joinToString("\n")
        }

        ZipFile(request.baseApkFile).use { zip ->
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile), 65536)).use { zos ->
                val dexEntries = mutableListOf<ZipEntry>()
                val entries = zip.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if (entry.isDirectory) continue
                    if (isDexFileName(name)) {
                        dexEntries.add(entry)
                        continue
                    }
                    if (shouldExcludeFromStandaloneApk(name, request.abiFilter)) continue

                    when {
                        name == "AndroidManifest.xml" -> {
                            onManifestRewrite()
                            val rawManifest = zip.getInputStream(entry).use { it.readBytes() }
                            val modifiedManifest = ManifestEditor.modifyManifest(rawManifest, manifestConfig)
                            writeDeflatedEntry(zos, name, modifiedManifest)
                        }

                        name == "META-INF/xposed/scope.list" -> {
                            writeDeflatedEntry(zos, name, scopeListContent.toByteArray(Charsets.UTF_8))
                            scopeWritten = true
                        }

                        name.startsWith("lib/") && name.endsWith(".so") -> {
                            val bytes = zip.getInputStream(entry).use { it.readBytes() }
                            writeStoredEntry(zos, name, bytes)
                        }

                        else -> {
                            val bytes = zip.getInputStream(entry).use { it.readBytes() }
                            writeDeflatedEntry(zos, name, bytes)
                        }
                    }
                }

                // 1. 筛选并按顺序重编号写入 classes*.dex（将包含 MainHook 入口的 DEX 置于首位 classes.dex，使用 DEFLATED 高压缩比存储）
                dexEntries.sortBy { dexIndex(it.name) }
                val keptDexList = mutableListOf<ByteArray>()
                for (dexEntry in dexEntries) {
                    val rawDex = zip.getInputStream(dexEntry).use { it.readBytes() }
                    val slimDex = DexSlimmer.slimDexOrNull(rawDex) ?: continue
                    if (DexSlimmer.containsHookEntry(slimDex)) {
                        keptDexList.add(0, slimDex)
                    } else {
                        keptDexList.add(slimDex)
                    }
                }
                for ((idx, dexBytes) in keptDexList.withIndex()) {
                    val dexNum = idx + 1
                    val outDexName = if (dexNum == 1) "classes.dex" else "classes$dexNum.dex"
                    writeDeflatedEntry(zos, outDexName, dexBytes)
                }

                // 2. 写入极简 resources.arsc（STORED，供 ZipAligner 4 字节对齐）与单一图标 res/icon.png
                val minimalArsc = ArscBuilder.buildMinimalArsc(
                    packageName = request.packageName,
                    iconPath = ArscBuilder.ICON_ENTRY_PATH
                )
                writeStoredEntry(zos, "resources.arsc", minimalArsc)

                val iconBytes = request.customIconPngBytes ?: FALLBACK_ICON_PNG
                writeStoredEntry(zos, ArscBuilder.ICON_ENTRY_PATH, iconBytes)

                // 3. 写入独立模块专属 Hook 入口 assets/xposed_init（指向 StandaloneMainHook，彻底脱离工作区）
                writeDeflatedEntry(
                    zos,
                    "assets/xposed_init",
                    "com.kulipai.luahook.hook.entry.StandaloneMainHook\n".toByteArray(Charsets.UTF_8)
                )

                // 4. 确保 META-INF/xposed/scope.list 写入
                if (!scopeWritten) {
                    writeDeflatedEntry(
                        zos,
                        "META-INF/xposed/scope.list",
                        scopeListContent.toByteArray(Charsets.UTF_8)
                    )
                }

                // 5. 注入项目文件到 assets/standalone_project/
                val sortedProjectEntries = request.projectFiles.entries.sortedBy { it.key }
                for ((relPath, contentBytes) in sortedProjectEntries) {
                    val cleanRel = relPath.trimStart('/').replace('\\', '/')
                    if (cleanRel.isBlank() || cleanRel.contains("..")) continue
                    val entryPath = StandaloneHookRunner.ASSET_PREFIX + cleanRel
                    writeDeflatedEntry(zos, entryPath, contentBytes)
                }

                // 6. 注入打包元信息配置 assets/standalone_project/pack_config.json
                val packConfigJson = JSONObject().apply {
                    put("projectName", request.projectName)
                    put("appName", request.appName)
                    put("packageName", request.packageName)
                    put("versionName", request.versionName)
                    put("versionCode", request.versionCode)
                    put("description", request.description)
                    put("author", request.author)
                    put("showLauncherIcon", request.showLauncherIcon)
                    put("scopes", JSONArray(request.scopes))
                    put("buildTime", System.currentTimeMillis())
                }.toString(2)

                writeDeflatedEntry(
                    zos,
                    StandaloneHookRunner.PACK_CONFIG_ENTRY,
                    packConfigJson.toByteArray(Charsets.UTF_8)
                )
            }
        }
    }

    private fun isDexFileName(entryName: String): Boolean {
        return !entryName.contains('/') && entryName.startsWith("classes") && entryName.endsWith(".dex")
    }

    private fun dexIndex(entryName: String): Int {
        val middle = entryName.removePrefix("classes").removeSuffix(".dex")
        return if (middle.isEmpty()) 1 else middle.toIntOrNull() ?: 999
    }

    private fun shouldExcludeFromStandaloneApk(entryName: String, abiFilter: Set<String>): Boolean {
        // 剥离原包庞大的 resources.arsc 与所有 res/**（由极简 ArscBuilder + res/icon.png 替代）
        if (entryName == "resources.arsc" || entryName.startsWith("res/")) {
            return true
        }

        // 剥离 JCodings 编码表、Kotlin 编译器元数据及调试探针
        if (entryName.startsWith("tables/") ||
            entryName.startsWith("kotlin/") ||
            entryName.startsWith("okhttp3/") ||
            entryName == "DebugProbesKt.bin"
        ) {
            return true
        }

        // 仅保留 META-INF/xposed/**，剥离旧签名与所有库版本元数据
        // 特别注意：必须剔除 META-INF/xposed/java_init.list，防止 LSPosed 误识别为 LibXposed API 102 模块而忽略 assets/xposed_init
        if (entryName.startsWith("META-INF/")) {
            if (entryName == "META-INF/xposed/java_init.list") {
                return true
            }
            return !entryName.startsWith("META-INF/xposed/")
        }

        // assets/ 下由打包引擎统一写入专属 assets/xposed_init 与 assets/standalone_project/**，剥离原包 assets
        if (entryName.startsWith("assets/")) {
            return true
        }

        // 按所选 ABI 过滤 Native 库
        if (entryName.startsWith("lib/") && abiFilter.isNotEmpty()) {
            val parts = entryName.split('/')
            if (parts.size >= 3) {
                val abi = parts[1]
                if (abi !in abiFilter) return true
            }
        }

        return false
    }

    private fun writeStoredEntry(zos: ZipOutputStream, name: String, bytes: ByteArray) {
        val crc = CRC32().apply { update(bytes) }.value
        val entry = ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc
        }
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun writeDeflatedEntry(zos: ZipOutputStream, name: String, bytes: ByteArray) {
        val entry = ZipEntry(name).apply {
            method = ZipEntry.DEFLATED
        }
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun computeSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(16384)
            var r: Int
            while (fis.read(buf).also { r = it } != -1) {
                md.update(buf, 0, r)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
