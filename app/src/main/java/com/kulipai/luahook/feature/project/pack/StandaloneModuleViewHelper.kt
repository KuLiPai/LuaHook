package com.kulipai.luahook.feature.project.pack

import android.app.Activity
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.kulipai.luahook.databinding.ActivityMainBinding
import org.json.JSONObject

/**
 * 当应用作为独立打包模块安装并从桌面打开时，在 MainActivity 渲染轻量级的模块状态卡片界面。
 */
object StandaloneModuleViewHelper {

    fun bindStandaloneStatusUi(activity: Activity, binding: ActivityMainBinding) {
        val configJsonText = runCatching {
            activity.assets.open("standalone_project/pack_config.json").bufferedReader().use { it.readText() }
        }.getOrDefault("{}")
        val obj = runCatching { JSONObject(configJsonText) }.getOrDefault(JSONObject())

        val appName = obj.optString("appName", activity.applicationInfo.loadLabel(activity.packageManager).toString())
        val pkgName = obj.optString("packageName", activity.packageName)
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

        binding.toolbar.title = appName
        binding.toolbar.menu.clear()
        binding.viewPager2.visibility = View.GONE
        binding.bottomBar.visibility = View.GONE

        val density = activity.resources.displayMetrics.density
        fun Int.dp(): Int = (this * density).toInt()

        val scrollView = ScrollView(activity).apply {
            layoutParams = binding.viewPager2.layoutParams
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.dp(), 28.dp(), 24.dp(), 28.dp())
        }

        val iconView = ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(76.dp(), 76.dp())
            setImageDrawable(activity.applicationInfo.loadIcon(activity.packageManager))
        }
        container.addView(iconView)

        val titleTv = TextView(activity).apply {
            text = appName
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 14.dp(), 0, 4.dp())
        }
        container.addView(titleTv)

        val subTv = TextView(activity).apply {
            text = "$pkgName · v$versionName ($versionCode)"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20.dp())
        }
        container.addView(subTv)

        val infoCard = MaterialCardView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            radius = 16.dp().toFloat()
            cardElevation = 2.dp().toFloat()
        }

        val cardBody = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 18.dp(), 20.dp(), 18.dp())
        }

        if (author.isNotBlank()) {
            val authorTv = TextView(activity).apply {
                text = "作者 / Author: $author"
                textSize = 14f
                setPadding(0, 0, 0, 8.dp())
            }
            cardBody.addView(authorTv)
        }

        if (desc.isNotBlank()) {
            val descTv = TextView(activity).apply {
                text = desc
                textSize = 15f
                setPadding(0, 0, 0, 12.dp())
            }
            cardBody.addView(descTv)
        }

        val scopeTitle = TextView(activity).apply {
            text = "生效作用域 / Target Scope:"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 4.dp(), 0, 6.dp())
        }
        cardBody.addView(scopeTitle)

        val chipGroup = ChipGroup(activity)
        if (scopes.isEmpty() || scopes.contains("all")) {
            chipGroup.addView(Chip(activity).apply { text = "All Apps" })
        } else {
            for (s in scopes) {
                chipGroup.addView(Chip(activity).apply { text = s })
            }
        }
        cardBody.addView(chipGroup)
        infoCard.addView(cardBody)
        container.addView(infoCard)

        val firstTargetPkg = scopes.firstOrNull { it != "all" }
        if (firstTargetPkg != null) {
            val launchBtn = MaterialButton(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    54.dp()
                ).apply {
                    topMargin = 24.dp()
                }
                text = "启动目标应用 ($firstTargetPkg)"
                setOnClickListener {
                    val launchIntent = activity.packageManager.getLaunchIntentForPackage(firstTargetPkg)
                    if (launchIntent != null) {
                        activity.startActivity(launchIntent)
                    } else {
                        Toast.makeText(activity, "未安装目标应用: $firstTargetPkg", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            container.addView(launchBtn)
        }

        scrollView.addView(container)
        binding.main.addView(scrollView)
    }
}
