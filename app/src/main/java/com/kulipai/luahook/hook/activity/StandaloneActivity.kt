package com.kulipai.luahook.hook.activity

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/**
 * 独立打包模块的轻量级状态展示页。
 *
 * 仅依赖原生 Android SDK (`android.app.Activity`, `android.widget.*`)，
 * 零 AndroidX / Material / ViewBinding / R 资源依赖，使打包后的 APK 可彻底剥离主工程的 UI 库与资源表。
 */
class StandaloneActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

            val bgColor = resolveDynamicColor(
                lightRes = android.R.color.system_neutral1_10,
                darkRes = android.R.color.system_neutral1_900,
                lightFallback = Color.parseColor("#F8F9FA"),
                darkFallback = Color.parseColor("#121316"),
                isNight = isNight
            )
            val cardColor = resolveDynamicColor(
                lightRes = android.R.color.system_neutral1_0,
                darkRes = android.R.color.system_neutral1_800,
                lightFallback = Color.parseColor("#FFFFFF"),
                darkFallback = Color.parseColor("#1E2025"),
                isNight = isNight
            )
            val cardStrokeColor = resolveDynamicColor(
                lightRes = android.R.color.system_neutral2_200,
                darkRes = android.R.color.system_neutral2_700,
                lightFallback = Color.parseColor("#E3E6EA"),
                darkFallback = Color.parseColor("#2E3138"),
                isNight = isNight
            )
            val primaryTextColor = resolveDynamicColor(
                lightRes = android.R.color.system_neutral1_900,
                darkRes = android.R.color.system_neutral1_50,
                lightFallback = Color.parseColor("#1A1C1E"),
                darkFallback = Color.parseColor("#F1F3F5"),
                isNight = isNight
            )
            val secondaryTextColor = resolveDynamicColor(
                lightRes = android.R.color.system_neutral2_700,
                darkRes = android.R.color.system_neutral2_300,
                lightFallback = Color.parseColor("#5F6368"),
                darkFallback = Color.parseColor("#9AA0A6"),
                isNight = isNight
            )
            val accentColor = resolveDynamicColor(
                lightRes = android.R.color.system_accent1_600,
                darkRes = android.R.color.system_accent1_300,
                lightFallback = Color.parseColor("#0B57D0"),
                darkFallback = Color.parseColor("#A8C7FA"),
                isNight = isNight
            )
            val accentBgColor = resolveDynamicColor(
                lightRes = android.R.color.system_accent1_100,
                darkRes = android.R.color.system_accent1_800,
                lightFallback = Color.parseColor("#D3E3FD"),
                darkFallback = Color.parseColor("#1E3A5F"),
                isNight = isNight
            )
            val buttonTextColor = resolveDynamicColor(
                lightRes = android.R.color.system_neutral1_0,
                darkRes = android.R.color.system_accent1_900,
                lightFallback = Color.parseColor("#FFFFFF"),
                darkFallback = Color.parseColor("#062E6F"),
                isNight = isNight
            )

            val configJsonText = runCatching {
                assets.open("standalone_project/pack_config.json").bufferedReader().use { it.readText() }
            }.getOrDefault("{}")
            val obj = runCatching { JSONObject(configJsonText) }.getOrDefault(JSONObject())

            val appName = obj.optString("appName", "").takeIf { it.isNotBlank() }
                ?: runCatching { applicationInfo.loadLabel(packageManager).toString() }.getOrNull()
                ?: "Standalone Module"
            val pkgName = obj.optString("packageName", packageName)
            val versionName = obj.optString("versionName", "1.0.0")
            val versionCode = obj.optInt("versionCode", 1)
            val desc = obj.optString("description", "")
            val author = obj.optString("author", "")
            val scopesArr = obj.optJSONArray("scopes")
            val scopes = mutableListOf<String>()
            if (scopesArr != null) {
                for (i in 0 until scopesArr.length()) {
                    scopesArr.optString(i)?.takeIf { it.isNotBlank() }?.let { scopes.add(it) }
                }
            }

            title = appName
            runCatching { actionBar?.hide() }

        val density = resources.displayMetrics.density
        fun Int.dp(): Int = (this * density + 0.5f).toInt()

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(bgColor)
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.dp(), 48.dp(), 24.dp(), 32.dp())
        }

        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(80.dp(), 80.dp())
            runCatching {
                setImageDrawable(applicationInfo.loadIcon(packageManager))
            }
        }
        container.addView(iconView)

        val titleTv = TextView(this).apply {
            text = appName
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            setPadding(0, 16.dp(), 0, 4.dp())
        }
        container.addView(titleTv)

        val subTv = TextView(this).apply {
            text = "$pkgName · v$versionName ($versionCode)"
            textSize = 13f
            setTextColor(secondaryTextColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12.dp())
        }
        container.addView(subTv)

        val badgeTv = TextView(this).apply {
            text = "Xposed / LSPosed Module"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
            setPadding(14.dp(), 6.dp(), 14.dp(), 6.dp())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999.dp().toFloat()
                setColor(accentBgColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24.dp()
            }
        }
        container.addView(badgeTv)

        val cardBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 18.dp(), 20.dp(), 18.dp())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18.dp().toFloat()
                setColor(cardColor)
                setStroke(1.dp(), cardStrokeColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (author.isNotBlank()) {
            val authorLabel = TextView(this).apply {
                text = "作者 / Author"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(secondaryTextColor)
            }
            val authorValue = TextView(this).apply {
                text = author
                textSize = 15f
                setTextColor(primaryTextColor)
                setPadding(0, 2.dp(), 0, 14.dp())
            }
            cardBody.addView(authorLabel)
            cardBody.addView(authorValue)
        }

        if (desc.isNotBlank()) {
            val descLabel = TextView(this).apply {
                text = "模块说明 / Description"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(secondaryTextColor)
            }
            val descValue = TextView(this).apply {
                text = desc
                textSize = 15f
                setTextColor(primaryTextColor)
                setPadding(0, 2.dp(), 0, 14.dp())
            }
            cardBody.addView(descLabel)
            cardBody.addView(descValue)
        }

        val scopeTitle = TextView(this).apply {
            text = "生效作用域 / Target Scope"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 8.dp())
        }
        cardBody.addView(scopeTitle)

        val displayScopes = if (scopes.isEmpty() || scopes.contains("all")) {
            listOf("All Apps (全局作用域)")
        } else {
            scopes
        }

        for (s in displayScopes) {
            val chipTv = TextView(this).apply {
                text = s
                textSize = 13f
                setTextColor(primaryTextColor)
                setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10.dp().toFloat()
                    setColor(bgColor)
                    setStroke(1.dp(), cardStrokeColor)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 6.dp()
                }
                setOnClickListener {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("scope", s))
                    Toast.makeText(this@StandaloneActivity, "已复制: $s", Toast.LENGTH_SHORT).show()
                }
            }
            cardBody.addView(chipTv)
        }

        container.addView(cardBody)

        // 自动释放项目脚本到当前模块的私有目录（如 /data/data/<pkg>/files/project）
        val projectDir = File(filesDir, "project")
        val extractedFiles = extractProjectAssetsToPrivateDir(projectDir)

        val filesCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 18.dp(), 20.dp(), 18.dp())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18.dp().toFloat()
                setColor(cardColor)
                setStroke(1.dp(), cardStrokeColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16.dp()
            }
        }

        val filesTitle = TextView(this).apply {
            text = "项目文件 (模块私有目录) / Project Files"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 4.dp())
        }
        filesCard.addView(filesTitle)

        val dirPathTv = TextView(this).apply {
            text = projectDir.absolutePath
            textSize = 12f
            setTextColor(accentColor)
            setPadding(0, 0, 0, 10.dp())
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("path", projectDir.absolutePath))
                Toast.makeText(this@StandaloneActivity, "已复制路径: ${projectDir.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        }
        filesCard.addView(dirPathTv)

        if (extractedFiles.isNotEmpty()) {
            for ((rel, size) in extractedFiles) {
                val fileItemTv = TextView(this).apply {
                    val sizeText = when {
                        size < 1024 -> "$size B"
                        size < 1024 * 1024 -> "${size / 1024} KB"
                        else -> "%.1f MB".format(size / (1024f * 1024f))
                    }
                    text = "• $rel ($sizeText)"
                    textSize = 13f
                    setTextColor(primaryTextColor)
                    setPadding(0, 2.dp(), 0, 2.dp())
                }
                filesCard.addView(fileItemTv)
            }
        } else {
            val emptyTv = TextView(this).apply {
                text = "（assets 中未扫描到脚本，已使用内存虚拟文件系统）"
                textSize = 13f
                setTextColor(secondaryTextColor)
            }
            filesCard.addView(emptyTv)
        }

        val reExtractBtn = TextView(this).apply {
            text = "重新释放脚本到私有目录"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
            gravity = Gravity.CENTER
            setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8.dp().toFloat()
                setColor(accentBgColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12.dp()
            }
            setOnClickListener {
                val refreshed = extractProjectAssetsToPrivateDir(projectDir)
                Toast.makeText(this@StandaloneActivity, "成功释放 ${refreshed.size} 个项目文件至私有目录", Toast.LENGTH_SHORT).show()
            }
        }
        filesCard.addView(reExtractBtn)

        container.addView(filesCard)

        val firstTargetPkg = scopes.firstOrNull { it != "all" }
        if (firstTargetPkg != null) {
            val launchBtn = TextView(this).apply {
                text = "启动目标应用 ($firstTargetPkg)"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(buttonTextColor)
                gravity = Gravity.CENTER
                setPadding(20.dp(), 14.dp(), 20.dp(), 14.dp())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14.dp().toFloat()
                    setColor(accentColor)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 24.dp()
                }
                setOnClickListener {
                    var launched = false
                    val launchIntent = packageManager.getLaunchIntentForPackage(firstTargetPkg)
                        ?: packageManager.getLeanbackLaunchIntentForPackage(firstTargetPkg)
                        ?: Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            setPackage(firstTargetPkg)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }.takeIf { it.resolveActivity(packageManager) != null }

                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching {
                            startActivity(launchIntent)
                            launched = true
                        }.onFailure { err ->
                            Toast.makeText(
                                this@StandaloneActivity,
                                "启动失败: ${err.message ?: err.javaClass.simpleName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    if (!launched && launchIntent == null) {
                        Toast.makeText(
                            this@StandaloneActivity,
                            "未安装目标应用或无启动入口: $firstTargetPkg",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            container.addView(launchBtn)
        }

        rootScroll.addView(container)
        setContentView(rootScroll)

        runCatching {
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val appearance = if (isNight) 0 else (
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                    appearance,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else if (!isNight) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        } catch (t: Throwable) {
            android.util.Log.e("StandaloneActivity", "Error in onCreate", t)
            val errScroll = ScrollView(this).apply {
                isFillViewport = true
                setBackgroundColor(Color.parseColor("#121316"))
            }
            val errTv = TextView(this).apply {
                text = "模块界面初始化异常:\n${t.message}\n\n${t.stackTraceToString()}"
                setPadding(32, 64, 32, 32)
                setTextColor(Color.parseColor("#FF6B6B"))
                textSize = 13f
            }
            errScroll.addView(errTv)
            setContentView(errScroll)
        }
    }

    private fun extractProjectAssetsToPrivateDir(projectDir: File): List<Pair<String, Long>> {
        val extracted = mutableListOf<Pair<String, Long>>()
        projectDir.mkdirs()
        val apkFile = File(packageResourcePath)
        if (apkFile.exists()) {
            runCatching {
                ZipFile(apkFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        val name = entry.name
                        if (!name.startsWith("assets/standalone_project/")) continue
                        val relPath = name.removePrefix("assets/standalone_project/").trimStart('/')
                        if (relPath.isBlank() || relPath.contains("..") || relPath == "pack_config.json") continue
                        val outFile = File(projectDir, relPath)
                        outFile.parentFile?.mkdirs()
                        if (outFile.exists()) outFile.setWritable(true)
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        outFile.setReadable(true, false)
                        extracted.add(relPath to outFile.length())
                    }
                }
            }
        }
        if (extracted.isEmpty()) {
            runCatching {
                fun copyDir(subDir: String) {
                    val list = assets.list(if (subDir.isEmpty()) "standalone_project" else "standalone_project/$subDir") ?: emptyArray()
                    for (name in list) {
                        val rel = if (subDir.isEmpty()) name else "$subDir/$name"
                        val fullAssetPath = "standalone_project/$rel"
                        val children = assets.list(fullAssetPath)
                        if (!children.isNullOrEmpty()) {
                            copyDir(rel)
                        } else {
                            if (name == "pack_config.json") continue
                            runCatching {
                                val outFile = File(projectDir, rel)
                                outFile.parentFile?.mkdirs()
                                if (outFile.exists()) outFile.setWritable(true)
                                assets.open(fullAssetPath).use { input ->
                                    outFile.outputStream().use { output -> input.copyTo(output) }
                                }
                                outFile.setReadable(true, false)
                                extracted.add(rel to outFile.length())
                            }
                        }
                    }
                }
                copyDir("")
            }
        }
        return extracted
    }

    private fun resolveDynamicColor(
        lightRes: Int,
        darkRes: Int,
        lightFallback: Int,
        darkFallback: Int,
        isNight: Boolean
    ): Int {
        val targetRes = if (isNight) darkRes else lightRes
        val fallback = if (isNight) darkFallback else lightFallback
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { resources.getColor(targetRes, theme) }.getOrDefault(fallback)
        } else {
            fallback
        }
    }
}
