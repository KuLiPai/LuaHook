package io.dingyi222666.sora.lua.tool

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.UnderlineSpan
import dalvik.system.DexFile
import io.kulipai.sora.luaj.CompletionName
import com.kulipai.luahook.R
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Field
import java.lang.reflect.Method

object PackageUtil {
    private var packages: JSONObject? = null
    private val classMap = mutableMapOf<String, MutableList<String>>()

    private val classNames = mutableListOf<String>()

    private val cacheClassed = mutableMapOf<String, List<CompletionName>>()

    private const val FULL_TYPE: Boolean = true

    @JvmStatic
    fun load(context: Context) {
        if (packages != null) return

        try {
            // 先尝试从缓存加载
            loadFromCache(context)
        } catch (e: Exception) {
            // 缓存加载失败，从原始资源加载
            loadFromRawResource(context)
        }
    }

    @JvmStatic
    fun load(context: Context, path: String) {
        if (packages != null) return

        try {
            File(path).readText().let { content ->
                initializePackages(context, content)
            }
        } catch (e: Exception) {
            // 如果自定义路径加载失败，回退到默认加载
            load(context)
        }
    }

    private fun loadFromCache(context: Context) {
        val cacheFile = File(context.cacheDir, "package_cache.json")
        if (!cacheFile.exists()) {
            throw FileNotFoundException("Cache file not found")
        }
        initializePackages(context, cacheFile.readText())
    }

    private fun loadFromRawResource(context: Context) {
        context.resources.openRawResource(R.raw.android).use { stream ->
            val content = stream.bufferedReader().use { it.readText() }
            initializePackages(context, content)

            // 保存到缓存
            try {
                val cacheFile = File(context.cacheDir, "package_cache.json")
                cacheFile.writeText(content)
            } catch (e: Exception) {
                // 缓存保存失败可以忽略
            }
        }
    }

    private fun initializePackages(context: Context, jsonContent: String) {
        packages = JSONObject(jsonContent)

        // 处理 DexFile 条目
        DexFile(context.packageCodePath).entries().asSequence()
            .map { it.replace("$", ".").split(".") }.forEach { parts ->
                var currentJson = packages
                parts.filter { it.length > 2 }.forEach { part ->
                    currentJson = currentJson?.let { json ->
                        when {
                            json.has(part) -> json.getJSONObject(part)
                            else -> JSONObject().also { json.put(part, it) }
                        }
                    }
                }
            }

        // 构建导入映射
        buildImports(packages!!, "")
    }

    private fun buildImports(json: JSONObject, pkg: String) {
        json.keys().asSequence().forEach { key ->
            try {
                val subJson = json.getJSONObject(key)
                if (key[0].isUpperCase()) {
                    classMap.getOrPut(key) { mutableListOf() }.add(pkg + key)
                }
                if (subJson.length() == 0) {
                    classNames.add(key)
                } else {
                    buildImports(subJson, "$pkg$key.")
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }
    }

    @JvmStatic
    fun fix(name: String): List<String>? = classMap[name]

    @JvmStatic
    fun filter(name: String): List<CompletionName> {
        if (packages == null) return emptyList()

        val parts = name.split(".")
        val (searchDepth, searchTerm) = if (name.endsWith(".")) {
            Pair(parts.size, "")
        } else {
            Pair(parts.size - 1, parts.last().lowercase())
        }

        var currentJson = packages
        // 遍历路径
        for (i in 0 until searchDepth) {
            currentJson = try {
                currentJson?.getJSONObject(parts[i])
            } catch (e: Exception) {
                return emptyList()
            }
        }


        val classes = classNames.filter { it.lowercase().startsWith(searchTerm) }
            .map {
                CompletionName(
                    it,
                    CompletionItemKind.Class,
                    fix(it)?.joinToString("|") ?: " :class"
                )
            }
            .toList()

        return classes
    }

    @JvmStatic
    fun filterPackage(name: String, current: String): List<CompletionName> {
        if (packages == null) return emptyList()

        return classMap.keys.asSequence().filter {
            name.indexOf(it.lowercase()) != -1 && name.length > 3
        }.flatMap {
            classMap.getValue(it)
        }.mapNotNull {
            runCatching {
                Class.forName(it)
            }.getOrNull()
        }.flatMap {
            runCatching {
                cacheClassed.getOrPut(it.name) {
                    getJavaMethods(it) + getJavaFields(it)
                }
            }.getOrElse { emptyList() }
        }.distinctBy {
            it.name.toString()
        }
            .filter {
                it.name.toString().lowercase().startsWith(current.lowercase())
            }.toList()
    }

    private fun getJavaMethodParameters(method: Method) = buildString {
        append(method.name)
        append("(")
        method.parameters.forEach {
            append(it.type.simpleName)
            append(",")
        }
        if (method.parameterCount > 0) {
            deleteAt(lastIndex)
        }

        append(")")
    }.let {
        SpannableString(it).apply {
            setSpan(object : UnderlineSpan() {

                override fun updateDrawState(ds: TextPaint) {
                    ds.setColor(Color.GRAY)
                    ds.isUnderlineText = false
                }
            }, method.name.length, it.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun getJavaMethods(clazz: Class<*>): List<CompletionName> {

        val methods = clazz.methods
        val names = mutableListOf<CompletionName>()
        for (method in methods) {
            if (method.name.contains("lambda")) continue

            names.add(
                buildJavaMethod(method)
            )

            if (method.parameters.isEmpty() && method.name.startsWith("get")) {
                val name = method.name.substring(3).let {
                    it.substring(0, 1).lowercase() + it.substring(1)
                }

                names.add(
                    CompletionName(
                        name,
                        CompletionItemKind.Property,
                        if (FULL_TYPE) method.returnType.simpleName else " :property"
                    )
                )

                continue
            }

            if (method.parameters.size == 1 && method.name.startsWith(
                    "set"
                )
            ) {
                var name = method.name.substring(3)

                if (name.endsWith("Listener")) {
                    name = name.substring(0, name.length - 8)
                }

                // sort the first char
                name = name.substring(0, 1).lowercase() + name.substring(1)

                names.add(
                    CompletionName(
                        name,
                        CompletionItemKind.Field,
                        if (FULL_TYPE) method.parameters[0].type.simpleName else " :field"
                    )
                )

                continue
            }


        }

        return names
    }

    @JvmStatic
    fun buildJavaMethod(method: Method): CompletionName {
        return CompletionName(
            if (FULL_TYPE) getJavaMethodParameters(method) else "",
            CompletionItemKind.Method,
            if (FULL_TYPE) method.returnType.simpleName else " :method",
            method.name
        )
    }

    @JvmStatic
    fun buildJavaField(field: Field): CompletionName {
        return CompletionName(
            field.name,
            CompletionItemKind.Field,
            if (FULL_TYPE) field.type.simpleName else " :field"
        )
    }

    private fun getJavaFields(clazz: Class<*>): List<CompletionName> {
        val fields = clazz.fields
        val names = mutableListOf<CompletionName>()
        for (field in fields) {
            names.add(
                buildJavaField(field)
            )
        }

        return names
    }
}