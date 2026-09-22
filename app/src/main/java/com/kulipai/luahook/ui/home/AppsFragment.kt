package com.kulipai.luahook.ui.home

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kulipai.luahook.R
import com.kulipai.luahook.app.MyApplication
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.data.model.AppInfo
import com.kulipai.luahook.databinding.FragmentHomeAppsBinding
import com.kulipai.luahook.ui.script.editor.app.AppsEdit
import com.kulipai.luahook.ui.script.selector.SelectApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader


class AppsFragment : BaseFragment<FragmentHomeAppsBinding>() {


    private lateinit var adapter: AppsAdapter
    private var appInfoList: List<AppInfo> = emptyList()


    private val launcher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            refreshData()
        }

    override fun initView() {
        super.initView()
        var searchJob: Job? = null
        // 加载 Fragment 的布局

        // 设置rec的bottom高度适配
        activity?.findViewById<BottomNavigationView>(R.id.bottomBar)?.let { bottomNavigationView ->
            val bottomBarHeight = bottomNavigationView.height

            binding.rec.setPadding(
                binding.rec.paddingLeft,
                binding.rec.paddingTop,
                binding.rec.paddingRight,
                bottomBarHeight
            )
        }

        adapter = AppsAdapter(emptyList(), requireContext()) // 先传空列表
        binding.rec.layoutManager = LinearLayoutManager(requireContext())
        binding.rec.adapter = adapter


        // 在处理 Fragment 视图相关的操作时，使用 viewLifecycleOwner.lifecycleScope 更安全
        ShellManager.mode.observe(viewLifecycleOwner) {
            if (it != ShellManager.Mode.NONE) {
                refreshData()
            }
        }



        binding.searchbar.setOnClickListener {
            binding.searchBarTextView.requestFocus()
            // 显示软键盘
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchBarTextView, InputMethodManager.SHOW_IMPLICIT)
        }


        //搜索
        binding.searchBarTextView.doAfterTextChanged { s ->
            s.toString()
            searchJob?.cancel()
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                if (appInfoList.isNotEmpty()) {
                    delay(100) // 延迟300ms
                    filterAppList(s.toString().trim())
                }
            }
        }

        binding.clearText.setOnClickListener {
            binding.searchBarTextView.setText("")
            binding.clearText.visibility = View.INVISIBLE
        }


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


        binding.fab1.setOnClickListener {
            if (ShellManager.mode.value != ShellManager.Mode.NONE) {
                val intent = Intent(requireContext(), SelectApps::class.java)
                launcher.launch(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.Inactive_modules),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.fab2.setOnClickListener {
            openFilePicker()
        }

        //fab点击
        var isOpen = false
        binding.fab.setOnClickListener {
            val rotateAnimator =
                ObjectAnimator.ofFloat(
                    binding.fab,
                    "rotation",
                    binding.fab.rotation,
                    binding.fab.rotation + 45f
                )
            rotateAnimator.duration = 300
            rotateAnimator.start()
            if (isOpen) {
                hideFabWithAnimation(binding.fab1)
                hideFabWithAnimation(binding.fab2, 300)
                isOpen = !isOpen
            } else {
                showFabWithAnimation(binding.fab2)
                showFabWithAnimation(binding.fab1, 350)
                isOpen = !isOpen
            }
        }

//        fab.setOnClickListener {
//            if (ShellManager.getMode() != ShellManager.Mode.NONE) {
//                val intent = Intent(requireContext(), SelectApps::class.java)
//                launcher.launch(intent)
//            } else {
//                Toast.makeText(requireContext(), "未激活模块", Toast.LENGTH_SHORT).show()
//
//            }
//
//        }

    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeAppsBinding {
        return FragmentHomeAppsBinding.inflate(inflater, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (view != null) refreshData()
    }

    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (ShellManager.mode.value != ShellManager.Mode.NONE) {
                val savedList = WorkspaceFileManager.readStringList("/apps.txt")
                if (savedList.isNotEmpty()) {
                    appInfoList = MyApplication.instance.getAppInfoList(savedList)
                    adapter.updateData(appInfoList)
                } else {
                    appInfoList = emptyList()
                    adapter.updateData(emptyList())
                }
            }
        }
    }

    // TODO)) 封装，传入一个clearText这个ImageView
    private fun filterAppList(query: String) {
        val filteredList = if (query.isEmpty()) {
            binding.clearText.visibility = View.INVISIBLE
            appInfoList // 显示全部
        } else {
            binding.clearText.visibility = View.VISIBLE
            appInfoList.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filteredList)
    }


    fun showFabWithAnimation(fab: View, time: Long = 300) {
        // 如果 FAB 已经可见，就不再执行进入动画
        if (fab.isVisible && fab.alpha == 1f && fab.scaleX == 1f) {
            return
        }

        // 重置状态，确保动画从正确位置开始
        fab.alpha = 0f
        fab.scaleX = 0f
        fab.scaleY = 0f
        fab.visibility = View.VISIBLE // 动画开始前先让视图可见，否则无法动画

        val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 0f, 1f)
        val alphaAnimator = ObjectAnimator.ofFloat(fab, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            duration = time // 动画持续时间，例如 300 毫秒
            interpolator = AccelerateDecelerateInterpolator() // 加速然后减速的插值器
        }
        animatorSet.start()
    }


    fun hideFabWithAnimation(fab: View, time: Long = 250) {
        // 如果 FAB 已经不可见，就不再执行退出动画
        if (fab.isInvisible && fab.alpha == 0f && fab.scaleX == 0f) {
            return
        }

        val scaleXAnimator = ObjectAnimator.ofFloat(fab, "scaleX", 1f, 0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(fab, "scaleY", 1f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(fab, "alpha", 1f, 0f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            duration = time // 退出动画通常可以快一点
            interpolator = AccelerateDecelerateInterpolator()
            // 动画结束后将视图设置为不可见，不占据空间
            addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    fab.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        animatorSet.start()
    }


    // ActivityResultLauncher 用于处理文件选择器的结果
    val pickFileLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            // 当文件选择器返回结果时会调用此 lambda
            uri?.let {
                // 处理选择的文件 URI
                readFileContent(it)
            } ?: run {
                // 用户取消了文件选择
            }
        }


    // TODO)) 封装，
    /**
     * 调用系统文件选择器来选择文件。
     * 使用 Storage Access Framework (SAF) 的 ACTION_OPEN_DOCUMENT。
     */
    fun openFilePicker() {
        // 参数是一个字符串数组，表示你想要选择的文件类型（MIME 类型）。
        // 例如：
        // arrayOf("image/*") 选择所有图片文件
        // arrayOf("application/pdf") 选择PDF文件
        // arrayOf("text/plain") 选择文本文件
        // arrayOf("*/*") 选择所有文件类型
        pickFileLauncher.launch(arrayOf("*/*"))
    }

    // TODO)) 封装，
    /**
     * 读取指定 URI 的文件内容并显示。
     *
     * @param fileUri 文件的 Uri，通过文件选择器获取。
     */
    fun readFileContent(fileUri: Uri) {
        val stringBuilder = StringBuilder()
        resources.getString(R.string.unknown_file)

        // 获取文件名 (可选)
//        contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                if (displayNameIndex != -1) {
//                    fileName = cursor.getString(displayNameIndex)
//                }
//            }
//        }

        try {
            // 打开文件输入流
            requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                // 使用 BufferedReader 读取文本内容
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                }
            }
            loadScript(stringBuilder.toString())


//            fileContentTextView.text = "文件名: $fileName\n\n文件内容:\n${stringBuilder.toString()}"
        } catch (e: Exception) {
            Log.e("FilePicker", resources.getString(R.string.read_file_failed) + "${e.message}", e)
//            fileContentTextView.text = "读取文件失败: ${e.message}"
        }
    }

    // TODO)) 封装，
    @OptIn(DelicateCoroutinesApi::class)
    fun loadScript(script: String) {
        // 解析参数
        val param = WorkspaceFileManager.parseParameters(script)
        if (param?.name.isNullOrEmpty() || param.packageName.isNullOrEmpty()) {
            Toast.makeText(
                requireActivity(),
                resources.getString(R.string.read_file_failed),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // apps列表
            val appList = WorkspaceFileManager.readStringList("/apps.txt")
            if (appList.isEmpty() || !appList.contains(param.packageName)) {
                appList.add(param.packageName)
                WorkspaceFileManager.writeStringList("/apps.txt", appList)
            }


            // appconf
            // 写配置
            val path = WorkspaceFileManager.AppConf + "/" + param.packageName + ".txt"
            val map = WorkspaceFileManager.readMap(path)
            map[param.name] = arrayOf<Any?>(true, param.descript, "v1.0")
            WorkspaceFileManager.writeMap(path, map)
            WorkspaceFileManager.ensureDirectoryExists(WorkspaceFileManager.DIR + "/" + WorkspaceFileManager.AppScript + "/" + param.packageName)


            // appscript
            val path2 =
                WorkspaceFileManager.AppScript + "/" + param.packageName + "/" + param.name + ".lua"
            WorkspaceFileManager.write(path2, script)

            lifecycleScope.launch {
                // 更新页面
                val appInfoList = MyApplication.Companion.instance.getAppInfoList(appList)
                adapter.updateData(appInfoList)

            }

            // 打开页面
            // 进入编辑界面
            val intent = Intent(requireContext(), AppsEdit::class.java)
            intent.putExtra("packageName", param.packageName)
            intent.putExtra("scripName", param.name)
            intent.putExtra("scriptDescription", param.descript)
            launcher.launch(intent)
        }

    }


}