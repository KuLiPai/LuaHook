package com.kulipai.luahook.feature.project.editor

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.shell.ShellResult
import com.kulipai.luahook.databinding.ActivityProjectEditorBinding
import com.kulipai.luahook.feature.logcat.LogCatActivity
import com.kulipai.luahook.feature.project.pack.ProjectPackActivity
import com.kulipai.luahook.feature.script.editor.SoraEditorDelegate.initLuaEditor
import com.kulipai.luahook.feature.script.editor.SymbolAdapter
import com.kulipai.luahook.feature.script.editor.ToolAdapter
import com.myopicmobile.textwarrior.common.AutoIndent
import com.myopicmobile.textwarrior.common.Flag
import com.myopicmobile.textwarrior.common.LuaParser
import org.luaj.Globals
import java.io.File
import android.os.Build
import android.os.FileObserver
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.core.file.WorkspaceEventManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun String.normalizeContent(): String = replace("\r\n", "\n").replace("\r", "\n").trimEnd('\r', '\n')

class ProjectEditorActivity : BaseActivity<ActivityProjectEditorBinding>() {

    private lateinit var projectName: String
    private lateinit var projectDir: String
    private var currentFile: String = "" // Relative path
    private val openFiles = mutableListOf<String>()

    private var lastSavedContent: String = ""
    private var lastSelfSaveTime: Long = 0L
    private var conflictDialog: AlertDialog? = null

    private var projectFileObserver: FileObserver? = null
    private val fileObserverEvents = MutableSharedFlow<String?>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private lateinit var fileAdapter: FileListAdapter

    override fun inflateBinding(inflater: LayoutInflater): ActivityProjectEditorBinding {
        return ActivityProjectEditorBinding.inflate(inflater)
    }

    override fun initView() {
        // Let DrawerLayout handle insets via fitsSystemWindows

        setSupportActionBar(binding.toolbar)

        // 监听窗口边距变化（包括状态栏、导航栏、IME键盘）
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            binding.navView.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }


        binding.symbolRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.symbolRecyclerView.adapter = SymbolAdapter(binding.editor)


        val tool = listOf(
            R.string.gen_hook_code,
            R.string.funcSign,

        )

        binding.toolRec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.toolRec.adapter = ToolAdapter(tool, binding.editor, this)

        // SoraEditor integration
        initLuaEditor(binding.editor, binding.errorMessage)

