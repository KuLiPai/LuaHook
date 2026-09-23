package com.kulipai.luahook.ui.script.editor.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doBeforeTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.databinding.ActivityAppsEditBinding
import com.kulipai.luahook.ui.logcat.LogCatActivity
import com.kulipai.luahook.ui.manual.Manual
import com.kulipai.luahook.ui.script.editor.SoraEditorDelegate.initLuaEditor
import com.kulipai.luahook.ui.script.editor.SymbolAdapter
import com.kulipai.luahook.ui.script.editor.ToolAdapter
import com.kulipai.luahook.ui.script.setting.ScriptSetActivity
import com.myopicmobile.textwarrior.common.AutoIndent
import com.myopicmobile.textwarrior.common.Flag
import com.myopicmobile.textwarrior.common.LuaParser
import io.github.rosemoe.sora.widget.EditorSearcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.luaj.Globals
import java.io.File

class AppsEdit : BaseActivity<ActivityAppsEditBinding>() {

    // 文件分享
    private val FILE_PROVIDER_AUTHORITY = "com.kulipai.luahook.fileprovider"

    // 全局变量
    private lateinit var currentPackageName: String
    private lateinit var scripName: String
    private lateinit var scriptDescription: String
    private var author: String = ""

    override fun onStop() {
        super.onStop()
        saveScript(binding.editor.text.toString())
    }

    override fun onPause() {
        super.onPause()
        saveScript(binding.editor.text.toString())
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityAppsEditBinding {
        return ActivityAppsEditBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)

        // 监听窗口边距变化（包括状态栏、导航栏、IME键盘）
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

        // soraEditor
        initLuaEditor(binding.editor, binding.errMessage)

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
        // 接收传递信息
        val intent = intent
        if (intent != null) {
            currentPackageName = intent.getStringExtra("packageName").toString()
            scriptDescription = intent.getStringExtra("scriptDescription").toString()
            scripName = intent.getStringExtra("scripName").toString()
            title = scripName
        }

        val scriptPath = "${WorkspaceFileManager.AppScript}/$currentPackageName/$scripName.lua"
        binding.editor.setText(WorkspaceFileManager.read(scriptPath), null)
    }

    override fun initEvent() {
        binding.fab.setOnClickListener {
            saveScript(binding.editor.text.toString())
            Toast.makeText(this, resources.getString(R.string.save_ok), Toast.LENGTH_SHORT).show()
        }
    }

    // 菜单
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

    // TODO)) 每条写注释或者换个写法
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> {
                saveScript(binding.editor.text.toString())
                ShellManager.shell("am force-stop $currentPackageName")
                launchApp(this, currentPackageName)
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
                try {
                    val editor = binding.editor
                    // 1. 保存当前状态
                    val cursorLine = editor.cursor.leftLine
                    val cursorColumn = editor.cursor.leftColumn
                    val currentScrollY = editor.offsetY // 获取当前滚动纵坐标

                    val luaCode = editor.text.toString()
                    LuaParser.lexer(luaCode, Globals(), Flag())
                    val formattedCode = AutoIndent.format(luaCode, 2)

                    // 获取 Content 对象
                    val content = editor.text

                    // 2. 使用 batchEdit 进行原子操作（关键步骤）
                    // 这告诉编辑器这一系列操作是一个整体，Undo 时会一步撤销回格式化前
                    content.beginBatchEdit()

                    // 删除全部内容 (从 0,0 到 最后一行,最后一列)
                    content.delete(0, 0, content.lineCount - 1, content.getColumnCount(content.lineCount - 1))

                    // 插入格式化后的代码
                    content.insert(0, 0, formattedCode)

                    // 结束编辑
                    content.endBatchEdit()

                    // 3. 恢复光标位置
                    val targetLine = cursorLine.coerceAtMost(content.lineCount - 1)
                    val targetCol = if (targetLine == cursorLine) cursorColumn else 0
                    editor.setSelection(targetLine, targetCol)

                    // 4. 强制恢复视角（解决概率性跳回第一行的问题）
                    if (!editor.scroller.isFinished) {
                        editor.scroller.forceFinished(true)
                    }
                    editor.scroller.startScroll(0, currentScrollY, 0, 0, 0)

                } catch (e: Exception) {
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
                    WorkspaceFileManager.DIR + WorkspaceFileManager.AppScript + "/" + currentPackageName + "/" + scripName + ".lua"
                )
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    private fun handleShare() {
        val prefs = getSharedPreferences("conf", MODE_PRIVATE)
        if (prefs.getString("author", "").isNullOrEmpty()) {
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
                        prefs.edit {
                            putString("author", edit.text.toString())
                            apply()
                        }
                        shareFileFromTmp(
                            this,
                            "/data/local/tmp/LuaHook/${WorkspaceFileManager.AppScript}/$currentPackageName/$scripName.lua"
                        )
                    }
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            shareFileFromTmp(
                this,
                "/data/local/tmp/LuaHook/${WorkspaceFileManager.AppScript}/$currentPackageName/$scripName.lua"
            )
        }
    }

