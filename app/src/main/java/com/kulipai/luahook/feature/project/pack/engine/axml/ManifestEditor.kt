package com.kulipai.luahook.feature.project.pack.engine.axml

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * 打包清单配置参数
 */
data class ManifestConfig(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val description: String,
    val showLauncherIcon: Boolean = true,
    val customIconResId: Int? = null,
    val standaloneSlimMode: Boolean = true
)

/**
 * 解析后的摘要信息（用于测试校验与产物核对）
 */
data class ParsedManifestSummary(
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val appLabel: String,
    val xposedDescription: String,
    val applicationName: String,
    val authorities: List<String>,
    val permissions: List<String>,
    val usesPermissions: List<String>,
    val activities: List<String>,
    val hasLauncherCategory: Boolean
)

/**
 * Android 二进制 XML (AXML) 清单编辑器。
 *
 * 采用「保留原字符串池索引 + 尾部追加新字符串 + 按树深度裁剪冗余节点」的无损重写策略：
 * - 100% 保持原 `RES_XML_RESOURCE_MAP_TYPE` 属性资源 ID 映射不变；
 * - 原地更新 `<manifest>`、`<application>`、`<activity>`、`<provider>`、`<permission>`、`<uses-permission>`、`<meta-data>` 属性；
 * - 自动重写 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` 等自带包名前缀的权限声明，彻底解决与主应用共存安装时的 `INSTALL_FAILED_DUPLICATE_PERMISSION` 冲突；
 * - 在独立精简模式下，将入口 Activity 指向纯原生 SDK 的 `StandaloneActivity`，将 Application 设为 `android.app.Application`，并裁剪所有主 IDE 专属的 Activity、Service、Receiver、Provider 与敏感权限。
 */
object ManifestEditor {

    private const val RES_XML_TYPE = 0x0003
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val RES_XML_END_ELEMENT_TYPE = 0x0103

    private const val UTF8_FLAG = 0x00000100

    // Android 标准属性 Resource ID
    private const val ATTR_THEME = 0x01010000
    private const val ATTR_LABEL = 0x01010001
    private const val ATTR_ICON = 0x01010002
    private const val ATTR_NAME = 0x01010003
    private const val ATTR_AUTHORITIES = 0x01010018
    private const val ATTR_DESCRIPTION = 0x01010020
    private const val ATTR_VALUE = 0x01010024
    private const val ATTR_VERSION_CODE = 0x0101021b
    private const val ATTR_VERSION_NAME = 0x0101021c
    private const val ATTR_FULL_BACKUP_CONTENT = 0x010104eb
    private const val ATTR_APP_COMPONENT_FACTORY = 0x0101057a
    private const val ATTR_DATA_EXTRACTION_RULES = 0x0101063e
    private const val ATTR_ROUND_ICON = 0x0101052c

    // AXML TypedValue dataType
    private const val TYPE_NULL = 0x00
    private const val TYPE_REFERENCE = 0x01
    private const val TYPE_STRING = 0x03
    private const val TYPE_INT_DEC = 0x10

    // @android:style/Theme.DeviceDefault.DayNight
    private const val SYSTEM_THEME_DEVICE_DEFAULT_DAYNIGHT = 0x01030241

    private const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
    private const val CATEGORY_INFO = "android.intent.category.INFO"

    private const val STANDALONE_ACTIVITY_CLASS = "com.kulipai.luahook.hook.activity.StandaloneActivity"
    private const val EMPTY_ACTIVITY_CLASS = "com.kulipai.luahook.hook.activity.EmptyActivity"
    private const val XPOSED_PROVIDER_CLASS = "io.github.libxposed.service.XposedProvider"

    private val IDE_ONLY_PERMISSIONS = setOf(
        "moe.shizuku.manager.permission.API_V23",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.ACCESS_LOCAL_NETWORK"
    )

