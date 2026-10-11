package com.kulipai.luahook.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.kulipai.luahook.R
import com.kulipai.luahook.app.MyApplication
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.language.LanguageUtils
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.core.shizuku.ShizukuApi
import com.kulipai.luahook.databinding.ActivityMainBinding
import com.kulipai.luahook.ui.logcat.LogAdapter
import com.kulipai.luahook.ui.logcat.LogcatDelegate
import com.kulipai.luahook.ui.script.editor.app.AppsEdit
import com.kulipai.luahook.ui.script.editor.global.EditActivity
import com.kulipai.luahook.ui.setting.SettingsActivity
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import com.petterp.floatingx.system.systemHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private companion object {
        const val LOGCAT_FLOATING_TAG = "logcat"
        const val LOGCAT_CACHE = "cache"
        const val LOGCAT_CLEAR_TIME = "logClearTime"
    }

    private var lastLanguage: String = ""
    private var floatingLogRefreshJob: Job? = null
    private var floatingLogMinimized = false
    
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
        setupToolbar()
        setupInsets()
        setupViewPager()
        setupBottomBar()
    }

    override fun initData() {
        checkLastState()
        initAppData()
    }

    override fun initEvent() {


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
                val floatingLogcat = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG)
                if (floatingLogcat != null) {
                    if (floatingLogMinimized) {
                        restoreFloatingLogcat()
                    } else if (floatingLogcat.isShowing) {
                        floatingLogcat.hide()
                    } else {
                        floatingLogcat.show()
                        refreshFloatingLogcat()
                    }
                } else {
                    FloatingX.install(LOGCAT_FLOATING_TAG) {
                        layout(R.layout.floating_logcat)
                        anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
                        adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
                        systemHost(this@MainActivity) {
                            theme(R.style.Theme_LuaHook)
                            permission(FxPermissionStrategy.auto())
                        }
                    }.also { control ->
                        floatingLogMinimized = false
                        control.updateContent(::bindFloatingLogcat)
                        control.show()
                    }
                    refreshFloatingLogcat()
                }
            }

            if (it.itemId == 2) {
                lastLanguage = LanguageUtils.getCurrentLanguage(this)
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            }
            true
        }
    }

    private fun bindFloatingLogcat(holder: com.petterp.floatingx.core.container.FxViewHolder) {
        val recyclerView = holder.getView<androidx.recyclerview.widget.RecyclerView>(R.id.LogRecyclerView)
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        }
        if (recyclerView.adapter == null) {
            recyclerView.adapter = LogAdapter(mutableListOf())
        }

        val hasShell = ShellManager.mode.value != ShellManager.Mode.NONE
        holder.getView<View>(R.id.noPower).visibility = if (hasShell) View.GONE else View.VISIBLE
        recyclerView.visibility = if (hasShell) View.VISIBLE else View.INVISIBLE

        holder.getView<ImageButton>(R.id.btnClean).setOnClickListener {
            getSharedPreferences(LOGCAT_CACHE, MODE_PRIVATE).edit {
                putString(LOGCAT_CLEAR_TIME, LogcatDelegate.getCurrentLogcatTimeFormat())
            }
            refreshFloatingLogcat()
        }
        holder.getView<ImageButton>(R.id.btnMinimize).setOnClickListener {
            minimizeFloatingLogcat()
        }
        holder.getView<ImageButton>(R.id.btnClose).setOnClickListener {
            floatingLogRefreshJob?.cancel()
            floatingLogRefreshJob = null
            FloatingX.uninstall(LOGCAT_FLOATING_TAG)
        }
    }

    private fun refreshFloatingLogcat() {
        val control = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) ?: return
        if (floatingLogMinimized) return
        if (ShellManager.mode.value == ShellManager.Mode.NONE) {
            control.updateContent(::bindFloatingLogcat)
            return
        }

        floatingLogRefreshJob?.cancel()
        floatingLogRefreshJob = lifecycleScope.launch {
            val logs = LogcatDelegate.getSystemLogsByTagSince(
                "LuaXposed",
                getSharedPreferences(LOGCAT_CACHE, MODE_PRIVATE).getString(LOGCAT_CLEAR_TIME, null)
            ).filter(String::isNotBlank)

            FloatingX.controlOrNull(LOGCAT_FLOATING_TAG)?.updateContent { holder ->
                if (floatingLogMinimized) return@updateContent
                bindFloatingLogcat(holder)
                val recyclerView = holder.getView<androidx.recyclerview.widget.RecyclerView>(R.id.LogRecyclerView)
                val adapter = recyclerView.adapter as? LogAdapter ?: return@updateContent
                adapter.updateLogs(logs.toMutableList())
                if (logs.isNotEmpty()) recyclerView.scrollToPosition(logs.lastIndex)
            }
        }
    }

    private fun minimizeFloatingLogcat() {
        val control = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) ?: return
        floatingLogMinimized = true
        floatingLogRefreshJob?.cancel()
        floatingLogRefreshJob = null
        control.update {
            layout(R.layout.floating_logcat_ball)
        }
        control.updateContent { holder ->
            holder.view.setOnClickListener {
                restoreFloatingLogcat()
            }
            holder.view.setOnLongClickListener {
                floatingLogRefreshJob?.cancel()
                floatingLogRefreshJob = null
                FloatingX.uninstall(LOGCAT_FLOATING_TAG)
                floatingLogMinimized = false
                true
            }
        }
    }

    private fun restoreFloatingLogcat() {
        val control = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) ?: return
        floatingLogMinimized = false
        control.update {
            layout(R.layout.floating_logcat)
        }
        control.updateContent(::bindFloatingLogcat)
        refreshFloatingLogcat()
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
        val app = application as MyApplication
        lifecycleScope.launch {
            app.getAppListAsync()
            if (ShellManager.mode.value != ShellManager.Mode.NONE) {
                val savedList = getSavedAppList()
                if (savedList.isNotEmpty()) {
                    MyApplication.instance.getAppInfoList(savedList)
                }
            }
        }
    }

    private fun getSavedAppList(): MutableList<String> {
        val serialized = WorkspaceFileManager.read("/apps.txt")
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
        Shizuku.addRequestPermissionResultListener(ShizukuApi.permissionListener)
    }

    override fun onStop() {
        super.onStop()
        Shizuku.removeRequestPermissionResultListener(ShizukuApi.permissionListener)
    }

    override fun onDestroy() {
        floatingLogRefreshJob?.cancel()
        floatingLogRefreshJob = null
        floatingLogMinimized = false
        FloatingX.uninstall(LOGCAT_FLOATING_TAG)
        super.onDestroy()
    }
}