    // TODO)) 封装起来调用
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

    // TODO)) 封装
    fun saveScript(script: String) {
        val path = WorkspaceFileManager.AppScript + "/" + currentPackageName + "/" + scripName + ".lua"
        WorkspaceFileManager.write(path, script)


    }

    // TODO)) 封装
    fun shareFileFromTmp(
        context: Context,
        sourceFilePath: String,
        title: String = resources.getString(R.string.share_doc),
        mimeType: String = "*/*"
    ) {
        (context as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
            val copiedFile: File? = try {
                val cacheDir = context.cacheDir
                val destinationFile = File(cacheDir, File(sourceFilePath).name)
                val relative = sourceFilePath.removePrefix(WorkspaceFileManager.DIR)
                val originalContent = WorkspaceFileManager.read(relative)
                author = getSharedPreferences("conf", MODE_PRIVATE).getString("author", "").toString()
                val headerContent = "-- name: $scripName\n-- descript: $scriptDescription\n-- package: $currentPackageName\n-- author: $author\n\n"
                val mergedContent = headerContent + originalContent
                destinationFile.writeText(mergedContent)
                destinationFile
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "文件复制或写入失败: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
                null
            }

            copiedFile?.let { fileToShare ->
                withContext(Dispatchers.Main) {
                    try {
                        val fileUri: Uri = getFileUri(context, fileToShare)
                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            type = mimeType
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, title)
                        if (shareIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(chooser)
                        } else {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.no_apps_share),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            resources.getString(R.string.errors_sharing) + "${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // TODO)) 封装
    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }



    // TODO)) 搜索内容空，上下寻找闪退
    private fun initSearchPanel() {
        // 监听搜索框文本变化
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    // search(text, options)
                    // 参数2 SearchOptions: (ignoreCase, normal/regex)
                    // 这里默认: 不区分大小写(false -> true才区分), 普通模式(false -> true才正则)
                    // 如果你想完全精确搜索，可以根据需求调整 SearchOptions
                    binding.editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
                } else {
                    binding.editor.searcher.stopSearch()
                }
            }
        })

        // 监听键盘的"搜索"按钮
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.editor.searcher.gotoNext()
                true
            } else {
                false
            }
        }

        // 上一个
        binding.btnSearchPrev.setOnClickListener {
            binding.editor.searcher.gotoPrevious()
        }

        // 下一个
        binding.btnSearchNext.setOnClickListener {
            binding.editor.searcher.gotoNext()
        }

        // 替换当前选中
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

        // 替换所有
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

        // 关闭面板
        binding.btnSearchClose.setOnClickListener {
            closeSearchPanel()
        }
    }

    private fun openSearchPanel() {
        binding.searchPanel.visibility = View.VISIBLE
        binding.etSearch.requestFocus()
        // 弹出键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)

        // 如果之前有选中的文本，自动填入搜索框
        if (binding.editor.cursor.isSelected) {
            val selectedText = binding.editor.text.substring(binding.editor.cursor.left, binding.editor.cursor.right)
            if (selectedText.length < 50 && !selectedText.contains("\n")) { // 限制长度防止填入大段代码
                binding.etSearch.setText(selectedText)
                // 移动光标到末尾
                binding.etSearch.setSelection(selectedText.length)
            }
        }
    }

    private fun closeSearchPanel() {
        binding.searchPanel.visibility = View.GONE
        binding.editor.searcher.stopSearch()
        // 关闭键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        // 焦点还给编辑器
        binding.editor.requestFocus()
    }



    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }
}