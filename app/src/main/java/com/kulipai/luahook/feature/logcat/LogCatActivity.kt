package com.kulipai.luahook.feature.logcat

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.databinding.ActivityLogCatBinding
import kotlinx.coroutines.launch

class LogCatActivity : BaseActivity<ActivityLogCatBinding>() {

    private lateinit var adapter: LogAdapter

    override fun inflateBinding(inflater: LayoutInflater): ActivityLogCatBinding {
        return ActivityLogCatBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)
        
        // Navigation buttons
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Binding IDs are guessed from original code:
        // LogRecyclerView -> binding.LogRecyclerView
        // toolbar -> binding.toolbar
        // noPower -> binding.noPower
        // fab -> binding.fab (refresh)
    }

    override fun initEvent() {
        // Back press handling
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
            lifecycleScope.launch {
                val logs = AppContainer.instance.logcatRepository.getSystemLogsByTagSince(
                    "LuaXposed",
                    getSharedPreferences("cache", MODE_PRIVATE).getString("logClearTime", null)
                )
                adapter.updateLogs(logs as MutableList<String>)
            }
        }
    }

    override fun initData() {
        if (AppContainer.instance.shellExecutor.mode.value != ShellMode.NONE) {
            lifecycleScope.launch {
                val logs = AppContainer.instance.logcatRepository.getSystemLogsByTagSince(
                    "LuaXposed",
                    getSharedPreferences("cache", MODE_PRIVATE).getString("logClearTime", null)
                )
                binding.LogRecyclerView.layoutManager = LinearLayoutManager(this@LogCatActivity, LinearLayoutManager.VERTICAL, false)
                adapter = LogAdapter(logs as MutableList<String>)
                binding.LogRecyclerView.adapter = adapter
            }
        } else {
            binding.noPower.visibility = View.VISIBLE
            binding.LogRecyclerView.visibility = View.INVISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 0, 0, "Clear")
            ?.setIcon(R.drawable.cleaning_services_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> {
                val clearTime = AppContainer.instance.logcatRepository.getCurrentLogcatTimeFormat()
                getSharedPreferences("cache", MODE_PRIVATE).edit().putString("logClearTime", clearTime).apply()
                lifecycleScope.launch {
                    val logs = AppContainer.instance.logcatRepository.getSystemLogsByTagSince(
                        "LuaXposed",
                        getSharedPreferences("cache", MODE_PRIVATE).getString("logClearTime", null)
                    )
                    adapter.updateLogs(logs as MutableList<String>)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
