package com.kulipai.luahook.feature.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.kulipai.luahook.R
import com.kulipai.luahook.app.MyApplication
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.language.LanguageUtils
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.databinding.ActivityMainBinding
import com.kulipai.luahook.feature.logcat.FloatingLogcatController
import com.kulipai.luahook.feature.project.pack.StandaloneModuleViewHelper
import com.kulipai.luahook.feature.script.editor.app.AppsEdit
import com.kulipai.luahook.feature.script.editor.global.EditActivity
import com.kulipai.luahook.feature.setting.SettingsActivity
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var lastLanguage: String = ""
    private val floatingLogcatController by lazy { FloatingLogcatController(this) }

    private val settingsLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (LanguageUtils.getCurrentLanguage(this) != lastLanguage) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageUtils.applyLanguage(this)
        super.onCreate(savedInstanceState)
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun initView() {
        setupInsets()
        if (MyApplication.instanceOrNull?.isStandaloneModule == true) {
            StandaloneModuleViewHelper.bindStandaloneStatusUi(this, binding)
            return
        }
        setupToolbar()
        setupViewPager()
        setupBottomBar()
    }

    override fun initData() {
        if (MyApplication.instanceOrNull?.isStandaloneModule == true) return
        checkLastState()
        initAppData()
    }

    override fun initEvent() {
        if (MyApplication.instanceOrNull?.isStandaloneModule == true) return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppContainer.instance.shellExecutor.mode.collect { mode ->
                    floatingLogcatController.onShellModeChanged(mode)
                }
            }
        }
    }

    // --------------------- Toolbar ---------------------

    private fun setupToolbar() {
        binding.toolbar.menu.add(0, 1, 0, getString(R.string.log))  // 新增菜单项
            .setIcon(R.drawable.baseline_bug_report_24)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        binding.toolbar.menu.add(0, 2, 1, getString(R.string.setting))
            .setIcon(R.drawable.settings_24px)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == 1) {
                floatingLogcatController.toggle()
            }

            if (it.itemId == 2) {
                lastLanguage = LanguageUtils.getCurrentLanguage(this)
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            }
            true
        }
    }

    // --------------------- 状态恢复 ---------------------

    private fun checkLastState() {
        val prefs = getSharedPreferences("status", MODE_PRIVATE)
        when (prefs.getString("current", null)) {
            "apps" -> {
                startActivity(Intent(this, AppsEdit::class.java).apply {
                    putExtra("packageName", prefs.getString("packageName", ""))
                    putExtra("appName", prefs.getString("appName", ""))
                })
            }
            "global" -> startActivity(Intent(this, EditActivity::class.java))
        }
        prefs.edit { putString("current", "null") }
    }

    // --------------------- 数据加载 ---------------------

    private fun initAppData() {
        lifecycleScope.launch {
            AppContainer.instance.installedAppsRepository.getInstalledApps(forceRefresh = true)
            if (AppContainer.instance.shellExecutor.mode.value != ShellMode.NONE) {
                val savedList = getSavedAppList()
                if (savedList.isNotEmpty()) {
                    AppContainer.instance.installedAppsRepository.getAppInfoList(savedList)
                }
            }
        }
    }

    private fun getSavedAppList(): MutableList<String> {
        val serialized = AppContainer.instance.workspaceStorage.read("/apps.txt")
        return if (serialized.isNotBlank()) serialized.split(",").toMutableList()
        else mutableListOf()
    }

    // --------------------- Insets ---------------------

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, 0)
            insets
        }
    }

    // --------------------- BottomBar + ViewPager ---------------------

    private fun setupViewPager() {
        val fragments = listOf(
            HomeFragment(),
            AppsFragment(),
            ProjectFragment(),
            PluginsFragment()
        )
        binding.viewPager2.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment = fragments[position]
            override fun getItemCount() = fragments.size
        }
    }

    private fun setupBottomBar() {
        val menu = binding.bottomBar.menu
        menu.add(Menu.NONE, 0, 0, getString(R.string.home))
            .setIcon(R.drawable.home_24px)
        menu.add(Menu.NONE, 1, 1, getString(R.string.apps))
            .setIcon(R.drawable.apps_24px)
        menu.add(Menu.NONE, 2, 2, getString(R.string.project))
            .setIcon(R.drawable.work_24px)
        menu.add(Menu.NONE, 3, 3, getString(R.string.plugins))
            .setIcon(R.drawable.extension_24px)

        binding.bottomBar.setOnItemSelectedListener {
            binding.viewPager2.currentItem = it.itemId
            true
        }

        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomBar.menu[position].isChecked = true
                updateBottomIcons(position)
            }
        })
    }

    private fun updateBottomIcons(position: Int) {
        val menu = binding.bottomBar.menu
        val icons = listOf(
            R.drawable.home_24px to R.drawable.home_fill_24px,
            R.drawable.apps_24px to R.drawable.apps_24px,
            R.drawable.work_24px to R.drawable.work_24px_fill,
            R.drawable.extension_24px to R.drawable.extension_fill_24px
        )
        for (i in 0 until menu.size) {
            menu[i].setIcon(if (i == position) icons[i].second else icons[i].first)
        }
    }

    // --------------------- Shizuku 权限逻辑 ---------------------

    override fun onStart() {
        super.onStart()
        ShizukuApi.registerPermissionListener()
    }

    override fun onStop() {
        super.onStop()
        ShizukuApi.unregisterPermissionListener()
    }

    override fun onDestroy() {
        floatingLogcatController.destroy()
        super.onDestroy()
    }
}
