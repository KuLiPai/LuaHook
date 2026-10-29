package com.kulipai.luahook.feature.project.pack.engine.signer

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * 纯 Kotlin 实现的 Android APK ZipAlign 对齐器。
 *
 * 严格遵循 Android AOSP `zipalign` 与 Android 15+ 16KB Page Size 规范：
 * - 所有未压缩条目（STORED，如 `resources.arsc`、图片资源）按 4 字节边界对齐；
 * - 所有未压缩共享库（`lib/**/*.so`）按 16384 字节（16KB，同时满足 4KB 与 4 字节）边界对齐；
 * - 禁用 Data Descriptor 标志，确保 Local File Header 偏移量精确可控，100% 兼容 Google `apksig`。
 */
object ZipAligner {

    private const val LFH_SIGNATURE = 0x04034b50
    private const val CDFH_SIGNATURE = 0x02014b50
    private const val EOCD_SIGNATURE = 0x06054b50

    private const val METHOD_STORED = 0
    private const val METHOD_DEFLATED = 8

    const val DEFAULT_ALIGNMENT = 4
    const val SO_PAGE_ALIGNMENT = 16384 // 16 KB page alignment (Android 15+ ready)

    /**
     * 对输入 APK/ZIP 文件执行对齐并输出到目标文件。
     */
    fun alignZip(inputFile: File, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        ZipFile(inputFile).use { zip ->
            BufferedOutputStream(FileOutputStream(outputFile), 65536).use { out ->
                val cdRecords = ArrayList<CentralDirRecord>()
                var currentOffset = 0L

                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (entry.isDirectory) continue

                    val rawBytes = zip.getInputStream(entry).use { it.readBytes() }
                    val shouldStore = shouldStoreUncompressed(name, entry.method)

                    val crc = CRC32().apply { update(rawBytes) }.value.toInt()
                    val uncompressedSize = rawBytes.size
                    val payloadBytes: ByteArray
                    val method: Int

                    if (shouldStore) {
                        method = METHOD_STORED
                        payloadBytes = rawBytes
                    } else {
                        method = METHOD_DEFLATED
                        payloadBytes = deflateBytes(rawBytes)
                    }

                    val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
                    val alignment = when {
                        method != METHOD_STORED -> 1
                        name.startsWith("lib/") && name.endsWith(".so") -> SO_PAGE_ALIGNMENT
                        else -> DEFAULT_ALIGNMENT
                    }

                    val dataStartWithoutExtra = currentOffset + 30L + nameBytes.size
                    val padding = if (alignment > 1) {
                        ((alignment - (dataStartWithoutExtra % alignment)) % alignment).toInt()
                    } else {
                        0
                    }
                    val extraBytes = ByteArray(padding)

                    val localHeaderOffset = currentOffset
                    val lfh = ByteBuffer.allocate(30 + nameBytes.size + extraBytes.size)
                        .order(ByteOrder.LITTLE_ENDIAN)
                    lfh.putInt(LFH_SIGNATURE)
                    lfh.putShort(20.toShort()) // version needed to extract (2.0)
                    lfh.putShort(0x0800.toShort()) // general purpose bit flag: UTF-8 (bit 11), no data descriptor
                    lfh.putShort(method.toShort())
                    lfh.putInt(0x54210000) // fixed DOS time/date for deterministic builds
                    lfh.putInt(crc)
                    lfh.putInt(payloadBytes.size)
                    lfh.putInt(uncompressedSize)
                    lfh.putShort(nameBytes.size.toShort())
                    lfh.putShort(extraBytes.size.toShort())
                    lfh.put(nameBytes)
                    lfh.put(extraBytes)

                    val lfhArray = lfh.array()
                    out.write(lfhArray)
                    out.write(payloadBytes)

                    currentOffset += lfhArray.size + payloadBytes.size

                    cdRecords.add(
                        CentralDirRecord(
                            nameBytes = nameBytes,
                            method = method,
                            crc = crc,
                            compressedSize = payloadBytes.size,
                            uncompressedSize = uncompressedSize,
                            localHeaderOffset = localHeaderOffset.toInt()
                        )
                    )
                }

                val cdStartOffset = currentOffset.toInt()
                var cdSize = 0
                for (rec in cdRecords) {
                    val cdfh = ByteBuffer.allocate(46 + rec.nameBytes.size)
                        .order(ByteOrder.LITTLE_ENDIAN)
                    cdfh.putInt(CDFH_SIGNATURE)
                    cdfh.putShort(20.toShort()) // version made by
                    cdfh.putShort(20.toShort()) // version needed
                    cdfh.putShort(0x0800.toShort()) // UTF-8 flag
                    cdfh.putShort(rec.method.toShort())
                    cdfh.putInt(0x54210000) // DOS time/date
                    cdfh.putInt(rec.crc)
                    cdfh.putInt(rec.compressedSize)
                    cdfh.putInt(rec.uncompressedSize)
                    cdfh.putShort(rec.nameBytes.size.toShort())
                    cdfh.putShort(0.toShort()) // extra length in CD = 0
                    cdfh.putShort(0.toShort()) // comment length = 0
                    cdfh.putShort(0.toShort()) // disk number = 0
                    cdfh.putShort(0.toShort()) // internal file attributes
                    cdfh.putInt(0) // external file attributes
                    cdfh.putInt(rec.localHeaderOffset)
                    cdfh.put(rec.nameBytes)

                    val arr = cdfh.array()
                    out.write(arr)
                    cdSize += arr.size
                }

                val eocd = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
                eocd.putInt(EOCD_SIGNATURE)
                eocd.putShort(0.toShort()) // disk number
                eocd.putShort(0.toShort()) // disk with CD
                eocd.putShort(cdRecords.size.toShort())
                eocd.putShort(cdRecords.size.toShort())
                eocd.putInt(cdSize)
                eocd.putInt(cdStartOffset)
                eocd.putShort(0.toShort()) // comment length
                out.write(eocd.array())
                out.flush()
            }
        }
    }

    /**
     * 校验指定 APK/ZIP 中所有未压缩条目（STORED）的数据偏移量是否满足对齐要求。
     */
    fun verifyAlignment(zipFile: File): Boolean {
        RandomAccessFile(zipFile, "r").use { raf ->
            var offset = 0L
            val headerBuf = ByteArray(30)
            while (offset + 30 <= raf.length()) {
                raf.seek(offset)
                raf.readFully(headerBuf)
                val bb = ByteBuffer.wrap(headerBuf).order(ByteOrder.LITTLE_ENDIAN)
                val sig = bb.int
                if (sig != LFH_SIGNATURE) {
                    // 到达 Central Directory 或 APK Signing Block
                    break
                }
                bb.short // version
                bb.short // flags
                val method = bb.short.toInt() and 0xFFFF
                bb.int // time
                bb.int // crc
                val compSize = bb.int.toLong() and 0xFFFFFFFFL
                bb.int // uncompSize
                val nameLen = bb.short.toInt() and 0xFFFF
                val extraLen = bb.short.toInt() and 0xFFFF

                val nameBytes = ByteArray(nameLen)
                raf.readFully(nameBytes)
                val name = String(nameBytes, StandardCharsets.UTF_8)

                val dataOffset = offset + 30L + nameLen + extraLen
                if (method == METHOD_STORED && compSize > 0) {
                    val requiredAlign = if (name.startsWith("lib/") && name.endsWith(".so")) {
                        SO_PAGE_ALIGNMENT
                    } else {
                        DEFAULT_ALIGNMENT
                    }
                    if (dataOffset % requiredAlign != 0L) {
                        return false
                    }
                }
                offset = dataOffset + compSize
            }
        }
        return true
    }

    private fun shouldStoreUncompressed(name: String, originalMethod: Int): Boolean {
        if (name == "resources.arsc") return true
        if (name.startsWith("lib/") && name.endsWith(".so")) return true
        if (name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".jpg") || name.endsWith(".ogg") || name.endsWith(".mp3")) {
            return true
        }
        return originalMethod == ZipEntry.STORED
    }

    private fun deflateBytes(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        return try {
            deflater.setInput(input)
            deflater.finish()
            val bos = ByteArrayOutputStream(input.size)
            val buf = ByteArray(8192)
            while (!deflater.finished()) {
                val count = deflater.deflate(buf)
                if (count > 0) {
                    bos.write(buf, 0, count)
                }
            }
            bos.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private class CentralDirRecord(
        val nameBytes: ByteArray,
        val method: Int,
        val crc: Int,
        val compressedSize: Int,
        val uncompressedSize: Int,
        val localHeaderOffset: Int
    )
}
