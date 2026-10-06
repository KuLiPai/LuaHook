package com.kulipai.luahook.ui.script.editor.global

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kulipai.luahook.R
import com.kulipai.luahook.ui.manual.Manual
import com.kulipai.luahook.ui.script.editor.SymbolAdapter
import com.kulipai.luahook.ui.logcat.LogCatActivity
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.ui.script.editor.SoraEditorDelegate.initLuaEditor
import com.myopicmobile.textwarrior.common.AutoIndent
import com.myopicmobile.textwarrior.common.Flag
import com.myopicmobile.textwarrior.common.LuaParser
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import org.luaj.Globals

class EditActivity : AppCompatActivity() {


    // 1. 懒加载搜索视图控件
    private val searchPanel: View by lazy { findViewById(R.id.search_panel) }
    private val etSearch: EditText by lazy { findViewById(R.id.et_search) }
    private val etReplace: EditText by lazy { findViewById(R.id.et_replace) }
    private val btnPrev: View by lazy { findViewById(R.id.btn_search_prev) }
    private val btnNext: View by lazy { findViewById(R.id.btn_search_next) }
    private val btnClose: View by lazy { findViewById(R.id.btn_search_close) }
    private val btnReplaceOne: View by lazy { findViewById(R.id.btn_replace_one) }
    private val btnReplaceAll: View by lazy { findViewById(R.id.btn_replace_all) }

    private val errMessage: TextView by lazy { findViewById(R.id.errMessage) }


