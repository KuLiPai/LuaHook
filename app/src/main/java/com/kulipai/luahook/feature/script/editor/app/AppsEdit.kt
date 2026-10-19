package com.kulipai.luahook.feature.script.editor.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doBeforeTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.databinding.ActivityAppsEditBinding
import com.kulipai.luahook.feature.logcat.LogCatActivity
import com.kulipai.luahook.feature.manual.Manual
import com.kulipai.luahook.feature.script.editor.ScriptEditorEffect
import com.kulipai.luahook.feature.script.editor.ScriptEditorMode
import com.kulipai.luahook.feature.script.editor.ScriptEditorViewModel
import com.kulipai.luahook.feature.script.editor.SoraEditorDelegate.initLuaEditor
import com.kulipai.luahook.feature.script.editor.SymbolAdapter
import com.kulipai.luahook.feature.script.editor.ToolAdapter
import com.kulipai.luahook.feature.script.editor.formatLua
import com.kulipai.luahook.feature.script.setting.ScriptSetActivity
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.launch
import java.io.File

class AppsEdit : BaseActivity<ActivityAppsEditBinding>() {

    private val FILE_PROVIDER_AUTHORITY = "com.kulipai.luahook.fileprovider"

    private val viewModel: ScriptEditorViewModel by viewModels {
        ScriptEditorViewModel.provideFactory()
    }

    private lateinit var currentPackageName: String
    private lateinit var scripName: String
    private var scriptDescription: String = ""
    private var appName: String = ""

    override fun inflateBinding(inflater: LayoutInflater): ActivityAppsEditBinding {
        return ActivityAppsEditBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.symbolRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.symbolRecyclerView.adapter = SymbolAdapter(binding.editor)

        initLuaEditor(binding.editor, binding.errMessage, enableLegacySyntaxCheck = false)
        binding.editor.subscribeAlways<ContentChangeEvent> {
            viewModel.onContentChanged(binding.editor.text.toString())
        }

        val tool = listOf(
            R.string.gen_hook_code,
            R.string.funcSign,
        )

        binding.toolRec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.toolRec.adapter = ToolAdapter(tool, binding.editor, this)

        initSearchPanel()
    }

    override fun initData() {
        val intent = intent
        if (intent != null) {
            currentPackageName = intent.getStringExtra("packageName").orEmpty()
            scriptDescription = intent.getStringExtra("scriptDescription").orEmpty()
            scripName = intent.getStringExtra("scripName") ?: intent.getStringExtra("scriptName").orEmpty()
            appName = intent.getStringExtra("appName").orEmpty()
            title = scripName
        }

        val mode = ScriptEditorMode.AppSpecific(
            packageName = currentPackageName,
            scriptName = scripName,
            appName = appName,
            scriptDescription = scriptDescription
        )
        viewModel.init(mode)
    }

