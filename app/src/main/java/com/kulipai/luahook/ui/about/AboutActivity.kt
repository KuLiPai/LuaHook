package com.kulipai.luahook.ui.about

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.update.AppUpdater
import com.kulipai.luahook.data.model.DeveloperInfo
import com.kulipai.luahook.databinding.ActivityAboutBinding
import kotlin.math.abs

/**
 * Refactored to use ViewBinding and BaseActivity.
 */

class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    // Colors for expansion/collapse
    private var expandedToolbarColor: Int = 0
    private var collapsedToolbarColor: Int = 0

    // For color interpolation
    private val argbEvaluator = ArgbEvaluator()

    private val appUpdater by lazy { AppUpdater(this) }

    override fun inflateBinding(inflater: LayoutInflater): ActivityAboutBinding {
        return ActivityAboutBinding.inflate(inflater)
    }

    override fun initView() {
        // Colors
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        expandedToolbarColor = ContextCompat.getColor(this, typedValue.resourceId)

        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        collapsedToolbarColor = ContextCompat.getColor(this, typedValue.resourceId)

        // Toolbar title
        binding.toolbar.title = getString(R.string.about)
        binding.toolbar.setTitleTextColor(expandedToolbarColor)
        binding.toolbar.navigationIcon?.let {
            DrawableCompat.setTint(it.mutate(), expandedToolbarColor)
        }

        // Handle insets specifically for this layout structure
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.appBar.setPadding(0, systemBars.top, 0, 0)

            val contentPaddingTop = binding.contentLayout.paddingTop
            binding.contentLayout.setPadding(
                systemBars.left,
                contentPaddingTop,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    private lateinit var developerAdapter: DeveloperAdapter
    private var isDevelopersExpanded = false
    private val collapsedCount = 7

    override fun initEvent() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        val luaHookGithubUrl = "https://github.com/KuLiPai/LuaHook"
        binding.githubCard.setOnClickListener { openGithubUrl(luaHookGithubUrl) }
        binding.cardCheckUpdate.setOnClickListener { checkUpdate() }
        binding.cardDonate.setOnClickListener { showDonatePopup(it) }

        setupDeveloperList()

        // App Logo transparency and Toolbar color fade
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val currentScroll = abs(verticalOffset)

            val scrollRatio =
                if (totalScrollRange == 0) 0f else currentScroll.toFloat() / totalScrollRange.toFloat()

            // Control App Logo transparency
            val appLogoAlpha = (1f - scrollRatio).coerceIn(0f, 1f)
            binding.appLogo.alpha = appLogoAlpha

            if (appLogoAlpha == 0f && binding.appLogo.isVisible) {
                binding.appLogo.visibility = View.INVISIBLE
            } else if (appLogoAlpha > 0f && binding.appLogo.isInvisible) {
                binding.appLogo.visibility = View.VISIBLE
            }
            if (verticalOffset == 0) {
                binding.appLogo.visibility = View.VISIBLE
            }

            // Control Toolbar content color fade
            val colorFadeStartRatio = 0.5f
            val colorFadeEndRatio = 0.8f

            val colorInterpolationRatio = if (scrollRatio <= colorFadeStartRatio) {
                0f
            } else if (scrollRatio >= colorFadeEndRatio) {
                1f
            } else {
                (scrollRatio - colorFadeStartRatio) / (colorFadeEndRatio - colorFadeStartRatio)
            }

            val interpolatedColor = argbEvaluator.evaluate(
                colorInterpolationRatio,
                expandedToolbarColor,
                collapsedToolbarColor
            ) as Int

            binding.toolbar.setTitleTextColor(interpolatedColor)
            binding.toolbar.navigationIcon?.let {
                DrawableCompat.setTint(it.mutate(), interpolatedColor)
            }
        }
    }

    override fun initData() {
        try {
             packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun setupDeveloperList() {
        val developers = getDevelopers()
        developerAdapter = DeveloperAdapter { openGithubUrl(it.githubUrl) }
        binding.rvDevelopers.adapter = developerAdapter

        if (developers.size <= collapsedCount) {
            binding.layoutToggleExpand.visibility = View.GONE
            developerAdapter.submitList(developers)
        } else {
            binding.layoutToggleExpand.visibility = View.VISIBLE
            updateDeveloperListDisplay(developers)
            binding.layoutToggleExpand.setOnClickListener {
                isDevelopersExpanded = !isDevelopersExpanded
                updateDeveloperListDisplay(developers)
                binding.ivToggleExpand.animate()
                    .rotation(if (isDevelopersExpanded) 180f else 0f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun updateDeveloperListDisplay(developers: List<DeveloperInfo>) {
        if (isDevelopersExpanded) {
            developerAdapter.submitList(developers)
            binding.tvToggleExpand.setText(R.string.collapse_contributors)
        } else {
            developerAdapter.submitList(developers.take(collapsedCount))
            binding.tvToggleExpand.text = getString(R.string.expand_all_contributors, developers.size)
        }
    }

    private fun getDevelopers(): List<DeveloperInfo> = listOf(
        DeveloperInfo(
            name = getString(R.string.KuLiPai),
            contribution = getString(R.string.kulipai_contribute),
            avatarRes = R.drawable.kulipai,
            githubUrl = "https://github.com/KuLiPai"
        ),
        DeveloperInfo(
            name = "California",
            contribution = getString(R.string.california_contribute),
            avatarRes = R.drawable.california,
            githubUrl = "https://github.com/Samzhaohx"
        ),
        DeveloperInfo(
            name = getString(R.string.paditianxiu),
            contribution = getString(R.string.padi_contribute),
            avatarRes = R.drawable.padi,
            githubUrl = "https://github.com/paditianxiu"
        ),
        DeveloperInfo(
            name = "Eleven",
            contribution = getString(R.string.eleven_contribute),
            avatarRes = R.drawable.eleven,
            githubUrl = "https://github.com/imconfident11"
        ),
        DeveloperInfo(
            name = "Carrot",
            contribution = getString(R.string.carrot_contribute),
            avatarRes = R.drawable.carrot,
            githubUrl = "https://github.com/TrialCarrot"
        ),
        DeveloperInfo(
            name = "AQinTrue",
            contribution = getString(R.string.aqintrue_contribute),
            avatarRes = R.drawable.aqintrue,
            githubUrl = "https://github.com/AQinTrue"
        ) ,
        DeveloperInfo(
            name = "Ghost-xx",
            contribution = getString(R.string.ghost_xx_contribute),
            avatarRes = android.R.color.transparent,
            githubUrl = "https://github.com/ghost-xx"
        )
    )

    private fun openGithubUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, resources.getString(R.string.cant_open_links), Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUpdate() {
        appUpdater.checkUpdate(object : AppUpdater.UpdateCallback {
            override fun onStart() {
                binding.tvUpdateStatus.text = resources.getString(R.string.checking)
            }

            @SuppressLint("SetTextI18n")
            override fun onSuccess(latestVersion: String, releasePageUrl: String, currentVersion: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    binding.tvUpdateStatus.text = resources.getString(R.string.new_version) + latestVersion
                    AlertDialog.Builder(this@AboutActivity)
                        .setTitle(resources.getString(R.string.find_new_version))
                        .setMessage(
                            resources.getString(R.string.current_version) + currentVersion +
                                    resources.getString(R.string.n_latest_version) + latestVersion +
                                    resources.getString(R.string.if_goto_github)
                        )
                        .setPositiveButton(resources.getString(R.string.goto_github_release)) { dialog, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, releasePageUrl.toUri())
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    this@AboutActivity,
                                    resources.getString(R.string.cant_open_link_goto) + releasePageUrl,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onLatest(currentVersion: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    binding.tvUpdateStatus.text = resources.getString(R.string.latest_version)
                    Toast.makeText(this@AboutActivity, resources.getString(R.string.latest_version), Toast.LENGTH_SHORT).show()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onError(message: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    binding.tvUpdateStatus.text = resources.getString(R.string.check_failed) + message
                    Toast.makeText(this@AboutActivity, resources.getString(R.string.check_update_false), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showDonatePopup(anchor: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.qrcode, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            isOutsideTouchable = true
            animationStyle = R.style.PopupAnimation
            elevation = 16f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val renderEffect = RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
                    binding.main.setRenderEffect(renderEffect)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            setOnDismissListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.main.setRenderEffect(null)
                }
            }
        }

        popupView.setOnClickListener { popupWindow.dismiss() }

        // It seems safer to use binding.main directly if we know it exists, but the original code used logic for mainLayout null check.
        // With ViewBinding, binding.main is non-null if the ID exists in layout.
        popupWindow.showAtLocation(binding.main, Gravity.CENTER, 0, 0)
    }
}
