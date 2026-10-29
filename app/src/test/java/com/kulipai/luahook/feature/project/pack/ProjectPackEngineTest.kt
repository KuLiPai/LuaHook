package com.kulipai.luahook.feature.project.pack

import com.kulipai.luahook.feature.project.pack.engine.ApkPacker
import com.kulipai.luahook.feature.project.pack.engine.PackRequest
import com.kulipai.luahook.feature.project.pack.engine.axml.ManifestConfig
import com.kulipai.luahook.feature.project.pack.engine.axml.ManifestEditor
import com.kulipai.luahook.feature.project.pack.engine.signer.ApkSignerHelper
import com.kulipai.luahook.feature.project.pack.engine.signer.SignConfig
import com.kulipai.luahook.feature.project.pack.engine.signer.TestKeys
import com.kulipai.luahook.feature.project.pack.engine.signer.ZipAligner
import com.kulipai.luahook.feature.project.pack.engine.slimmer.ArscBuilder
import com.kulipai.luahook.feature.project.pack.engine.slimmer.DexSlimmer
import com.kulipai.luahook.hook.entry.StandaloneHookRunner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ProjectPackEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `PackageNameValidator validates and suggests valid package names`() {
        assertTrue(PackageNameValidator.isValid("com.example.myhook"))
        assertTrue(PackageNameValidator.isValid("io.kulipai.luahook.mod_1"))
        assertFalse(PackageNameValidator.isValid("singleword"))
        assertFalse(PackageNameValidator.isValid("com.123invalid.app"))
        assertFalse(PackageNameValidator.isValid("com..empty"))
        assertFalse(PackageNameValidator.isValid(""))

        val asciiSuggested = PackageNameValidator.suggestPackageName("WeChatHook")
        assertEquals("com.luahook.project.wechathook", asciiSuggested)
        assertTrue(PackageNameValidator.isValid(asciiSuggested))

        val cnSuggested = PackageNameValidator.suggestPackageName("微信防撤回")
        assertTrue(PackageNameValidator.isValid(cnSuggested))
    }

    @Test
    fun `ManifestEditor rewrites permissions, package, version, label, description, authorities and strips IDE components`() {
        val sampleAxml = buildSampleManifestAxml()

        val configWithIcon = ManifestConfig(
            appName = "微信防撤回独立版",
            packageName = "com.example.wxrecall",
            versionName = "2.5.1",
            versionCode = 18,
            description = "独立打包测试模块描述",
            showLauncherIcon = true,
            standaloneSlimMode = true
        )
        val modifiedBytes = ManifestEditor.modifyManifest(sampleAxml, configWithIcon)
        val summary = ManifestEditor.parseSummary(modifiedBytes)

        assertEquals("com.example.wxrecall", summary.packageName)
        assertEquals(18, summary.versionCode)
        assertEquals("2.5.1", summary.versionName)
        assertEquals("微信防撤回独立版", summary.appLabel)
        assertEquals("独立打包测试模块描述", summary.xposedDescription)
        assertEquals("android.app.Application", summary.applicationName)
        assertTrue(summary.authorities.contains("com.example.wxrecall.XposedService"))
        assertTrue(
            "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION must be rewritten to new package",
            summary.permissions.contains("com.example.wxrecall.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        )
        assertFalse(
            "Old permission must not remain",
            summary.permissions.contains("com.kulipai.luahook.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        )
        assertTrue(summary.hasLauncherCategory)

        // 测试隐藏桌面图标（LAUNCHER -> INFO）
        val configNoLauncher = configWithIcon.copy(showLauncherIcon = false)
        val hiddenLauncherBytes = ManifestEditor.modifyManifest(sampleAxml, configNoLauncher)
        val hiddenSummary = ManifestEditor.parseSummary(hiddenLauncherBytes)
        assertFalse(hiddenSummary.hasLauncherCategory)

        // 若本地存在真实编译的 APK，额外使用真实 aapt2 产出的 AndroidManifest.xml 验证无损重写与组件剥离
        val realApk = listOf(
            File("build/outputs/apk/debug/app-debug.apk"),
            File("app/build/outputs/apk/debug/app-debug.apk"),
            File("build/outputs/apk/release/app-release-unsigned.apk"),
            File("app/build/outputs/apk/release/app-release-unsigned.apk")
        ).firstOrNull { it.exists() && it.isFile }

        if (realApk != null) {
            ZipFile(realApk).use { zip ->
                val realManifest = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() }
                val rewrittenReal = ManifestEditor.modifyManifest(realManifest, configWithIcon)
                val realSummary = ManifestEditor.parseSummary(rewrittenReal)
                assertEquals("com.example.wxrecall", realSummary.packageName)
                assertEquals(18, realSummary.versionCode)
                assertEquals("2.5.1", realSummary.versionName)
                assertEquals("微信防撤回独立版", realSummary.appLabel)
                assertEquals("独立打包测试模块描述", realSummary.xposedDescription)
                assertEquals("android.app.Application", realSummary.applicationName)
                assertEquals(listOf("com.example.wxrecall.XposedService"), realSummary.authorities)
                assertTrue(
                    realSummary.permissions.contains("com.example.wxrecall.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
                )
                assertTrue(
                    realSummary.usesPermissions.contains("com.example.wxrecall.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
                )
                assertFalse(
                    realSummary.usesPermissions.contains("moe.shizuku.manager.permission.API_V23")
                )
                assertEquals(
                    listOf("com.kulipai.luahook.hook.activity.StandaloneActivity"),
                    realSummary.activities
                )
            }
        }
    }

    @Test
    fun `ApkPacker slims real APK down to minimal standalone module`() = runTest {
        val realApkCandidates = listOf(
            File("build/outputs/apk/release/app-release-unsigned.apk"),
            File("app/build/outputs/apk/release/app-release-unsigned.apk"),
            File("build/outputs/apk/debug/app-debug.apk"),
            File("app/build/outputs/apk/debug/app-debug.apk")
        ).filter { it.exists() && it.isFile }

        for (realApk in realApkCandidates) {
            val workDir = tempFolder.newFolder("work_${realApk.name}")
            val outApk = File(tempFolder.root, "packed_${realApk.name}")
            val res = ApkPacker.pack(
                PackRequest(
                    projectName = "Frida0x1",
                    appName = "Frida0x1",
                    packageName = "com.luahook.project.frida0x1",
                    versionName = "1.0.0",
                    versionCode = 1,
                    description = "Standalone Module",
                    author = "Test",
                    scopes = listOf("com.tencent.mm"),
                    showLauncherIcon = true,
                    abiFilter = setOf("arm64-v8a"),
                    baseApkFile = realApk,
                    projectFiles = mapOf(
                        "init.lua" to "name='Frida0x1'".toByteArray(),
                        "main.lua" to "print('ok')".toByteArray()
                    ),
                    workDir = workDir,
                    outputApkFile = outApk
                )
            )
            val verify = ApkSignerHelper.verifyApk(res.apkFile)
            assertTrue("Verification failed for ${realApk.name}: ${verify.errors}", verify.isVerified)
            println(
                "Packed from ${realApk.name} (${realApk.length() / 1024} KB) -> " +
                    "${res.apkFile.length() / 1024} KB"
            )
        }
    }

    @Test
    fun `ZipAligner aligns stored resources to 4 bytes and native so libraries to 16KB`() {
        val unalignedZip = tempFolder.newFile("unaligned.apk")
        val alignedZip = tempFolder.newFile("aligned.apk")

        ZipOutputStream(FileOutputStream(unalignedZip)).use { zos ->
            val arscBytes = ArscBuilder.buildMinimalArsc("com.example.align")
            val arscEntry = ZipEntry("resources.arsc").apply {
                method = ZipEntry.STORED
                size = arscBytes.size.toLong()
                compressedSize = arscBytes.size.toLong()
                crc = CRC32().apply { update(arscBytes) }.value
            }
            zos.putNextEntry(arscEntry)
            zos.write(arscBytes)
            zos.closeEntry()

            val soBytes = ByteArray(1024) { 0x7F }
            val soEntry = ZipEntry("lib/arm64-v8a/libluahook.so").apply {
                method = ZipEntry.STORED
                size = soBytes.size.toLong()
                compressedSize = soBytes.size.toLong()
                crc = CRC32().apply { update(soBytes) }.value
            }
            zos.putNextEntry(soEntry)
            zos.write(soBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("dex_code_payload".toByteArray())
            zos.closeEntry()
        }

        ZipAligner.alignZip(unalignedZip, alignedZip)
        assertTrue("Aligned APK must pass ZipAligner verification", ZipAligner.verifyAlignment(alignedZip))
    }

    @Test
    fun `ApkPacker end-to-end builds, slims, aligns and signs standalone APK with V1 V2 V3 schemes`() = runTest {
        val baseApk = tempFolder.newFile("base_template.apk")
        createSyntheticBaseApk(baseApk)

        val workDir = tempFolder.newFolder("pack_work")
        val outputApk = File(tempFolder.root, "output_signed.apk")

        val projectFiles = mapOf(
            "init.lua" to """
                name = "DemoProject"
                description = "Demo Standalone Description"
                author = "KuLiPai"
                scope = { "com.tencent.mm" }
            """.trimIndent().toByteArray(),
            "main.lua" to """
                print("Hello from standalone DemoProject!")
            """.trimIndent().toByteArray(),
            "utils/helper.lua" to "return { version = 1 }".toByteArray()
        )

        val request = PackRequest(
            projectName = "DemoProject",
            appName = "Demo Module",
            packageName = "com.example.demomodule",
            versionName = "1.2.0",
            versionCode = 5,
            description = "Custom Xposed Description",
            author = "KuLiPai",
            scopes = listOf("com.tencent.mm", "com.eg.android.AlipayGphone"),
            showLauncherIcon = true,
            abiFilter = setOf("arm64-v8a"),
            signConfig = SignConfig(
                useCustomKeystore = false,
                v1SigningEnabled = true,
                v2SigningEnabled = true,
                v3SigningEnabled = true
            ),
            baseApkFile = baseApk,
            projectFiles = projectFiles,
            workDir = workDir,
            outputApkFile = outputApk
        )

        val result = ApkPacker.pack(request)
        assertTrue(result.apkFile.exists())
        assertTrue(result.fileSizeBytes > 0)
        assertEquals(64, result.sha256.length)

        // 1. 验证 V1 + V2 + V3 数字签名有效性
        val verifyResult = ApkSignerHelper.verifyApk(result.apkFile)
        assertTrue("APK signature verification failed: ${verifyResult.errors}", verifyResult.isVerified)
        assertTrue(verifyResult.isVerifiedUsingV1Scheme)
        assertTrue(verifyResult.isVerifiedUsingV2Scheme)
        assertTrue(verifyResult.isVerifiedUsingV3Scheme)

        // 2. 验证打包产物内部结构与精简效果
        ZipFile(result.apkFile).use { zip ->
            assertNotNull(zip.getEntry(StandaloneHookRunner.INIT_LUA_ENTRY))
            assertNotNull(zip.getEntry(StandaloneHookRunner.MAIN_LUA_ENTRY))
            assertNotNull(zip.getEntry(StandaloneHookRunner.PACK_CONFIG_ENTRY))
            assertNotNull(zip.getEntry("assets/standalone_project/utils/helper.lua"))
            assertNotNull(zip.getEntry("resources.arsc"))
            assertNotNull(zip.getEntry(ArscBuilder.ICON_ENTRY_PATH))

            // 确认冗余 IDE 资源与编码表已被剥离
            assertEquals(null, zip.getEntry("assets/skill/SKILL.md"))
            assertEquals(null, zip.getEntry("tables/Transcoder_Big5_WordArray.bin"))
            assertEquals(null, zip.getEntry("res/layout/activity_main.xml"))

            // 确认未勾选的 armeabi-v7a 已被过滤，arm64-v8a 保留
            assertNotNull(zip.getEntry("lib/arm64-v8a/libluahook.so"))
            assertEquals(null, zip.getEntry("lib/armeabi-v7a/libluahook.so"))

            // 确认 META-INF/xposed/scope.list 已写入配置的包名
            val scopeEntry = zip.getEntry("META-INF/xposed/scope.list")
            assertNotNull(scopeEntry)
            val scopeText = zip.getInputStream(scopeEntry).bufferedReader().use { it.readText() }
            assertEquals("com.tencent.mm\ncom.eg.android.AlipayGphone", scopeText.trim())

            // 确认 AndroidManifest.xml 已被重写
            val manifestBytes = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() }
            val summary = ManifestEditor.parseSummary(manifestBytes)
            assertEquals("com.example.demomodule", summary.packageName)
            assertEquals(5, summary.versionCode)
            assertEquals("1.2.0", summary.versionName)
            assertEquals("Demo Module", summary.appLabel)
            assertEquals("Custom Xposed Description", summary.xposedDescription)
            assertTrue(summary.permissions.contains("com.example.demomodule.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"))

            // 确认 assets/xposed_init 写入独立入口 StandaloneMainHook，且剥离了 API 102 的 java_init.list
            val initEntry = zip.getEntry("assets/xposed_init")
            assertNotNull(initEntry)
            val initContent = zip.getInputStream(initEntry).bufferedReader().use { it.readText() }.trim()
            assertEquals("com.kulipai.luahook.hook.entry.StandaloneMainHook", initContent)
            assertEquals(null, zip.getEntry("META-INF/xposed/java_init.list"))
        }
    }

    @Test
    fun `DexSlimmer keeps Hook runtime classes and excludes IDE UI classes`() {
        assertTrue(DexSlimmer.shouldKeepClass("Lcom/kulipai/luahook/hook/entry/MainHook;"))
        assertTrue(DexSlimmer.shouldKeepClass("Lcom/kulipai/luahook/hook/entry/StandaloneMainHook;"))
        assertTrue(DexSlimmer.shouldKeepClass("Lcom/kulipai/luahook/hook/activity/StandaloneActivity;"))
        assertTrue(DexSlimmer.shouldKeepClass("Lio/github/kulipai/luahook/hook/entry/LuaHookEngine;"))
        assertTrue(DexSlimmer.shouldKeepClass("Lorg/luaj/Globals;"))
        assertFalse(DexSlimmer.shouldKeepClass("Lcom/kulipai/luahook/feature/home/MainActivity;"))
        assertFalse(DexSlimmer.shouldKeepClass("Lio/github/rosemoe/sora/widget/CodeEditor;"))
        assertFalse(DexSlimmer.shouldKeepClass("Lcom/kongzue/dialogx/dialogs/MessageDialog;"))
        assertFalse(DexSlimmer.shouldKeepClass("Lkotlin/reflect/jvm/internal/KClassImpl;"))
        // R8 top-level obfuscated classes must be preserved
        assertTrue(DexSlimmer.shouldKeepClass("La00;"))
    }

    @Test
    fun `StandaloneHookRunner adapts getProjectDir require dofile loadfile for disk and in-memory assets`() {
        val projectDir = tempFolder.newFolder("standalone_extracted")
        val absDir = projectDir.absolutePath
        val baseDir = StandaloneHookRunner.normalizeBaseDir("MyProj", absDir)
        File(projectDir, "disk_mod.lua").writeText("return { source = 'disk' }")

        val inMemoryAssets = mapOf(
            "mem_mod.lua" to "return { source = 'memory', val = 42 }".toByteArray(),
            "utils/sub_mod.lua" to "return function(x) return x * 2 end".toByteArray(),
            "extra_dofile.lua" to "global_from_dofile = 'loaded_via_dofile'".toByteArray()
        )

        // 1. 验证相对路径、前导斜杠与硬编码工作区路径重定向
        assertEquals("${baseDir}sub/a.lua", StandaloneHookRunner.resolveProjectFilePath("sub/a.lua", baseDir, absDir))
        assertEquals("${baseDir}sub/a.lua", StandaloneHookRunner.resolveProjectFilePath("./sub/a.lua", baseDir, absDir))
        assertEquals(
            "${baseDir}extra_dofile.lua",
            StandaloneHookRunner.resolveProjectFilePath(
                "/data/local/tmp/LuaHook/Project/MyProj/extra_dofile.lua",
                baseDir,
                absDir
            )
        )

        // 2. 验证 require 候选路径生成（支持点号模块、斜杠路径及 .lua 后缀）
        val dotCandidates = StandaloneHookRunner.buildRequireCandidatePaths("utils.sub_mod", baseDir, absDir)
        assertTrue(dotCandidates.contains("utils/sub_mod.lua"))
        assertTrue(dotCandidates.contains("utils/sub_mod/init.lua"))

        val extCandidates = StandaloneHookRunner.buildRequireCandidatePaths("mem_mod.lua", baseDir, absDir)
        assertTrue(extCandidates.contains("mem_mod.lua"))

        // 3. 验证磁盘优先加载与内存 assets 零磁盘兜底
        val diskLoaded = StandaloneHookRunner.loadProjectScriptSource("disk_mod.lua", baseDir, absDir, inMemoryAssets)
        assertNotNull(diskLoaded)
        assertEquals("return { source = 'disk' }", diskLoaded!!.first)

        val memLoaded = StandaloneHookRunner.loadProjectScriptSource("mem_mod.lua", baseDir, absDir, inMemoryAssets)
        assertNotNull(memLoaded)
        assertEquals("return { source = 'memory', val = 42 }", memLoaded!!.first)

        val redirectedLoaded = StandaloneHookRunner.loadProjectScriptSource(
            "/data/local/tmp/LuaHook/Project/MyProj/extra_dofile.lua",
            baseDir,
            absDir,
            inMemoryAssets
        )
        assertNotNull(redirectedLoaded)
        assertEquals("global_from_dofile = 'loaded_via_dofile'", redirectedLoaded!!.first)
    }

    @Test
    fun `ApkSignerHelper supports custom PKCS12 keystore file`() {
        val baseApk = tempFolder.newFile("unsigned_custom.apk")
        createSyntheticBaseApk(baseApk)
        val alignedApk = tempFolder.newFile("aligned_custom.apk")
        ZipAligner.alignZip(baseApk, alignedApk)

        val ksFile = tempFolder.newFile("user_custom.p12")
        val ksPass = "my_secret_pass"
        val keyAlias = "my_release_key"

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, ksPass.toCharArray())
        ks.setKeyEntry(
            keyAlias,
            TestKeys.privateKey,
            ksPass.toCharArray(),
            arrayOf(TestKeys.certificate)
        )
        FileOutputStream(ksFile).use { fos ->
            ks.store(fos, ksPass.toCharArray())
        }

        val signedOut = File(tempFolder.root, "signed_with_custom_ks.apk")
        ApkSignerHelper.signApk(
            inputApk = alignedApk,
            outputApk = signedOut,
            config = SignConfig(
                useCustomKeystore = true,
                keystoreFile = ksFile,
                keystorePassword = ksPass,
                keyAlias = keyAlias,
                keyPassword = ksPass,
                v1SigningEnabled = true,
                v2SigningEnabled = true,
                v3SigningEnabled = false
            )
        )

        val verifyResult = ApkSignerHelper.verifyApk(signedOut)
        assertTrue(verifyResult.isVerified)
        assertTrue(verifyResult.isVerifiedUsingV1Scheme)
        assertTrue(verifyResult.isVerifiedUsingV2Scheme)
        assertFalse(verifyResult.isVerifiedUsingV3Scheme)
    }

    private fun createSyntheticBaseApk(outFile: File) {
        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(buildSampleManifestAxml())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(ByteArray(64) { it.toByte() })
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("assets/xposed_init"))
            zos.write("com.kulipai.luahook.hook.entry.MainHook".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("assets/skill/SKILL.md"))
            zos.write("should be stripped in standalone apk".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("tables/Transcoder_Big5_WordArray.bin"))
            zos.write("should be stripped in standalone apk".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("res/layout/activity_main.xml"))
            zos.write("should be stripped in standalone apk".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("lib/arm64-v8a/libluahook.so"))
            zos.write(ByteArray(512) { 0x42 })
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("lib/armeabi-v7a/libluahook.so"))
            zos.write(ByteArray(512) { 0x24 })
            zos.closeEntry()
        }
    }

    /**
     * 构造一份标准的二进制 AndroidManifest.xml 用于单元测试。
     */
    private fun buildSampleManifestAxml(): ByteArray {
        val strings = listOf(
            "label",             // 0 -> 0x01010001
            "name",              // 1 -> 0x01010003
            "authorities",       // 2 -> 0x01010018
            "description",       // 3 -> 0x01010020
            "value",             // 4 -> 0x01010024
            "versionCode",       // 5 -> 0x0101021b
            "versionName",       // 6 -> 0x0101021c
            "package",           // 7
            "manifest",          // 8
            "application",       // 9
            "provider",          // 10
            "meta-data",         // 11
            "category",          // 12
            "com.kulipai.luahook", // 13
            "4.2.0",             // 14
            ".app.MyApplication",// 15
            "LuaHook",           // 16
            "com.kulipai.luahook.XposedService", // 17
            "xposeddescription", // 18
            "Original Description", // 19
            "android.intent.category.LAUNCHER", // 20
            "permission",        // 21
            "com.kulipai.luahook.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" // 22
        )

        val resIds = intArrayOf(
            0x01010001,
            0x01010003,
            0x01010018,
            0x01010020,
            0x01010024,
            0x0101021b,
            0x0101021c
        )

        val strData = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)
        for (i in strings.indices) {
            offsets[i] = strData.size()
            val s = strings[i]
            strData.write(s.length and 0xFF)
            strData.write((s.length shr 8) and 0xFF)
            strData.write(s.toByteArray(StandardCharsets.UTF_16LE))
            strData.write(0)
            strData.write(0)
        }
        while (strData.size() % 4 != 0) strData.write(0)
        val strDataBytes = strData.toByteArray()
        val spHeaderSize = 28
        val spStringsStart = spHeaderSize + strings.size * 4
        val spChunkSize = spStringsStart + strDataBytes.size

        val spBuf = ByteBuffer.allocate(spChunkSize).order(ByteOrder.LITTLE_ENDIAN)
        spBuf.putShort(0x0001)
        spBuf.putShort(spHeaderSize.toShort())
        spBuf.putInt(spChunkSize)
        spBuf.putInt(strings.size)
        spBuf.putInt(0)
        spBuf.putInt(0)
        spBuf.putInt(spStringsStart)
        spBuf.putInt(0)
        for (off in offsets) spBuf.putInt(off)
        spBuf.put(strDataBytes)

        val rmSize = 8 + resIds.size * 4
        val rmBuf = ByteBuffer.allocate(rmSize).order(ByteOrder.LITTLE_ENDIAN)
        rmBuf.putShort(0x0180)
        rmBuf.putShort(8)
        rmBuf.putInt(rmSize)
        for (id in resIds) rmBuf.putInt(id)

        fun buildStartElement(tagIdx: Int, attrs: List<Triple<Int, Int, Int>>): ByteArray {
            val chunkSize = 16 + 20 + attrs.size * 20
            val b = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
            b.putShort(0x0102)
            b.putShort(16)
            b.putInt(chunkSize)
            b.putInt(1)
            b.putInt(-1)
            b.putInt(-1)
            b.putInt(tagIdx)
            b.putShort(20)
            b.putShort(20)
            b.putShort(attrs.size.toShort())
            b.putShort(0)
            b.putShort(0)
            b.putShort(0)
            for ((nameIdx, dataType, dataVal) in attrs) {
                b.putInt(-1)
                b.putInt(nameIdx)
                b.putInt(if (dataType == 0x03) dataVal else -1)
                b.putShort(8)
                b.put(0)
                b.put(dataType.toByte())
                b.putInt(dataVal)
            }
            return b.array()
        }

        val manifestElem = buildStartElement(
            8,
            listOf(
                Triple(7, 0x03, 13),
                Triple(5, 0x10, 44),
                Triple(6, 0x03, 14)
            )
        )
        val permElem = buildStartElement(
            21,
            listOf(
                Triple(1, 0x03, 22)
            )
        )
        val appElem = buildStartElement(
            9,
            listOf(
                Triple(1, 0x03, 15),
                Triple(0, 0x03, 16),
                Triple(3, 0x03, 19)
            )
        )
        val providerElem = buildStartElement(
            10,
            listOf(
                Triple(2, 0x03, 17)
            )
        )
        val metaElem = buildStartElement(
            11,
            listOf(
                Triple(1, 0x03, 18),
                Triple(4, 0x03, 19)
            )
        )
        val categoryElem = buildStartElement(
            12,
            listOf(
                Triple(1, 0x03, 20)
            )
        )

        val chunks = ByteArrayOutputStream()
        chunks.write(spBuf.array())
        chunks.write(rmBuf.array())
        chunks.write(manifestElem)
        chunks.write(permElem)
        chunks.write(appElem)
        chunks.write(providerElem)
        chunks.write(metaElem)
        chunks.write(categoryElem)

        val body = chunks.toByteArray()
        val total = 8 + body.size
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(0x0003)
        out.putShort(8)
        out.putInt(total)
        out.put(body)
        return out.array()
    }
}