    /**
     * 修改编译后的二进制 AndroidManifest.xml 字节数组并返回新字节数组。
     */
    fun modifyManifest(axmlBytes: ByteArray, config: ManifestConfig): ByteArray {
        val buf = ByteBuffer.wrap(axmlBytes).order(ByteOrder.LITTLE_ENDIAN)
        val fileType = buf.short.toInt() and 0xFFFF
        val fileHeaderSize = buf.short.toInt() and 0xFFFF
        val fileSize = buf.int

        require(fileType == RES_XML_TYPE) {
            "Invalid AXML magic type: 0x${fileType.toString(16)}"
        }

        // 1. 解析第一个 Chunk（必须是 String Pool）
        val spStart = fileHeaderSize
        buf.position(spStart)
        val spType = buf.short.toInt() and 0xFFFF
        val spHeaderSize = buf.short.toInt() and 0xFFFF
        val spChunkSize = buf.int
        require(spType == RES_STRING_POOL_TYPE) {
            "Expected RES_STRING_POOL_TYPE at offset $spStart, got 0x${spType.toString(16)}"
        }

        val stringPool = parseStringPool(axmlBytes, spStart, spHeaderSize, spChunkSize)
        val strings = stringPool.strings.toMutableList()

        fun addOrGetString(value: String): Int {
            for (i in stringPool.originalCount until strings.size) {
                if (strings[i] == value) return i
            }
            strings.add(value)
            return strings.size - 1
        }

        // 2. 遍历后续所有 Chunk，原地更新属性并按树深度过滤冗余节点
        val restStart = spStart + spChunkSize
        val restLength = fileSize - restStart
        val restBytes = axmlBytes.copyOfRange(restStart, restStart + restLength)
        val restBuf = ByteBuffer.wrap(restBytes).order(ByteOrder.LITTLE_ENDIAN)

        val filteredChunksOut = ByteArrayOutputStream(restLength)
        var resourceMap = IntArray(0)
        var originalPackage = "com.kulipai.luahook"
        var skipDepth = 0

        var offset = 0
        while (offset + 8 <= restBytes.size) {
            restBuf.position(offset)
            val chunkType = restBuf.short.toInt() and 0xFFFF
            val chunkHeaderSize = restBuf.short.toInt() and 0xFFFF
            val chunkSize = restBuf.int
            if (chunkSize <= 0 || offset + chunkSize > restBytes.size) break

            if (skipDepth > 0) {
                if (chunkType == RES_XML_START_ELEMENT_TYPE) {
                    skipDepth++
                } else if (chunkType == RES_XML_END_ELEMENT_TYPE) {
                    skipDepth--
                }
                offset += chunkSize
                continue
            }

            var dropCurrentElement = false

            if (chunkType == 0x0180) { // RES_XML_RESOURCE_MAP_TYPE
                val count = (chunkSize - chunkHeaderSize) / 4
                resourceMap = IntArray(count)
                restBuf.position(offset + chunkHeaderSize)
                for (i in 0 until count) {
                    resourceMap[i] = restBuf.int
                }
            } else if (chunkType == RES_XML_START_ELEMENT_TYPE) {
                val elemBase = offset + chunkHeaderSize
                restBuf.position(elemBase)
                restBuf.int // nsIdx
                val nameIdx = restBuf.int
                val attrStart = restBuf.short.toInt() and 0xFFFF
                val attrSize = restBuf.short.toInt() and 0xFFFF
                val attrCount = restBuf.short.toInt() and 0xFFFF

                val tagName = strings.getOrNull(nameIdx) ?: ""
                val attrsBase = elemBase + attrStart

                fun attrResId(attrNameIdx: Int): Int {
                    return if (attrNameIdx in resourceMap.indices) resourceMap[attrNameIdx] else 0
                }

                fun setStringAttr(attrPos: Int, strValue: String) {
                    val idx = addOrGetString(strValue)
                    restBuf.putInt(attrPos + 8, idx) // rawValue
                    restBuf.put(attrPos + 15, TYPE_STRING.toByte()) // dataType
                    restBuf.putInt(attrPos + 16, idx) // data
                }

                fun setIntAttr(attrPos: Int, intValue: Int) {
                    restBuf.putInt(attrPos + 8, -1) // rawValue = -1
                    restBuf.put(attrPos + 15, TYPE_INT_DEC.toByte()) // dataType
                    restBuf.putInt(attrPos + 16, intValue) // data
                }

                fun setRefAttr(attrPos: Int, resIdValue: Int) {
                    restBuf.putInt(attrPos + 8, -1) // rawValue = -1
                    restBuf.put(attrPos + 15, TYPE_REFERENCE.toByte()) // dataType
                    restBuf.putInt(attrPos + 16, resIdValue) // data
                }

                fun clearAttrToNull(attrPos: Int) {
                    restBuf.putInt(attrPos + 8, -1)
                    restBuf.put(attrPos + 15, TYPE_NULL.toByte())
                    restBuf.putInt(attrPos + 16, 0)
                }

                when (tagName) {
                    "manifest" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aNameIdx = restBuf.getInt(aPos + 4)
                            val aName = strings.getOrNull(aNameIdx) ?: ""
                            val resId = attrResId(aNameIdx)
                            val aData = restBuf.getInt(aPos + 16)

                            when {
                                aName == "package" -> {
                                    val oldPkg = strings.getOrNull(aData)
                                    if (!oldPkg.isNullOrBlank()) {
                                        originalPackage = oldPkg
                                    }
                                    setStringAttr(aPos, config.packageName)
                                }
                                resId == ATTR_VERSION_CODE || aName == "versionCode" -> {
                                    setIntAttr(aPos, config.versionCode)
                                }
                                resId == ATTR_VERSION_NAME || aName == "versionName" -> {
                                    setStringAttr(aPos, config.versionName)
                                }
                            }
                        }
                    }

                    "permission", "uses-permission", "uses-permission-sdk-23" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aNameIdx = restBuf.getInt(aPos + 4)
                            val aName = strings.getOrNull(aNameIdx) ?: ""
                            val resId = attrResId(aNameIdx)
                            val aData = restBuf.getInt(aPos + 16)

                            if (resId == ATTR_NAME || aName == "name") {
                                val permName = strings.getOrNull(aData) ?: ""
                                if (config.standaloneSlimMode && permName in IDE_ONLY_PERMISSIONS) {
                                    dropCurrentElement = true
                                } else if (permName.startsWith("$originalPackage.")) {
                                    val newPermName = config.packageName + permName.removePrefix(originalPackage)
                                    setStringAttr(aPos, newPermName)
                                }
                            }
                        }
                    }