        binding.fileListRecycler.layoutManager = LinearLayoutManager(this)
    }

    private var currentExplorerPath: String = ""

    override fun initData() {
        projectName = intent.getStringExtra("project_name") ?: ""
        if (projectName.isEmpty()) finish()

        title = projectName
        binding.toolbar.title = projectName
        projectDir = "${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$projectName"
        currentExplorerPath = projectDir

        fileAdapter = FileListAdapter(mutableListOf()) { file ->
            if (file.isDirectory) {
                if (file.name == "..") {
                    // Go up
                    val parent = File(currentExplorerPath).parentFile
                    if (parent != null && parent.absolutePath.startsWith(projectDir)) {
                        currentExplorerPath = parent.absolutePath
                        loadFileList()
                    } else {
                        // Already at root or error
                        currentExplorerPath = projectDir
                        loadFileList()
                    }
                } else {
                    // Go down
                    currentExplorerPath = file.path
                    loadFileList()
                }
            } else {
                // Check file type
                if (file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".jpeg")) {
                    showImagePreview(file)
                } else {
                    // Open for editing
                    // Calculate relative path for openFile/switchToFile
                    // file.path is absolute. switchToFile expects relative to projectDir? (Actually initData calls openFile("main.lua"))
                    // Let's check openFile logic. It stores filename. switchToFile constructs path using projectName.
                    // The current openFile/switchToFile implementation assumes files are in root of project?
                    // switchToFile: AppContainer.instance.workspaceStorage.read("${WorkspaceStorage.Project}/$projectName/$filename")
                    // This assumes filename is relative to project root.

                    // We need to support subclasses.
                    // Calculate relative path.
                    val relPath = if (file.path.startsWith(projectDir)) {
                        file.path.substring(projectDir.length + 1)
                    } else {
                        file.name
                    }
                    openFile(relPath)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
        }
        binding.fileListRecycler.adapter = fileAdapter

        loadFileList()
        openFile("main.lua")
        startFileObserver()
    }

    // ... (rest of class)

    override fun initEvent() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 监听工作区 MCP / 内存文件流变更
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WorkspaceEventManager.fileEvents.collect { relativePath ->
                    if (System.currentTimeMillis() - lastSelfSaveTime < 1000L) {
                        return@collect
                    }
                    val projectPrefix = "${WorkspaceStorage.Project}/$projectName/"
                    if (relativePath.startsWith(projectPrefix) || relativePath == "${WorkspaceStorage.Project}/$projectName") {
                        loadFileList()
                        val currentFileRelativePath = "$projectPrefix$currentFile"
                        if (relativePath == currentFileRelativePath) {
                            checkAndReloadCurrentFile(quiet = false)
                        }
                    }
                }
            }
        }

        // 监听外部原生文件观察器事件（基于 Flow 防抖处理）
        @OptIn(FlowPreview::class)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                fileObserverEvents
                    .debounce(200)
                    .collect { path ->
                        if (System.currentTimeMillis() - lastSelfSaveTime < 1000L) {
                            return@collect
                        }
                        loadFileList()
                        val isCurrent = path != null && (path == currentFile || path.endsWith("/$currentFile") || path == File(currentFile).name)
                        if (isCurrent) {
                            checkAndReloadCurrentFile(quiet = false)
                        }
                    }
            }
        }

        // Handle Back Press for Drawer
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })

        // Find header in drawer to set refresh listener
        // Bind Sidebar Buttons
        binding.btnNewFile.setOnClickListener {
            showCreateDialog(isFolder = false)
        }
        binding.btnNewFolder.setOnClickListener {
            showCreateDialog(isFolder = true)
        }

        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.text?.let { filename ->
                    if (filename.toString() != currentFile) {
                        if (isContentModified()) {
                            saveCurrentFile(force = true)
                        }
                        switchToFile(filename.toString())
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadFileList() {
        // Use currentExplorerPath
        val dir = File(currentExplorerPath)
        val files = mutableListOf<FileItem>()

        // Add ".." if not at root
        if (currentExplorerPath != projectDir && currentExplorerPath.startsWith(projectDir)) {
            files.add(FileItem("..", File(currentExplorerPath).parent ?: projectDir, true))
        }

        // Try Shell ls -F
        val result = AppContainer.instance.shellExecutor.exec("ls -F \"$currentExplorerPath\"")
        if (result is ShellResult.Success) {
            val lines = result.stdout.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    var isDir = false
                    var name = trimmed

                    if (name.endsWith("/")) {
                        isDir = true
                        name = name.dropLast(1)
                    } else if (name.endsWith("*") || name.endsWith("@") || name.endsWith("=") || name.endsWith(
                            "|"
                        )
                    ) {
                        name = name.dropLast(1)
                    }

                    files.add(FileItem(name, "$currentExplorerPath/$name", isDir))
                }
            }
        } else {
            // Fallback to Java File if shell fails (unlikely if root/shizuku, but maybe permission denied on partial path)
            dir.listFiles()?.forEach {
                files.add(FileItem(it.name, it.path, it.isDirectory))
            }
        }

        // Sort: Directories first, then files
        val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        fileAdapter.updateData(sorted)
    }

    private fun showImagePreview(file: FileItem) {
        val dialog = MaterialAlertDialogBuilder(this)
        val imageView = ImageView(this)
        val bitmap = BitmapFactory.decodeFile(file.path)
        imageView.setImageBitmap(bitmap)
        imageView.adjustViewBounds = true
        dialog.setView(imageView)
        dialog.setPositiveButton(getString(R.string.action_close), null)
        dialog.show()
    }

    private fun showCreateDialog(isFolder: Boolean) {
        val title =
            if (isFolder) getString(R.string.title_create_folder) else getString(R.string.title_create_file)

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit, null)
        val input = view.findViewById<TextView>(R.id.edit)
        input.hint = getString(R.string.hint_name)

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(getString(R.string.action_create)) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createItem(name, isFolder)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createItem(name: String, isFolder: Boolean) {
        if (isFolder) {
            val fullPath = "$currentExplorerPath/$name"
            if (AppContainer.instance.workspaceStorage.ensureDirectoryExists(fullPath)) {
                Toast.makeText(this, getString(R.string.msg_folder_created), Toast.LENGTH_SHORT)
                    .show()
                loadFileList()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.msg_folder_create_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // workspaceStorage.write expects path relative to DIR, starting with /
            val relDir = currentExplorerPath.removePrefix(WorkspaceStorage.DIR)
            val relPath = "$relDir/$name"

            if (AppContainer.instance.workspaceStorage.write(relPath, "")) {
                Toast.makeText(this, getString(R.string.msg_file_created), Toast.LENGTH_SHORT)
                    .show()
                loadFileList()
            } else {
                Toast.makeText(this, getString(R.string.msg_file_create_failed), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun refreshFileList() {
        Toast.makeText(this, getString(R.string.msg_refreshing_files), Toast.LENGTH_SHORT).show()
        loadFileList()
    }

    private fun openFile(filename: String) {
        val isNewTab = !openFiles.contains(filename)
        if (isNewTab) {
            openFiles.add(filename)
            val tab = binding.tabLayout.newTab().setText(filename)
            binding.tabLayout.addTab(tab)
            tab.select()
        } else {
            for (i in 0 until binding.tabLayout.tabCount) {
                if (binding.tabLayout.getTabAt(i)?.text == filename) {
                    binding.tabLayout.getTabAt(i)?.select()
                    break
                }
            }
        }
        if (currentFile != filename) {
            switchToFile(filename)
        }
    }

    private fun isContentModified(): Boolean {
        return binding.editor.text.toString() != lastSavedContent
    }

    private fun switchToFile(filename: String) {
        currentFile = filename
        val content =
            AppContainer.instance.workspaceStorage.read("${WorkspaceStorage.Project}/$projectName/$filename")
        binding.editor.setText(content)
        lastSavedContent = binding.editor.text.toString()
    }

    private fun saveCurrentFile(force: Boolean = false): Boolean {
        if (currentFile.isEmpty()) return true
        val currentText = binding.editor.text.toString()
        if (!force && !isContentModified()) {
            return true
        }

        // 严禁在待写入内容为空而磁盘原文件非空时静默覆盖（AGENTS.md 防空覆盖卫语句）
        if (currentText.isEmpty() && lastSavedContent.isNotEmpty()) {
            return false
        }

        val path = "${WorkspaceStorage.Project}/$projectName/$currentFile"
        val diskContent = AppContainer.instance.workspaceStorage.read(path)

        val normalizedDisk = diskContent.normalizeContent()
        val normalizedCurrent = currentText.normalizeContent()
        val normalizedSaved = lastSavedContent.normalizeContent()

        // 磁盘内容已与当前编辑器内存完全一致，无需重复写入
        if (normalizedDisk == normalizedCurrent) {
            lastSavedContent = currentText
            return true
        }

        // 非强制保存下，若磁盘内容被外部修改（既不等于基线，也不等于当前内存），提示冲突
        if (!force && normalizedDisk != normalizedSaved && normalizedDisk != normalizedCurrent) {
            showConflictDialog(diskContent)
            return false
        }

        val success = AppContainer.instance.workspaceStorage.write(path, currentText)
        if (success) {
            lastSavedContent = currentText
            lastSelfSaveTime = System.currentTimeMillis()
        }
        return success
    }

    private fun showConflictDialog(diskContent: String) {
        if (isFinishing || isDestroyed) return
        if (conflictDialog?.isShowing == true) return
        conflictDialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_file_conflict))
            .setMessage(getString(R.string.msg_file_conflict))
            .setPositiveButton(getString(R.string.action_reload_from_disk)) { _, _ ->
                applyNewContent(diskContent, showToast = true)
            }
            .setNegativeButton(getString(R.string.action_overwrite_disk)) { _, _ ->
                saveCurrentFile(force = true)
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .show()
    }

    private fun checkAndReloadCurrentFile(quiet: Boolean = false) {
        if (currentFile.isEmpty()) return
        val path = "${WorkspaceStorage.Project}/$projectName/$currentFile"
        val diskContent = AppContainer.instance.workspaceStorage.read(path)

        val normalizedDisk = diskContent.normalizeContent()
        val normalizedCurrent = binding.editor.text.toString().normalizeContent()
        val normalizedSaved = lastSavedContent.normalizeContent()

        // 1. 若磁盘内容与当前编辑器完全一致，说明已同步，直接更新基线
        if (normalizedDisk == normalizedCurrent) {
            lastSavedContent = binding.editor.text.toString()
            return
        }

        // 2. 若磁盘内容与基线一致，说明外部未做任何修改
        if (normalizedDisk == normalizedSaved) {
            return
        }

        // 3. 磁盘发生了外部变更：
        // 若当前编辑器无未保存修改，安全平滑重载
        if (!isContentModified()) {
            applyNewContent(diskContent, showToast = !quiet)
        } else {
            // 当前编辑器存在未保存修改，且外部内容与当前内容不一致，才弹窗询问
            showConflictDialog(diskContent)
        }
    }

    private fun applyNewContent(newContent: String, showToast: Boolean = false) {
        val editor = binding.editor
        val cursorLine = editor.cursor.leftLine
        val cursorColumn = editor.cursor.leftColumn
        val currentScrollY = editor.offsetY

        editor.setText(newContent)
        lastSavedContent = editor.text.toString()

        val content = editor.text
        if (content.lineCount > 0 && cursorLine >= 0) {
            val targetLine = cursorLine.coerceIn(0, content.lineCount - 1)
            val maxCol = content.getColumnCount(targetLine)
            val safeCol = cursorColumn.coerceIn(0, maxCol)
            editor.setSelection(targetLine, safeCol)
        }

        try {
            if (!editor.scroller.isFinished) {
                editor.scroller.forceFinished(true)
            }
            editor.scroller.startScroll(0, currentScrollY, 0, 0, 0)
        } catch (_: Exception) {}

        if (showToast) {
            Toast.makeText(this, getString(R.string.msg_file_reloaded_from_disk), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFileObserver() {
        stopFileObserver()
        try {
            val mask = FileObserver.CLOSE_WRITE or FileObserver.CREATE or FileObserver.DELETE or FileObserver.MOVED_TO or FileObserver.MOVED_FROM
            projectFileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                object : FileObserver(File(projectDir), mask) {
                    override fun onEvent(event: Int, path: String?) {
                        fileObserverEvents.tryEmit(path)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                object : FileObserver(projectDir, mask) {
                    override fun onEvent(event: Int, path: String?) {
                        fileObserverEvents.tryEmit(path)
                    }
                }
            }
            projectFileObserver?.startWatching()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFileObserver() {
        try {
            projectFileObserver?.stopWatching()
        } catch (_: Exception) {}
        projectFileObserver = null
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            Toast.makeText(this, getString(R.string.msg_exporting), Toast.LENGTH_SHORT).show()
            lifecycleScope.launch(Dispatchers.IO) {
                val success = AppContainer.instance.projectRepository.exportProject(
                    this@ProjectEditorActivity,
                    projectName,
                    it
                )
                withContext(Dispatchers.Main) {
                    if (success) Toast.makeText(
                        this@ProjectEditorActivity,
                        getString(R.string.msg_export_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    else Toast.makeText(
                        this@ProjectEditorActivity,
                        getString(R.string.msg_export_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Run Button with Custom View for Long Press
        val runItem = menu?.add(0, 0, 0, getString(R.string.action_run))
        runItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        val runView = ImageView(this)
        runView.setImageResource(R.drawable.play_arrow_24px)
        val padding = (8 * resources.displayMetrics.density).toInt()
        runView.setPadding(padding, padding, padding, padding)

        val outValue = TypedValue()
        theme.resolveAttribute(
            androidx.appcompat.R.attr.selectableItemBackgroundBorderless,
            outValue,
            true
        )
        runView.setBackgroundResource(outValue.resourceId)
        runView.contentDescription = getString(R.string.action_run)

        runView.setOnClickListener {
            if (isContentModified()) {
                saveCurrentFile(force = true)
            }
            runProject()
        }
        runView.setOnLongClickListener {
            showRunSelectionDialog()
            true
        }

        runItem?.actionView = runView

        menu?.add(0, 1, 0, getString(R.string.action_save))?.setIcon(R.drawable.save_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 2, 0, getString(R.string.action_undo))?.setIcon(R.drawable.undo_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 3, 0, getString(R.string.action_redo))?.setIcon(R.drawable.redo_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu?.add(0, 4, 0, getString(R.string.action_format))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 5, 0, getString(R.string.action_logcat))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 6, 0, getString(R.string.action_export))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 7, 0, getString(R.string.action_pack_apk))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> { // Run
                if (isContentModified()) {
                    saveCurrentFile(force = true)
                }
                runProject()
                true
            }

            1 -> { // Save
                val saved = saveCurrentFile(force = true)
                if (saved) {
                    Toast.makeText(this, getString(R.string.msg_saved_short), Toast.LENGTH_SHORT).show()
                }
                true
            }

            2 -> {
                binding.editor.undo(); true
            }

            3 -> {
                binding.editor.redo(); true
            }

            4 -> {
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
                    content.delete(
                        0,
                        0,
                        content.lineCount - 1,
                        content.getColumnCount(content.lineCount - 1)
                    )

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
                    Toast.makeText(
                        this,
                        getString(R.string.msg_format_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                true
            }

            5 -> {
                val intent = Intent(this, LogCatActivity::class.java)
                startActivity(intent)
                true
            }

            6 -> {
                exportLauncher.launch("$projectName.zip")
                true
            }

            7 -> {
                if (isContentModified()) {
                    saveCurrentFile(force = true)
                }
                val intent = Intent(this, ProjectPackActivity::class.java)
                intent.putExtra("project_name", projectName)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showRunSelectionDialog() {
        val dialogView = LinearLayout(this)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(32, 24, 32, 0)

        // Manual Input
        val inputLayout = TextInputLayout(this)
        inputLayout.boxBackgroundMode =
            TextInputLayout.BOX_BACKGROUND_OUTLINE
        inputLayout.hint = getString(R.string.hint_package_name)

        val input = TextInputEditText(this)
        inputLayout.addView(input)
        dialogView.addView(inputLayout)

        // Scope Chips
        val chipGroup = ChipGroup(this)
        chipGroup.setPadding(0, 24, 0, 0)

        val scroll = ScrollView(this)
        scroll.addView(chipGroup)
        // Constrain height if needed?

        dialogView.addView(
            scroll, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Load scopes
        lifecycleScope.launch(Dispatchers.IO) {
            val projects = AppContainer.instance.projectRepository.getProjects()
            val project = projects.find { it.name == projectName }
            val scopes = project?.scope ?: emptyList()
            withContext(Dispatchers.Main) {
                for (scope in scopes) {
                    if (scope != "all") {
                        val chip = Chip(this@ProjectEditorActivity)
                        chip.text = scope
                        chip.setOnClickListener {
                            input.setText(scope)
                        }
                        chipGroup.addView(chip)
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_select_launch_app))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_run)) { _, _ ->
                val pkg = input.text.toString().trim()
                if (pkg.isNotEmpty()) {
                    if (isContentModified()) {
                        saveCurrentFile(force = true)
                    }
                    runProject(pkg)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runProject(launcherPackage: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            val projects = AppContainer.instance.projectRepository.getProjects()
            val project = projects.find { it.name == projectName }
            withContext(Dispatchers.Main) {
                if (project != null) {
                    var pkgToLaunch = launcherPackage ?: project.launcher
                    if (pkgToLaunch.isEmpty()) {
                        if (project.scope.contains("all")) {
                            Toast.makeText(
                                this@ProjectEditorActivity,
                                getString(R.string.msg_scope_all_no_launcher),
                                Toast.LENGTH_LONG
                            ).show()
                            return@withContext
                        } else if (project.scope.isNotEmpty()) {
                            pkgToLaunch = project.scope[0]
                        }
                    }

                    if (pkgToLaunch.isNotEmpty()) {
                        try {
                            val launchIntent = packageManager.getLaunchIntentForPackage(pkgToLaunch)
                            if (launchIntent != null) {
                                withContext(Dispatchers.IO) {
                                    AppContainer.instance.shellExecutor.exec("am force-stop $pkgToLaunch")
                                    delay(300)
                                }
                                startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    this@ProjectEditorActivity,
                                    getString(R.string.msg_no_launch_intent, pkgToLaunch),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@ProjectEditorActivity,
                                getString(R.string.msg_launch_error, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@ProjectEditorActivity,
                            getString(R.string.msg_no_launcher_defined),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@ProjectEditorActivity,
                        getString(R.string.msg_project_info_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFileList()
        checkAndReloadCurrentFile(quiet = true)
    }

    override fun onPause() {
        super.onPause()
        val currentText = binding.editor.text.toString()
        if (isContentModified()) {
            if (currentText.isEmpty() && lastSavedContent.isNotEmpty()) {
                return
            }
            saveCurrentFile(force = true)
        }
    }

    override fun onDestroy() {
        stopFileObserver()
        conflictDialog?.dismiss()
        conflictDialog = null
        super.onDestroy()
    }
}

data class FileItem(val name: String, val path: String, val isDirectory: Boolean)

class FileListAdapter(
    private val files: MutableList<FileItem>,
    private val onClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    class ViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.file_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = files[position]
        holder.textView.text = item.name

        val iconRes = when {
            item.isDirectory -> R.drawable.folder
            item.name.endsWith(".lua") -> R.drawable.lua
            item.name.endsWith(".json") -> R.drawable.json
            item.name.endsWith(".png") || item.name.endsWith(".jpg") || item.name.endsWith(".jpeg") -> R.drawable.description_24px
            else -> R.drawable.file_open_24px
        }

        holder.textView.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        holder.itemView.setOnClickListener { onClick(item) }
    }


    override fun getItemCount() = files.size

    fun updateData(newFiles: List<FileItem>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }
}
