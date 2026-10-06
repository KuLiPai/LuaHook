package com.kulipai.luahook.ui.script.selector

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kulipai.luahook.R
import com.kulipai.luahook.app.MyApplication
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.data.model.AppInfo
import com.kulipai.luahook.databinding.ActivitySelectAppsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScopeSelectorActivity : BaseActivity<ActivitySelectAppsBinding>() {

    private var selectApps = mutableListOf<String>()
    private var searchJob: Job? = null
    private var allApps: List<AppInfo> = emptyList()
    private var availableAppsToShow: List<AppInfo> = emptyList()
    private lateinit var adapter: SelectAppsAdapter
    private var isLoaded = false
    private var showSystemApps = false
    private var currentSortMode = AppSortMode.NAME

    override fun inflateBinding(inflater: LayoutInflater): ActivitySelectAppsBinding {
        return ActivitySelectAppsBinding.inflate(inflater)
    }

    override fun initView() {
        DynamicColors.applyToActivityIfAvailable(this)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Scope"

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        binding.rec.layoutManager = LinearLayoutManager(this)
        binding.rec.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = (88 * resources.displayMetrics.density).toInt()
                }
            }
        })
    }

    override fun initData() {
        showSystemApps = SelectorPrefs.isShowSystemApps(this)
        currentSortMode = SelectorPrefs.getSortMode(this)

        // Load passed selection
        val passedSelection = intent.getStringArrayListExtra("current_scope")
        if (passedSelection != null) {
            selectApps = passedSelection.toMutableList()
        }

        adapter = SelectAppsAdapter(emptyList(), this, selectApps)
        binding.rec.adapter = adapter

        val app = application as MyApplication
        lifecycleScope.launch {
            allApps = app.getAppListAsync()
            refreshAppList()
            isLoaded = true
        }
    }

    override fun initEvent() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.searchbar.setOnClickListener {
            binding.searchBarTextView.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchBarTextView, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.searchBarTextView.doAfterTextChanged { s ->
            searchJob?.cancel()
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                if (isLoaded) {
                    delay(100)
                    filterAppList(s.toString().trim())
                }
            }
        }

        binding.clearText.setOnClickListener {
            binding.searchBarTextView.setText("")
            binding.clearText.visibility = View.INVISIBLE
        }

        binding.fab.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putStringArrayListExtra("selected_scope", ArrayList(selectApps))
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun refreshAppList() {
        availableAppsToShow = allApps.filter { appInfo ->
            showSystemApps || !appInfo.isSystemApp
        }.sortApps(currentSortMode)
        filterAppList(binding.searchBarTextView.text?.toString()?.trim().orEmpty())
    }

    private fun filterAppList(query: String) {
        val filteredList = if (query.isEmpty()) {
            binding.clearText.visibility = View.INVISIBLE
            availableAppsToShow
        } else {
            binding.clearText.visibility = View.VISIBLE
            availableAppsToShow.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filteredList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_select_apps, menu)
        menu?.findItem(R.id.action_show_system)?.isChecked = showSystemApps
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_system -> {
                showSystemApps = !showSystemApps
                SelectorPrefs.setShowSystemApps(this, showSystemApps)
                item.isChecked = showSystemApps
                refreshAppList()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_name),
            getString(R.string.sort_by_install_time),
            getString(R.string.sort_by_update_time),
            getString(R.string.sort_by_package_name)
        )
        val currentIndex = currentSortMode.value

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort_mode)
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                val newMode = AppSortMode.fromValue(which)
                if (newMode != currentSortMode) {
                    currentSortMode = newMode
                    SelectorPrefs.setSortMode(this, currentSortMode)
                    refreshAppList()
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
