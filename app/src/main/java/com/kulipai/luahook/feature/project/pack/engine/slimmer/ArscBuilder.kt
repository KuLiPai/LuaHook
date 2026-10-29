package com.kulipai.luahook.feature.project.pack.engine.slimmer

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * 极简 `resources.arsc` 生成器。
 *
 * 独立打包模块仅需声明一个应用图标资源（`0x7f010000` -> `res/icon.png`），
 * 其余字符串（`label`、`description`、`xposeddescription`）已在二进制 `AndroidManifest.xml` 中内联为字面量字符串。
 * 通过生成约 556 字节的极简资源表，可彻底剥离原 APK 中 1.4MB 的 `resources.arsc` 与上千个 UI 资源文件。
 */
object ArscBuilder {

    const val ICON_RESOURCE_ID = 0x7f010000
    const val ICON_ENTRY_PATH = "res/icon.png"

    private const val RES_TABLE_TYPE = 0x0002
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_TABLE_PACKAGE_TYPE = 0x0200
    private const val RES_TABLE_TYPE_TYPE = 0x0201
    private const val RES_TABLE_TYPE_SPEC_TYPE = 0x0202

    private const val UTF8_FLAG = 0x00000100
    private const val TYPE_STRING = 0x03

    fun buildMinimalArsc(
        packageName: String,
        iconPath: String = ICON_ENTRY_PATH
    ): ByteArray {
        val globalStringPool = buildUtf8StringPool(listOf(iconPath))
        val typeStringPool = buildUtf8StringPool(listOf("drawable"))
        val keyStringPool = buildUtf8StringPool(listOf("icon"))

        // 1. ResTable_typeSpec (0x0202): header=16, size=20, id=1, res0=0, res1=0, entryCount=1, flags[0]=0
        val typeSpecBuf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(RES_TABLE_TYPE_SPEC_TYPE.toShort())
            putShort(16.toShort())
            putInt(20)
            put(1.toByte()) // id = 1 (0x01)
            put(0.toByte()) // res0
            putShort(0.toShort()) // res1
            putInt(1) // entryCount = 1
            putInt(0) // spec flag for entry 0
        }
        val typeSpecBytes = typeSpecBuf.array()

        // 2. ResTable_type (0x0201):
        // header_size = 84 (20 bytes base + 64 bytes ResTable_config)
        // 1 entry offset (4 bytes) + 1 entry (8 bytes ResTable_entry + 8 bytes Res_value) = 20 bytes
        val typeHeaderSize = 84
        val entriesStart = typeHeaderSize + 4
        val typeChunkSize = entriesStart + 16
        val typeChunkBuf = ByteBuffer.allocate(typeChunkSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(RES_TABLE_TYPE_TYPE.toShort())
            putShort(typeHeaderSize.toShort())
            putInt(typeChunkSize)
            put(1.toByte()) // id = 1
            put(0.toByte()) // flags = 0
            putShort(0.toShort()) // reserved
            putInt(1) // entryCount = 1
            putInt(entriesStart) // entriesStart

            // ResTable_config (64 bytes, first 4 bytes = size 64)
            putInt(64)
            position(typeHeaderSize)

            // Entry offset table: entry[0] at offset 0
            putInt(0)

            // ResTable_entry (size=8, flags=0, key=0 -> "icon" in keyStringPool)
            putShort(8.toShort())
            putShort(0.toShort())
            putInt(0)

            // Res_value (size=8, res0=0, dataType=TYPE_STRING (0x03), data=0 -> iconPath in globalStringPool)
            putShort(8.toShort())
            put(0.toByte())
            put(TYPE_STRING.toByte())
            putInt(0)
        }
        val typeChunkBytes = typeChunkBuf.array()

        // 3. ResTable_package (0x0200): header_size = 288
        val pkgHeaderSize = 288
        val typeStringsOffset = pkgHeaderSize
        val keyStringsOffset = typeStringsOffset + typeStringPool.size
        val pkgBodySize = typeStringPool.size + keyStringPool.size + typeSpecBytes.size + typeChunkBytes.size
        val pkgChunkSize = pkgHeaderSize + pkgBodySize

        val pkgBuf = ByteBuffer.allocate(pkgChunkSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(RES_TABLE_PACKAGE_TYPE.toShort())
            putShort(pkgHeaderSize.toShort())
            putInt(pkgChunkSize)
            putInt(0x7f) // package id = 0x7f

            // char16_t name[128] (256 bytes UTF-16LE, null-padded)
            val nameBytes = packageName.take(127).toByteArray(StandardCharsets.UTF_16LE)
            val paddedName = ByteArray(256)
            System.arraycopy(nameBytes, 0, paddedName, 0, nameBytes.size.coerceAtMost(254))
            put(paddedName)

            putInt(typeStringsOffset)
            putInt(1) // lastPublicType
            putInt(keyStringsOffset)
            putInt(1) // lastPublicKey
            putInt(0) // typeIdOffset

            put(typeStringPool)
            put(keyStringPool)
            put(typeSpecBytes)
            put(typeChunkBytes)
        }
        val pkgChunkBytes = pkgBuf.array()

        // 4. ResTable_header (0x0002): header_size = 12
        val totalSize = 12 + globalStringPool.size + pkgChunkBytes.size
        return ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(RES_TABLE_TYPE.toShort())
            putShort(12.toShort())
            putInt(totalSize)
            putInt(1) // packageCount = 1
            put(globalStringPool)
            put(pkgChunkBytes)
        }.array()
    }

    private fun buildUtf8StringPool(strings: List<String>): ByteArray {
        val stringDataOut = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)

        for (i in strings.indices) {
            offsets[i] = stringDataOut.size()
            val s = strings[i]
            val utf16Len = s.length
            if (utf16Len > 0x7F) {
                stringDataOut.write(0x80 or ((utf16Len shr 8) and 0x7F))
                stringDataOut.write(utf16Len and 0xFF)
            } else {
                stringDataOut.write(utf16Len)
            }
            val utf8Bytes = s.toByteArray(StandardCharsets.UTF_8)
            val utf8Len = utf8Bytes.size
            if (utf8Len > 0x7F) {
                stringDataOut.write(0x80 or ((utf8Len shr 8) and 0x7F))
                stringDataOut.write(utf8Len and 0xFF)
            } else {
                stringDataOut.write(utf8Len)
            }
            stringDataOut.write(utf8Bytes)
            stringDataOut.write(0)
        }

        while (stringDataOut.size() % 4 != 0) {
            stringDataOut.write(0)
        }

        val stringDataBytes = stringDataOut.toByteArray()
        val headerSize = 28
        val stringsStart = headerSize + strings.size * 4
        val chunkSize = stringsStart + stringDataBytes.size

        return ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(RES_STRING_POOL_TYPE.toShort())
            putShort(headerSize.toShort())
            putInt(chunkSize)
            putInt(strings.size)
            putInt(0) // styleCount
            putInt(UTF8_FLAG)
            putInt(stringsStart)
            putInt(0) // stylesStart
            for (off in offsets) {
                putInt(off)
            }
            put(stringDataBytes)
        }.array()
    }
}