    private fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName!!
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }


    fun getDynamicColor(context: Context, @AttrRes colorAttributeResId: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(colorAttributeResId, typedValue, true)
        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(context, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }


    @SuppressLint("SetTextI18n")
    private fun showLsposedInfoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_info, null)

        val appLogoImageView: ImageView = view.findViewById(R.id.app_logo)
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val appVersionTextView: TextView = view.findViewById(R.id.app_version)
        val appDescriptionTextView: TextView = view.findViewById(R.id.app_description)

        // 设置应用信息
        appLogoImageView.setImageResource(R.drawable.luna)
        appNameTextView.text = "LuaHook"
        appVersionTextView.text = getAppVersionName(this)

        // 构建包含可点击链接的 SpannableString (与之前的示例代码相同)
        val descriptionText =
            resources.getString(R.string.find_us) + "\n" + resources.getString(R.string.find_us2)

        val spannableString = SpannableString(descriptionText)

        // 设置 GitHub 链接
        val githubStartIndex = descriptionText.indexOf("GitHub")
        val githubEndIndex = githubStartIndex + "GitHub".length
        if (githubStartIndex != -1) {
            val clickableSpanGithub = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openUrl("https://github.com/KuLiPai/LuaHook")
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = getDynamicColor(
                        this@EditActivity,
                        androidx.appcompat.R.attr.colorPrimary
                    )
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(
                clickableSpanGithub,
                githubStartIndex,
                githubEndIndex,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 设置 Telegram 链接
        val telegramStartIndex = descriptionText.indexOf("Telegram")
        val telegramEndIndex = telegramStartIndex + "Telegram".length
        if (telegramStartIndex != -1) {
            val clickableSpanTelegram = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openUrl("https://t.me/LuaXposed")
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = getDynamicColor(
                        this@EditActivity,
                        androidx.appcompat.R.attr.colorPrimary
                    )
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(
                clickableSpanTelegram,
                telegramStartIndex,
                telegramEndIndex,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 设置 QQ 链接
//        val qqStartIndex = descriptionText.indexOf("QQ")
//        val qqEndIndex = descriptionText.indexOf(" ", qqStartIndex) + " ".length
//        if (qqStartIndex != -1 && qqEndIndex != -1) {
//            val clickableSpanQq = object : ClickableSpan() {
//                override fun onClick(widget: View) {
//                    openUrl("https://qm.qq.com/cgi-bin/qm/qr?k=your_qq_key")
//                }
//
//                @SuppressLint("ResourceType")
//                override fun updateDrawState(ds: TextPaint) {
//                    super.updateDrawState(ds)
//                    ds.color = getDynamicColor(
//                        this@MainActivity,
//                        com.google.android.material.R.attr.colorPrimary
//                    )
//                    ds.isUnderlineText = true
//
//                }
//            }
//            spannableString.setSpan(
//                clickableSpanQq,
//                qqStartIndex,
//                qqEndIndex,
//                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//        }

        // 设置 TextView 的文本和 MovementMethod
        appDescriptionTextView.text = spannableString
        appDescriptionTextView.movementMethod = LinkMovementMethod.getInstance()

        // 使用 MaterialAlertDialogBuilder 构建并显示对话框
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .show()
    }


    //菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Undo")
            ?.setIcon(R.drawable.undo_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        menu?.add(0, 2, 0, "Redo")
            ?.setIcon(R.drawable.redo_24px)
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, 3, 0, resources.getString(R.string.format))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 4, 0, resources.getString(R.string.log))  //LogCat
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 5, 0, resources.getString(R.string.manual))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 6, 0, resources.getString(R.string.about))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 9, 0, resources.getString(R.string.search))
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                // "Undo"
                editor.undo()
                true
            }

            2 -> {
                // "Redo"
                editor.redo()
                true
            }

            3 -> {
                // 格式化
//                editor.format()
                try {
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
                //LogCat
                val intent = Intent(this, LogCatActivity::class.java)
                startActivity(intent)
                true
            }

            5 -> {
                //示例
                val intent = Intent(this, Manual::class.java)
                startActivity(intent)
                true
            }

            6 -> {
                showLsposedInfoDialog()
                true
            }

            9 -> {
                if (searchPanel.isVisible) {
                    closeSearchPanel()
                } else {
                    openSearchPanel()
                }
//                LuaParser.lexer(editor.text, Globals(), Flag())
//                editor.setText(AutoIndent.format(editor.text, 2))

//                editor.search()
                true
            }


            else -> false
        }
    }

    private val editor: CodeEditor by lazy { findViewById(R.id.editor) }
    private val fab: FloatingActionButton by lazy { findViewById(R.id.fab) }
    private val toolbar: MaterialToolbar by lazy { findViewById(R.id.toolbar) }
    private val rootLayout: CoordinatorLayout by lazy { findViewById(R.id.main) }
    private val bottomSymbolBar: LinearLayout by lazy { findViewById(R.id.bottomBar) }

    companion object


    fun saveScript(text: String) {
        WorkspaceFileManager.write("/global.lua", text)
    }


    override fun onStop() {
        super.onStop()
        saveScript(editor.text.toString())
    }

    override fun onPause() {
        super.onPause()
        saveScript(editor.text.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_global_edit)
        setSupportActionBar(toolbar)

        val symbolRecyclerView: RecyclerView = findViewById(R.id.symbolRecyclerView)
        symbolRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        symbolRecyclerView.adapter = SymbolAdapter(editor)


        //窗口处理
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            insets.getInsets(WindowInsetsCompat.Type.statusBars())

            // 调整底部符号栏的位置，使其位于输入法上方
            bottomSymbolBar.translationY = -imeInsets.bottom.toFloat()
            fab.translationY = -imeInsets.bottom.toFloat()

            editor.setPadding(0, 0, 0, navigationBarInsets.bottom)


            // 设置根布局的底部内边距
            if (imeInsets.bottom > 0) {
                // 输入法可见时，不需要额外的底部内边距来避免被导航栏遮挡，
                // 因为 bottomSymbolBar 已经移动到输入法上方
                view.setPadding(
                    navigationBarInsets.left,
                    0,
                    navigationBarInsets.right,
                    0
                )

            } else {
                // 输入法不可见时，设置底部内边距以避免内容被导航栏遮挡
                view.setPadding(
                    navigationBarInsets.left,
                    0,
                    navigationBarInsets.right,
                    navigationBarInsets.bottom
                )
            }

            insets
        }

        // 确保在布局稳定后请求 WindowInsets，以便监听器能够正确工作
        ViewCompat.requestApplyInsets(rootLayout)


        val luaScript = WorkspaceFileManager.read("/global.lua")
        if (luaScript == "") {
            val lua = """
        """.trimIndent()
            saveScript(lua)
        }


        // SoraEditor
        initLuaEditor(editor, errMessage)



        editor.setText(luaScript)

        fab.setOnClickListener {
            saveScript(editor.text.toString())
            Toast.makeText(this, resources.getString(R.string.save_ok), Toast.LENGTH_SHORT).show()
        }

        initSearchPanel()

    }

    private fun searchNext() {
        val text = etSearch.text?.toString().orEmpty()
        if (text.isEmpty()) return
        if (!editor.searcher.hasQuery()) {
            editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
        }
        try {
            editor.searcher.gotoNext()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun searchPrevious() {
        val text = etSearch.text?.toString().orEmpty()
        if (text.isEmpty()) return
        if (!editor.searcher.hasQuery()) {
            editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
        }
        try {
            editor.searcher.gotoPrevious()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearchPanel() {
        // 监听搜索框文本变化
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    // search(text, options)
                    // 参数2 SearchOptions: (ignoreCase, normal/regex)
                    // 这里默认: 不区分大小写(false -> true才区分), 普通模式(false -> true才正则)
                    // 如果你想完全精确搜索，可以根据需求调整 SearchOptions
                    editor.searcher.search(text, EditorSearcher.SearchOptions(false, false))
                } else {
                    editor.searcher.stopSearch()
                }
            }
        })

        // 监听键盘的"搜索"按钮
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                searchNext()
                true
            } else {
                false
            }
        }

        // 上一个
        btnPrev.setOnClickListener {
            searchPrevious()
        }

        // 下一个
        btnNext.setOnClickListener {
            searchNext()
        }

        // 替换当前选中
        btnReplaceOne.setOnClickListener {
            if (editor.searcher.hasQuery()) {
                val replaceText = etReplace.text.toString()
                try {
                    editor.searcher.replaceCurrentMatch(replaceText)
                } catch (e: Exception) {
                    Toast.makeText(this, "Replace failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 替换所有
        btnReplaceAll.setOnClickListener {
            if (editor.searcher.hasQuery()) {
                val replaceText = etReplace.text.toString()
                try {
                    editor.searcher.replaceAll(replaceText)
                } catch (e: Exception) {
                    Toast.makeText(this, "Replace All failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 关闭面板
        btnClose.setOnClickListener {
            closeSearchPanel()
        }
    }

    private fun openSearchPanel() {
        searchPanel.visibility = View.VISIBLE
        etSearch.requestFocus()
        // 弹出键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)

        // 如果之前有选中的文本，自动填入搜索框
        if (editor.cursor.isSelected) {
            val selectedText = editor.text.substring(editor.cursor.left, editor.cursor.right)
            if (selectedText.length < 50 && !selectedText.contains("\n")) { // 限制长度防止填入大段代码
                etSearch.setText(selectedText)
                // 移动光标到末尾
                etSearch.setSelection(selectedText.length)
            }
        }
    }

    private fun closeSearchPanel() {
        searchPanel.visibility = View.GONE
        editor.searcher.stopSearch()
        // 关闭键盘
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
        // 焦点还给编辑器
        editor.requestFocus()
    }



    override fun onDestroy() {
        super.onDestroy()
        editor.release()
    }


//    private fun updateBtnState() {
//        undo?.isEnabled = binding.editor.canUndo()
//        redo?.isEnabled = binding.editor.canRedo()
//    }


}