                    "application" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aNameIdx = restBuf.getInt(aPos + 4)
                            val aName = strings.getOrNull(aNameIdx) ?: ""
                            val resId = attrResId(aNameIdx)
                            val aData = restBuf.getInt(aPos + 16)

                            when {
                                resId == ATTR_LABEL || aName == "label" -> {
                                    setStringAttr(aPos, config.appName)
                                }
                                resId == ATTR_ICON || aName == "icon" -> {
                                    if (config.customIconResId != null) {
                                        setRefAttr(aPos, config.customIconResId)
                                    }
                                }
                                resId == ATTR_ROUND_ICON || aName == "roundIcon" -> {
                                    if (config.customIconResId != null) {
                                        setRefAttr(aPos, config.customIconResId)
                                    }
                                }
                                resId == ATTR_DESCRIPTION || aName == "description" -> {
                                    setStringAttr(aPos, config.description)
                                }
                                resId == ATTR_NAME || aName == "name" -> {
                                    if (config.standaloneSlimMode) {
                                        setStringAttr(aPos, "android.app.Application")
                                    } else {
                                        val clsName = strings.getOrNull(aData) ?: ""
                                        if (clsName.startsWith(".")) {
                                            setStringAttr(aPos, originalPackage + clsName)
                                        }
                                    }
                                }
                                config.standaloneSlimMode && (resId == ATTR_THEME || aName == "theme") -> {
                                    setRefAttr(aPos, SYSTEM_THEME_DEVICE_DEFAULT_DAYNIGHT)
                                }
                                config.standaloneSlimMode && (resId == ATTR_APP_COMPONENT_FACTORY || aName == "appComponentFactory") -> {
                                    setStringAttr(aPos, "android.app.AppComponentFactory")
                                }
                                config.standaloneSlimMode && (
                                    resId == ATTR_DATA_EXTRACTION_RULES || aName == "dataExtractionRules" ||
                                        resId == ATTR_FULL_BACKUP_CONTENT || aName == "fullBackupContent"
                                    ) -> {
                                    clearAttrToNull(aPos)
                                }
                            }
                        }
                    }

                    "activity" -> {
                        var isMainActivity = false
                        var isEmptyActivity = false
                        var nameAttrPos = -1

                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aNameIdx = restBuf.getInt(aPos + 4)
                            val aName = strings.getOrNull(aNameIdx) ?: ""
                            val resId = attrResId(aNameIdx)
                            val aData = restBuf.getInt(aPos + 16)

                            if (resId == ATTR_NAME || aName == "name") {
                                nameAttrPos = aPos
                                val clsName = strings.getOrNull(aData) ?: ""
                                val fullClsName = if (clsName.startsWith(".")) {
                                    originalPackage + clsName
                                } else {
                                    clsName
                                }
                                if (clsName.startsWith(".")) {
                                    setStringAttr(aPos, fullClsName)
                                }
                                if (fullClsName.endsWith(".MainActivity") || fullClsName == STANDALONE_ACTIVITY_CLASS) {
                                    isMainActivity = true
                                } else if (fullClsName == EMPTY_ACTIVITY_CLASS) {
                                    isEmptyActivity = true
                                }
                            }
                        }

                        if (config.standaloneSlimMode) {
                            if (isMainActivity) {
                                if (nameAttrPos >= 0) {
                                    setStringAttr(nameAttrPos, STANDALONE_ACTIVITY_CLASS)
                                }
                                for (i in 0 until attrCount) {
                                    val aPos = attrsBase + i * attrSize
                                    val aNameIdx = restBuf.getInt(aPos + 4)
                                    val aName = strings.getOrNull(aNameIdx) ?: ""
                                    val resId = attrResId(aNameIdx)
                                    if (resId == ATTR_LABEL || aName == "label") {
                                        setStringAttr(aPos, config.appName)
                                    } else if (resId == ATTR_THEME || aName == "theme") {
                                        setRefAttr(aPos, SYSTEM_THEME_DEVICE_DEFAULT_DAYNIGHT)
                                    }
                                }
                            } else if (!isEmptyActivity) {
                                dropCurrentElement = true
                            }
                        } else if (isMainActivity) {
                            for (i in 0 until attrCount) {
                                val aPos = attrsBase + i * attrSize
                                val aNameIdx = restBuf.getInt(aPos + 4)
                                val aName = strings.getOrNull(aNameIdx) ?: ""
                                val resId = attrResId(aNameIdx)
                                if (resId == ATTR_LABEL || aName == "label") {
                                    setStringAttr(aPos, config.appName)
                                }
                            }
                        }
                    }

                    "service", "receiver" -> {
                        if (config.standaloneSlimMode) {
                            dropCurrentElement = true
                        } else {
                            for (i in 0 until attrCount) {
                                val aPos = attrsBase + i * attrSize
                                val aNameIdx = restBuf.getInt(aPos + 4)
                                val aName = strings.getOrNull(aNameIdx) ?: ""
                                val resId = attrResId(aNameIdx)
                                val aData = restBuf.getInt(aPos + 16)
                                if (resId == ATTR_NAME || aName == "name") {
                                    val clsName = strings.getOrNull(aData) ?: ""
                                    if (clsName.startsWith(".")) {
                                        setStringAttr(aPos, originalPackage + clsName)
                                    }
                                }
                            }
                        }
                    }

                    "provider" -> {
                        var providerName = ""
                        var authVal = ""
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aNameIdx = restBuf.getInt(aPos + 4)
                            val aName = strings.getOrNull(aNameIdx) ?: ""
                            val resId = attrResId(aNameIdx)
                            val aData = restBuf.getInt(aPos + 16)

                            if (resId == ATTR_NAME || aName == "name") {
                                val clsName = strings.getOrNull(aData) ?: ""
                                providerName = if (clsName.startsWith(".")) {
                                    originalPackage + clsName
                                } else {
                                    clsName
                                }
                                if (clsName.startsWith(".")) {
                                    setStringAttr(aPos, providerName)
                                }
                            } else if (resId == ATTR_AUTHORITIES || aName == "authorities") {
                                val auth = strings.getOrNull(aData) ?: ""
                                authVal = auth
                                if (auth.startsWith(originalPackage)) {
                                    val newAuth = config.packageName + auth.removePrefix(originalPackage)
                                    setStringAttr(aPos, newAuth)
                                }
                            }
                        }

                        if (config.standaloneSlimMode) {
                            val isXposedProvider = providerName == XPOSED_PROVIDER_CLASS ||
                                authVal.endsWith(".XposedService")
                            if (!isXposedProvider) {
                                dropCurrentElement = true
                            }
                        }
                    }

                    "meta-data" -> {
                        var metaName = ""
                        var valueAttrPos = -1
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aNameIdx = restBuf.getInt(aPos + 4)
                            val aName = strings.getOrNull(aNameIdx) ?: ""
                            val resId = attrResId(aNameIdx)
                            val aData = restBuf.getInt(aPos + 16)

                            if (resId == ATTR_NAME || aName == "name") {
                                metaName = strings.getOrNull(aData) ?: ""
                            } else if (resId == ATTR_VALUE || aName == "value") {
                                valueAttrPos = aPos
                            }
                        }
                        if (metaName == "xposeddescription" && valueAttrPos >= 0) {
                            setStringAttr(valueAttrPos, config.description)
                        } else if (config.standaloneSlimMode && metaName == "moe.shizuku.client.V3_SUPPORT") {
                            dropCurrentElement = true
                        }
                    }

                    "category" -> {
                        if (!config.showLauncherIcon) {
                            for (i in 0 until attrCount) {
                                val aPos = attrsBase + i * attrSize
                                val aNameIdx = restBuf.getInt(aPos + 4)
                                val aName = strings.getOrNull(aNameIdx) ?: ""
                                val resId = attrResId(aNameIdx)
                                val aData = restBuf.getInt(aPos + 16)

                                if (resId == ATTR_NAME || aName == "name") {
                                    val catName = strings.getOrNull(aData) ?: ""
                                    if (catName == CATEGORY_LAUNCHER) {
                                        setStringAttr(aPos, CATEGORY_INFO)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (dropCurrentElement) {
                skipDepth = 1
            } else {
                filteredChunksOut.write(restBytes, offset, chunkSize)
            }

            offset += chunkSize
        }

        // 3. 重新序列化 String Pool 并拼接过滤后的其余 Chunks
        val newStringPoolBytes = buildStringPoolChunk(strings, stringPool.isUtf8)
        val finalRestBytes = filteredChunksOut.toByteArray()
        val totalSize = 8 + newStringPoolBytes.size + finalRestBytes.size
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(RES_XML_TYPE.toShort())
        out.putShort(8.toShort())
        out.putInt(totalSize)
        out.put(newStringPoolBytes)
        out.put(finalRestBytes)
        return out.array()
    }

    /**
     * 解析二进制 AXML 摘要信息，用于单元测试与打包校验。
     */
    fun parseSummary(axmlBytes: ByteArray): ParsedManifestSummary {
        val buf = ByteBuffer.wrap(axmlBytes).order(ByteOrder.LITTLE_ENDIAN)
        val fileType = buf.short.toInt() and 0xFFFF
        val fileHeaderSize = buf.short.toInt() and 0xFFFF
        val fileSize = buf.int
        require(fileType == RES_XML_TYPE)

        val spStart = fileHeaderSize
        buf.position(spStart)
        val spType = buf.short.toInt() and 0xFFFF
        val spHeaderSize = buf.short.toInt() and 0xFFFF
        val spChunkSize = buf.int
        require(spType == RES_STRING_POOL_TYPE)

        val strings = parseStringPool(axmlBytes, spStart, spHeaderSize, spChunkSize).strings

        var packageName = ""
        var versionCode = 0
        var versionName = ""
        var appLabel = ""
        var xposedDesc = ""
        var applicationName = ""
        val authorities = mutableListOf<String>()
        val permissions = mutableListOf<String>()
        val usesPermissions = mutableListOf<String>()
        val activities = mutableListOf<String>()
        var hasLauncherCategory = false

        var offset = spStart + spChunkSize
        while (offset + 8 <= fileSize) {
            buf.position(offset)
            val chunkType = buf.short.toInt() and 0xFFFF
            val chunkHeaderSize = buf.short.toInt() and 0xFFFF
            val chunkSize = buf.int
            if (chunkSize <= 0 || offset + chunkSize > fileSize) break

            if (chunkType == RES_XML_START_ELEMENT_TYPE) {
                val elemBase = offset + chunkHeaderSize
                buf.position(elemBase)
                buf.int // ns
                val nameIdx = buf.int
                val attrStart = buf.short.toInt() and 0xFFFF
                val attrSize = buf.short.toInt() and 0xFFFF
                val attrCount = buf.short.toInt() and 0xFFFF

                val tagName = strings.getOrNull(nameIdx) ?: ""
                val attrsBase = elemBase + attrStart

                when (tagName) {
                    "manifest" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            when (aName) {
                                "package" -> packageName = strings.getOrNull(aData) ?: ""
                                "versionCode" -> versionCode = aData
                                "versionName" -> versionName = strings.getOrNull(aData) ?: ""
                            }
                        }
                    }
                    "permission" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            if (aName == "name") {
                                strings.getOrNull(aData)?.let { permissions.add(it) }
                            }
                        }
                    }
                    "uses-permission", "uses-permission-sdk-23" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            if (aName == "name") {
                                strings.getOrNull(aData)?.let { usesPermissions.add(it) }
                            }
                        }
                    }
                    "application" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            when (aName) {
                                "label" -> appLabel = strings.getOrNull(aData) ?: ""
                                "name" -> applicationName = strings.getOrNull(aData) ?: ""
                            }
                        }
                    }
                    "activity" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            if (aName == "name") {
                                strings.getOrNull(aData)?.let { activities.add(it) }
                            }
                        }
                    }
                    "provider" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            if (aName == "authorities") {
                                strings.getOrNull(aData)?.let { authorities.add(it) }
                            }
                        }
                    }
                    "meta-data" -> {
                        var mName = ""
                        var mVal = ""
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            if (aName == "name") mName = strings.getOrNull(aData) ?: ""
                            if (aName == "value") mVal = strings.getOrNull(aData) ?: ""
                        }
                        if (mName == "xposeddescription") xposedDesc = mVal
                    }
                    "category" -> {
                        for (i in 0 until attrCount) {
                            val aPos = attrsBase + i * attrSize
                            val aName = strings.getOrNull(buf.getInt(aPos + 4)) ?: ""
                            val aData = buf.getInt(aPos + 16)
                            if (aName == "name" && strings.getOrNull(aData) == CATEGORY_LAUNCHER) {
                                hasLauncherCategory = true
                            }
                        }
                    }
                }
            }
            offset += chunkSize
        }

        return ParsedManifestSummary(
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            appLabel = appLabel,
            xposedDescription = xposedDesc,
            applicationName = applicationName,
            authorities = authorities,
            permissions = permissions,
            usesPermissions = usesPermissions,
            activities = activities,
            hasLauncherCategory = hasLauncherCategory
        )
    }

    private data class ParsedStringPool(
        val strings: List<String>,
        val originalCount: Int,
        val isUtf8: Boolean
    )

    private fun parseStringPool(
        bytes: ByteArray,
        chunkStart: Int,
        headerSize: Int,
        chunkSize: Int
    ): ParsedStringPool {
        val buf = ByteBuffer.wrap(bytes, chunkStart, chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(chunkStart + 8)
        val stringCount = buf.int
        buf.int // styleCount
        val flags = buf.int
        val stringsStart = buf.int
        buf.int // stylesStart

        val isUtf8 = (flags and UTF8_FLAG) != 0
        val offsets = IntArray(stringCount)
        buf.position(chunkStart + headerSize)
        for (i in 0 until stringCount) {
            offsets[i] = buf.int
        }

        val dataBase = chunkStart + stringsStart
        val result = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val strOffset = dataBase + offsets[i]
            if (isUtf8) {
                var pos = strOffset
                val u16Len = bytes[pos].toInt() and 0xFF
                pos += if ((u16Len and 0x80) != 0) 2 else 1
                val b1 = bytes[pos].toInt() and 0xFF
                val utf8ByteLen: Int
                if ((b1 and 0x80) != 0) {
                    val b2 = bytes[pos + 1].toInt() and 0xFF
                    utf8ByteLen = ((b1 and 0x7F) shl 8) or b2
                    pos += 2
                } else {
                    utf8ByteLen = b1
                    pos += 1
                }
                result.add(String(bytes, pos, utf8ByteLen, StandardCharsets.UTF_8))
            } else {
                var pos = strOffset
                val u1 = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
                val charLen: Int
                if ((u1 and 0x8000) != 0) {
                    val u2 = (bytes[pos + 2].toInt() and 0xFF) or ((bytes[pos + 3].toInt() and 0xFF) shl 8)
                    charLen = ((u1 and 0x7FFF) shl 16) or u2
                    pos += 4
                } else {
                    charLen = u1
                    pos += 2
                }
                result.add(String(bytes, pos, charLen * 2, StandardCharsets.UTF_16LE))
            }
        }

        return ParsedStringPool(result, stringCount, isUtf8)
    }

    private fun buildStringPoolChunk(strings: List<String>, isUtf8: Boolean): ByteArray {
        val stringDataOut = ByteArrayOutputStream()
        val offsets = IntArray(strings.size)

        for (i in strings.indices) {
            offsets[i] = stringDataOut.size()
            val s = strings[i]
            if (isUtf8) {
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
            } else {
                val utf16Len = s.length
                if (utf16Len > 0x7FFF) {
                    val hi = 0x8000 or ((utf16Len shr 16) and 0x7FFF)
                    val lo = utf16Len and 0xFFFF
                    stringDataOut.write(hi and 0xFF)
                    stringDataOut.write((hi shr 8) and 0xFF)
                    stringDataOut.write(lo and 0xFF)
                    stringDataOut.write((lo shr 8) and 0xFF)
                } else {
                    stringDataOut.write(utf16Len and 0xFF)
                    stringDataOut.write((utf16Len shr 8) and 0xFF)
                }
                val utf16Bytes = s.toByteArray(StandardCharsets.UTF_16LE)
                stringDataOut.write(utf16Bytes)
                stringDataOut.write(0)
                stringDataOut.write(0)
            }
        }

        while (stringDataOut.size() % 4 != 0) {
            stringDataOut.write(0)
        }

        val stringDataBytes = stringDataOut.toByteArray()
        val headerSize = 28
        val offsetsSize = strings.size * 4
        val stringsStart = headerSize + offsetsSize
        val chunkSize = stringsStart + stringDataBytes.size

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(RES_STRING_POOL_TYPE.toShort())
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.putInt(strings.size)
        buf.putInt(0)
        buf.putInt(if (isUtf8) UTF8_FLAG else 0)
        buf.putInt(stringsStart)
        buf.putInt(0)
        for (offset in offsets) {
            buf.putInt(offset)
        }
        buf.put(stringDataBytes)
        return buf.array()
    }
}
