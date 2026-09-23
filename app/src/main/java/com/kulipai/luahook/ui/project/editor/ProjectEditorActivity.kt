package com.kulipai.luahook.ui.project.editor

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
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.core.shell.ShellResult
import com.kulipai.luahook.databinding.ActivityProjectEditorBinding
import com.kulipai.luahook.ui.logcat.LogCatActivity
import com.kulipai.luahook.ui.script.editor.SoraEditorDelegate.initLuaEditor
import com.kulipai.luahook.ui.script.editor.SymbolAdapter
import com.kulipai.luahook.ui.script.editor.ToolAdapter
import com.myopicmobile.textwarrior.common.AutoIndent
import com.myopicmobile.textwarrior.common.Flag
import com.myopicmobile.textwarrior.common.LuaParser
import org.luaj.Globals
import java.io.File

class ProjectEditorActivity : BaseActivity<ActivityProjectEditorBinding>() {

    private lateinit var projectName: String
    private lateinit var projectDir: String
    private var currentFile: String = "main.lua" // Relative path
    private val openFiles = mutableListOf<String>()

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
        projectDir = "${WorkspaceFileManager.DIR}${WorkspaceFileManager.Project}/$projectName"
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
                    // switchToFile: WorkspaceFileManager.read("${WorkspaceFileManager.Project}/$projectName/$filename")
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
    }

    // ... (rest of class)

    override fun initEvent() {
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Handle Back Press for Drawer
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
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
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.text?.let { filename ->
                    if (filename.toString() != currentFile) {
                        saveCurrentFile()
                        switchToFile(filename.toString())
                    }
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
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
        val result = ShellManager.shell("ls -F \"$currentExplorerPath\"")
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
        val input = view.findViewById<android.widget.TextView>(R.id.edit)
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
            if (WorkspaceFileManager.ensureDirectoryExists(fullPath)) {
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
            // WorkspaceFileManager.write expects path relative to DIR, starting with /
            val relDir = currentExplorerPath.removePrefix(WorkspaceFileManager.DIR)
            val relPath = "$relDir/$name"

            if (WorkspaceFileManager.write(relPath, "")) {
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
        if (!openFiles.contains(filename)) {
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
        switchToFile(filename)
    }

    private fun switchToFile(filename: String) {
        currentFile = filename
        val content =
            WorkspaceFileManager.read("${WorkspaceFileManager.Project}/$projectName/$filename")
        binding.editor.setText(content)
    }

    private fun saveCurrentFile() {
        WorkspaceFileManager.write(
            "${WorkspaceFileManager.Project}/$projectName/$currentFile",
            binding.editor.text.toString()
        )
    }

    private val exportLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            Toast.makeText(this, getString(R.string.msg_exporting), Toast.LENGTH_SHORT).show()
            Thread {
                val success = com.kulipai.luahook.core.project.ProjectManager.exportProject(
                    this,
                    projectName,
                    it
                )
                runOnUiThread {
                    if (success) Toast.makeText(
                        this,
                        getString(R.string.msg_export_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    else Toast.makeText(
                        this,
                        getString(R.string.msg_export_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.start()
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

        val outValue = android.util.TypedValue()
        theme.resolveAttribute(
            androidx.appcompat.R.attr.selectableItemBackgroundBorderless,
            outValue,
            true
        )
        runView.setBackgroundResource(outValue.resourceId)
        runView.contentDescription = getString(R.string.action_run)

        runView.setOnClickListener {
            saveCurrentFile()
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

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> { // Run
                saveCurrentFile()
                runProject()
                true
            }

            1 -> { // Save
                saveCurrentFile()
                Toast.makeText(this, getString(R.string.msg_saved_short), Toast.LENGTH_SHORT).show()
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

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showRunSelectionDialog() {
        val dialogView = android.widget.LinearLayout(this)
        dialogView.orientation = android.widget.LinearLayout.VERTICAL
        dialogView.setPadding(32, 24, 32, 0)

        // Manual Input
        val inputLayout = com.google.android.material.textfield.TextInputLayout(this)
        inputLayout.boxBackgroundMode =
            com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
        inputLayout.hint = getString(R.string.hint_package_name)

        val input = com.google.android.material.textfield.TextInputEditText(this)
        inputLayout.addView(input)
        dialogView.addView(inputLayout)

        // Scope Chips
        val chipGroup = ChipGroup(this)
        chipGroup.setPadding(0, 24, 0, 0)

        val scroll = ScrollView(this)
        scroll.addView(chipGroup)
        // Constrain height if needed?

        dialogView.addView(
            scroll, android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Load scopes
        Thread {
            val projects = com.kulipai.luahook.core.project.ProjectManager.getProjects()
            val project = projects.find { it.name == projectName }
            val scopes = project?.scope ?: emptyList()
            runOnUiThread {
                for (scope in scopes) {
                    if (scope != "all") {
                        val chip = com.google.android.material.chip.Chip(this)
                        chip.text = scope
                        chip.setOnClickListener {
                            input.setText(scope)
                        }
                        chipGroup.addView(chip)
                    }
                }
            }
        }.start()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_select_launch_app))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_run)) { _, _ ->
                val pkg = input.text.toString().trim()
                if (pkg.isNotEmpty()) {
                    saveCurrentFile()
                    runProject(pkg)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runProject(launcherPackage: String? = null) {
        // Run on IO to avoid main thread block if scanning projects
        Thread {
            val projects = com.kulipai.luahook.core.project.ProjectManager.getProjects()
            val project = projects.find { it.name == projectName }
            runOnUiThread {
                if (project != null) {
                    var pkgToLaunch = launcherPackage ?: project.launcher
                    if (pkgToLaunch.isEmpty()) {
                        if (project.scope.contains("all")) {
                            Toast.makeText(
                                this,
                                getString(R.string.msg_scope_all_no_launcher),
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        } else if (project.scope.isNotEmpty()) {
                            pkgToLaunch = project.scope[0]
                        }
                    }

                    if (pkgToLaunch.isNotEmpty()) {
                        try {
                            val launchIntent = packageManager.getLaunchIntentForPackage(pkgToLaunch)
                            if (launchIntent != null) {
                                // Force stop before launching
                                ShellManager.shell("am force-stop $pkgToLaunch")
                                Thread.sleep(300) // Small delay to ensure it stopped

//                                Toast.makeText(this, "Launching $pkgToLaunch...", Toast.LENGTH_SHORT).show()
                                startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    this,
                                    getString(R.string.msg_no_launch_intent, pkgToLaunch),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this,
                                getString(R.string.msg_launch_error, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_no_launcher_defined),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.msg_project_info_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        saveCurrentFile()
    }
}

data class FileItem(val name: String, val path: String, val isDirectory: Boolean)

class FileListAdapter(
    private val files: MutableList<FileItem>,
    private val onClick: (FileItem) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val textView: android.widget.TextView = view.findViewById(R.id.file_name)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
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