    override fun initEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.syntaxError != null) {
                        binding.errMessage.text = state.syntaxError
                        binding.errMessage.visibility = View.VISIBLE
                    } else {
                        binding.errMessage.visibility = View.GONE
                    }
                    if (state.title.isNotEmpty()) {
                        binding.toolbar.title = state.title
                        title = state.title
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is ScriptEditorEffect.InitContent -> {
                            binding.editor.setText(effect.content, null)
                        }
                        is ScriptEditorEffect.ReloadContent -> {
                            val cursorLine = binding.editor.cursor.leftLine
                            val cursorColumn = binding.editor.cursor.leftColumn
                            val currentScrollY = binding.editor.offsetY

                            binding.editor.setText(effect.content, null)

                            val content = binding.editor.text
                            if (content.lineCount > 0 && cursorLine >= 0) {
                                val targetLine = cursorLine.coerceIn(0, content.lineCount - 1)
                                val maxCol = content.getColumnCount(targetLine)
                                val safeCol = cursorColumn.coerceIn(0, maxCol)
                                binding.editor.setSelection(targetLine, safeCol)
                            }
                            try {
                                if (!binding.editor.scroller.isFinished) {
                                    binding.editor.scroller.forceFinished(true)
                                }
                                binding.editor.scroller.startScroll(0, currentScrollY, 0, 0, 0)
                            } catch (_: Exception) {}
                        }
                        is ScriptEditorEffect.FileConflict -> {
                            MaterialAlertDialogBuilder(this@AppsEdit)
                                .setTitle(getString(R.string.tips))
                                .setMessage(getString(R.string.msg_file_conflict))
                                .setPositiveButton(getString(R.string.action_reload_from_disk)) { _, _ ->
                                    viewModel.applyDiskContent(effect.diskContent, showToast = true)
                                }
                                .setNegativeButton(getString(R.string.action_overwrite_disk)) { _, _ ->
                                    viewModel.forceSave(binding.editor.text.toString())
                                }
                                .setNeutralButton(getString(R.string.cancel), null)
                                .show()
                        }
                        is ScriptEditorEffect.LaunchApp -> {
                            launchApp(this@AppsEdit, effect.packageName)
                        }
                        is ScriptEditorEffect.ShareFile -> {
                            sharePreparedFile(effect.file)
                        }
                        is ScriptEditorEffect.ShowToast -> {
                            Toast.makeText(this@AppsEdit, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is ScriptEditorEffect.ShowToastRes -> {
                            Toast.makeText(this@AppsEdit, getString(effect.resId), Toast.LENGTH_SHORT).show()
                        }
                        is ScriptEditorEffect.SaveSuccess -> {
                            Toast.makeText(this@AppsEdit, resources.getString(R.string.save_ok), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.fab.setOnClickListener {
            viewModel.save(binding.editor.text.toString(), showToast = true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::currentPackageName.isInitialized && ::scripName.isInitialized) {
            viewModel.checkExternalChange(binding.editor.text.toString())
        }
    }

    override fun onStop() {
        super.onStop()
        if (::currentPackageName.isInitialized && ::scripName.isInitialized) {
            if (viewModel.isContentModified(binding.editor.text.toString())) {
                viewModel.save(binding.editor.text.toString(), showToast = false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::currentPackageName.isInitialized && ::scripName.isInitialized) {
            if (viewModel.isContentModified(binding.editor.text.toString())) {
                viewModel.save(binding.editor.text.toString(), showToast = false)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 0, 0, "Run")?.setIcon(R.drawable.play_arrow_24px)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 1, 0, "Undo")?.setIcon(R.drawable.undo_24px)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 2, 0, "Redo")?.setIcon(R.drawable.redo_24px)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 3, 0, resources.getString(R.string.format))?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 4, 0, resources.getString(R.string.log))?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 5, 0, resources.getString(R.string.manual))?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 9, 0, resources.getString(R.string.search))?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 15, 0, resources.getString(R.string.share))?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 25, 0, resources.getString(R.string.setting))?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> {
                viewModel.runScript(binding.editor.text.toString())
                true
            }
            1 -> {
                binding.editor.undo()
                true
            }
            2 -> {
                binding.editor.redo()
                true
            }
            3 -> {
                binding.editor.formatLua(2).onFailure { e ->
                    Toast.makeText(this, "Format failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            4 -> {
                val intent = Intent(this, LogCatActivity::class.java)
                startActivity(intent)
                true
            }
            5 -> {
                val intent = Intent(this, Manual::class.java)
                startActivity(intent)
                true
            }
            9 -> {
                if (binding.searchPanel.isVisible) {
                    closeSearchPanel()
                } else {
                    openSearchPanel()
                }
                true
            }
            15 -> {
                handleShare()
                true
            }
            25 -> {
                val intent = Intent(this, ScriptSetActivity::class.java)
                intent.putExtra(
                    "path",
                    "/data/local/tmp/LuaHook/AppScript/$currentPackageName/$scripName.lua"
                )
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    private fun handleShare() {
        val prefs = getSharedPreferences("conf", MODE_PRIVATE)
        val author = prefs.getString("author", "").orEmpty()
        if (author.isEmpty()) {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null)
            val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
            val edit = view.findViewById<TextInputEditText>(R.id.edit)
            inputLayout.hint = "作者名称"

            edit.doBeforeTextChanged { _, _, _, _ -> edit.error = null }

            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.author_info))
                .setView(view)
                .setPositiveButton(resources.getString(R.string.sure)) { _, _ ->
                    if (edit.text.isNullOrEmpty()) {
                        edit.error = resources.getString(R.string.input_id)
                    } else {
                        val inputAuthor = edit.text.toString()
                        prefs.edit {
                            putString("author", inputAuthor)
                            apply()
                        }
                        viewModel.prepareShare(cacheDir, inputAuthor)
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            viewModel.prepareShare(cacheDir, author)
        }
    }

    private fun sharePreparedFile(fileToShare: File) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, fileToShare)
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, resources.getString(R.string.share_doc))
            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.no_apps_share),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                resources.getString(R.string.errors_sharing) + "${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun launchApp(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            try {
                context.startActivity(launchIntent)
                Log.i("LaunchApp", "Successfully launched $packageName")
                true
            } catch (e: Exception) {
                Log.e("LaunchApp", "Failed to launch $packageName: ${e.message}")
                false
            }
        } else {
            Log.w("LaunchApp", "Launch intent not found for $packageName")
            false
        }
    }

    fun saveScript(script: String) {
        viewModel.save(script, showToast = true)
    }

    private fun searchNext() {
        val text = binding.etSearch.text?.toString().orEmpty()
        if (text.isEmpty()) return
        if (!binding.editor.searcher.hasQuery()) {
            binding.editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
        }
        try {
            binding.editor.searcher.gotoNext()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchPrevious() {
        val text = binding.etSearch.text?.toString().orEmpty()
        if (text.isEmpty()) return
        if (!binding.editor.searcher.hasQuery()) {
            binding.editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
        }
        try {
            binding.editor.searcher.gotoPrevious()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearchPanel() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    binding.editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
                } else {
                    binding.editor.searcher.stopSearch()
                }
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                searchNext()
                true
            } else {
                false
            }
        }

        binding.btnSearchPrev.setOnClickListener {
            searchPrevious()
        }

        binding.btnSearchNext.setOnClickListener {
            searchNext()
        }

        binding.btnReplaceOne.setOnClickListener {
            if (binding.editor.searcher.hasQuery()) {
                val replaceText = binding.etReplace.text.toString()
                try {
                    binding.editor.searcher.replaceCurrentMatch(replaceText)
                } catch (e: Exception) {
                    Toast.makeText(this, "Replace failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnReplaceAll.setOnClickListener {
            if (binding.editor.searcher.hasQuery()) {
                val replaceText = binding.etReplace.text.toString()
                try {
                    binding.editor.searcher.replaceAll(replaceText)
                } catch (e: Exception) {
                    Toast.makeText(this, "Replace All failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnSearchClose.setOnClickListener {
            closeSearchPanel()
        }
    }

    private fun openSearchPanel() {
        binding.searchPanel.visibility = View.VISIBLE
        binding.etSearch.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)

        if (binding.editor.cursor.isSelected) {
            val selectedText = binding.editor.text.substring(binding.editor.cursor.left, binding.editor.cursor.right)
            if (selectedText.length < 50 && !selectedText.contains("\n")) {
                binding.etSearch.setText(selectedText)
                binding.etSearch.setSelection(selectedText.length)
            }
        }
    }

    private fun closeSearchPanel() {
        binding.searchPanel.visibility = View.GONE
        binding.editor.searcher.stopSearch()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.editor.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }
}