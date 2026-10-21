package com.kulipai.luahook.feature.logcat

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.databinding.ActivityLogCatBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LogCatActivity : BaseActivity<ActivityLogCatBinding>() {

    private companion object {
        const val LOGCAT_CACHE = "cache"
        const val LOGCAT_CLEAR_TIME = "logClearTime"
        const val LOGCAT_REFRESH_INTERVAL = 1_000L
    }

    private val logHistory = LogHistory()
    private lateinit var adapter: LogAdapter
    private var lastError: String? = null

    override fun inflateBinding(inflater: LayoutInflater): ActivityLogCatBinding {
        return ActivityLogCatBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.LogRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = LogAdapter(logHistory.entries.toMutableList(), isSelectableMode = true)
        binding.LogRecyclerView.adapter = adapter
        binding.LogRecyclerView.itemAnimator = null

        updateStatusView()
    }

    override fun initEvent() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.fab.setOnClickListener {
            triggerManualRefresh()
        }
    }

    override fun initData() {
        // 观察 ShellMode 状态流：就绪时自动刷新界面与日志
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppContainer.instance.shellExecutor.mode.collect { mode ->
                    updateStatusView()
                    if (mode != ShellMode.NONE) {
                        fetchLogsOnce()
                    }
                }
            }
        }

        // 页面处于前台（STARTED）时，启动 1 秒轮询实时获取新日志，退出前台自动停止
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    if (AppContainer.instance.shellExecutor.mode.value != ShellMode.NONE) {
                        fetchLogsOnce()
                    }
                    delay(LOGCAT_REFRESH_INTERVAL)
                }
            }
        }
    }

    private suspend fun fetchLogsOnce() {
        val clearTime = getSharedPreferences(LOGCAT_CACHE, Context.MODE_PRIVATE)
            .getString(LOGCAT_CLEAR_TIME, null)
        val result = AppContainer.instance.logcatRepository.readSystemLogsByTagSince("LuaXposed", clearTime)

        lastError = result.exceptionOrNull()?.message
        result.getOrNull()?.let { snapshot ->
            val appended = logHistory.appendSnapshot(snapshot)
            if (appended.isNotEmpty()) {
                val followLatest = adapter.itemCount == 0 || !binding.LogRecyclerView.canScrollVertically(1)
                adapter.appendLogs(appended)
                if (followLatest) {
                    binding.LogRecyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }
        updateStatusView()
    }

    private fun triggerManualRefresh() {
        lifecycleScope.launch {
            fetchLogsOnce()
            if (adapter.itemCount > 0) {
                binding.LogRecyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun updateStatusView() {
        val mode = AppContainer.instance.shellExecutor.mode.value
        val message = when {
            adapter.itemCount > 0 -> null
            mode == ShellMode.NONE -> getString(R.string.no_power)
            lastError != null -> getString(R.string.logcat_read_failed, lastError)
            else -> getString(R.string.logcat_empty)
        }
        binding.noPower.text = message
        binding.noPower.visibility = if (message == null) View.GONE else View.VISIBLE
        binding.LogRecyclerView.visibility = if (message == null) View.VISIBLE else View.INVISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 0, 0, getString(R.string.clear))
            ?.setIcon(R.drawable.cleaning_services_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> {
                val clearTime = AppContainer.instance.logcatRepository.getCurrentLogcatTimeFormat()
                getSharedPreferences(LOGCAT_CACHE, Context.MODE_PRIVATE).edit {
                    putString(LOGCAT_CLEAR_TIME, clearTime)
                }
                logHistory.clear()
                lastError = null
                adapter.clearLogs()
                updateStatusView()
                Toast.makeText(this, getString(R.string.clear_success), Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
