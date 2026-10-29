package com.kulipai.luahook.feature.project.pack.engine.slimmer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * 独立模块 DEX 瘦身器。
 *
 * 为保证 ART `dex2oat` (`DexFileVerifier` 与 `MethodVerifier`) 及 R8 混淆类的 100% 字节码完整性：
 * 1. 严禁篡改 `.dex` 内部任何 `code_item` 的指令流（避免破坏 `<init>` 父类构造调用链、非 `void` 返回类型校验及 R8 混淆类）；
 * 2. 在多 DEX 分包构建中，若某个 `.dex` 分包完全不包含任何 Hook 运行时所需的类（如纯 SoraEditor、Material、AndroidX UI、apksig 分包），
 *    则直接整包剔除该 `.dex` 文件；
 * 3. 对保留的 `.dex` 分包保持 100% 原始编译器产物字节不变，并由 [com.kulipai.luahook.feature.project.pack.engine.ApkPacker]
 *    将包含 `MainHook` / `NewHook` 的主入口 DEX 置于 `classes.dex` 首位并以 `DEFLATED` 高压缩比写入。
 */
object DexSlimmer {

    private const val MAIN_HOOK_DESCRIPTOR = "Lcom/kulipai/luahook/hook/entry/MainHook;"
    private const val STANDALONE_HOOK_DESCRIPTOR = "Lcom/kulipai/luahook/hook/entry/StandaloneMainHook;"

    private val KEEP_CLASS_PREFIXES = arrayOf(
        "Lcom/kulipai/luahook/hook/",
        "Lcom/kulipai/luahook/core/",
        "Lcom/kulipai/luahook/app/di/",
        "Lio/github/kulipai/luahook/",
        "Lorg/luaj/",
        "Lcom/androlua/",
        "Lcom/luajava/",
        "Lcom/nekolaska/",
        "Ldx/",
        "Lorg/luckypray/dexkit/",
        "Ltop/sacz/xphelper/",
        "Lio/github/libxposed/",
        "Lokhttp3/",
        "Lokio/",
        "Lkotlin/",
        "Lkotlinx/coroutines/",
        "L_COROUTINE/",
        "Lorg/jetbrains/annotations/",
        "Lorg/intellij/lang/annotations/"
    )

    private val EXCLUDE_CLASS_PREFIXES = arrayOf(
        "Lkotlin/reflect/jvm/internal/",
        "Lkotlin/reflect/full/"
    )

    fun shouldKeepClass(classDescriptor: String): Boolean {
        if (classDescriptor.isEmpty()) return false
        // R8 混淆后的顶层类（如 La00;、Lya1;，内部无包名斜杠）可能包含 Kotlin 标准库与运行时核心工具，必须保留
        if (!classDescriptor.contains('/')) return true

        for (excluded in EXCLUDE_CLASS_PREFIXES) {
            if (classDescriptor.startsWith(excluded)) return false
        }
        for (prefix in KEEP_CLASS_PREFIXES) {
            if (classDescriptor.startsWith(prefix)) return true
        }
        return false
    }

    /**
     * 检测给定 DEX 是否包含 Xposed 入口类 `MainHook` 或 `StandaloneMainHook`，用于将其优先排在 `classes.dex`。
     */
    fun containsHookEntry(dexBytes: ByteArray): Boolean {
        return scanDexClasses(dexBytes) { descriptor ->
            descriptor == MAIN_HOOK_DESCRIPTOR || descriptor == STANDALONE_HOOK_DESCRIPTOR
        }
    }

    /**
     * 对单个 DEX 字节数组进行筛选：
     * - 如果该 DEX 内没有任何需要保留的运行时类（纯 UI/编辑器/签名库分包），返回 `null`（调用方跳过该分包）；
     * - 否则原样返回未经篡改的合法 DEX 字节数组，确保 ART `MethodVerifier` 与 R8 混淆类 100% 正常执行。
     */
    fun slimDexOrNull(dexBytes: ByteArray): ByteArray? {
        if (dexBytes.size < 112) return dexBytes
        if (dexBytes[0] != 'd'.code.toByte() ||
            dexBytes[1] != 'e'.code.toByte() ||
            dexBytes[2] != 'x'.code.toByte() ||
            dexBytes[3] != '\n'.code.toByte()
        ) {
            return dexBytes
        }

        val hasKeptClass = scanDexClasses(dexBytes) { descriptor ->
            shouldKeepClass(descriptor)
        }
        return if (hasKeptClass) dexBytes else null
    }

    private inline fun scanDexClasses(
        dexBytes: ByteArray,
        predicate: (String) -> Boolean
    ): Boolean {
        if (dexBytes.size < 112) return false
        return try {
            val buf = ByteBuffer.wrap(dexBytes).order(ByteOrder.LITTLE_ENDIAN)
            val stringIdsSize = buf.getInt(56)
            val stringIdsOff = buf.getInt(60)
            val typeIdsSize = buf.getInt(64)
            val typeIdsOff = buf.getInt(68)
            val classDefsSize = buf.getInt(96)
            val classDefsOff = buf.getInt(100)

            if (classDefsSize <= 0 || stringIdsSize <= 0 || typeIdsSize <= 0) {
                return false
            }

            for (i in 0 until classDefsSize) {
                val base = classDefsOff + i * 32
                val classIdx = buf.getInt(base)
                if (classIdx !in 0 until typeIdsSize) continue
                val strIdx = buf.getInt(typeIdsOff + classIdx * 4)
                if (strIdx !in 0 until stringIdsSize) continue
                val strDataOff = buf.getInt(stringIdsOff + strIdx * 4)
                val (_, utf8Start) = readUleb128(dexBytes, strDataOff)
                var end = utf8Start
                while (end < dexBytes.size && dexBytes[end] != 0.toByte()) {
                    end++
                }
                val descriptor = String(dexBytes, utf8Start, end - utf8Start, StandardCharsets.UTF_8)
                if (predicate(descriptor)) {
                    return true
                }
            }
            false
        } catch (_: Throwable) {
            true
        }
    }

    private fun readUleb128(data: ByteArray, startPos: Int): Pair<Int, Int> {
        var pos = startPos
        var result = 0
        var shift = 0
        while (pos < data.size) {
            val b = data[pos].toInt() and 0xFF
            pos++
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        return result to pos
    }
}
