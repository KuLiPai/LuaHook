package com.kulipai.luahook.ui.script.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doBeforeTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.script.ScriptConfigHelper
import com.kulipai.luahook.core.theme.ColorUtils.getDynamicColor
import com.kulipai.luahook.databinding.ActivityMultiScriptBinding
import com.kulipai.luahook.ui.script.editor.app.AppsEdit
import kotlinx.coroutines.launch

class MultiScriptActivity : BaseActivity<ActivityMultiScriptBinding>() {

    private lateinit var adapter: MultScriptAdapter
    private lateinit var currentPackageName: String
    private lateinit var appName: String
    private lateinit var scriptList: MutableList<MutableMap.MutableEntry<String, Any?>>

    private val launcher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch {
                scriptList = ScriptConfigHelper.readConf(currentPackageName)
                adapter.updateData(scriptList)
            }
        }

    override fun inflateBinding(inflater: LayoutInflater): ActivityMultiScriptBinding {
        return ActivityMultiScriptBinding.inflate(inflater)
    }

    override fun initView() {
        // Specific insets handling from original code (ignoring top/bottom padding on root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            binding.bottomTools.setPadding(0, 0, 0, nav.bottom + 8)
            insets
        }

        // Receive Intent data
        val intent = intent
        if (intent != null) {
            currentPackageName = intent.getStringExtra("packageName").toString()
            appName = intent.getStringExtra("appName").toString()
            binding.toolbar.title = "$appName ${resources.getString(R.string.script_manage)}"
        }
        
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rec.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    override fun initData() {
        if (::currentPackageName.isInitialized) {
            scriptList = ScriptConfigHelper.readConf(currentPackageName)
            adapter = MultScriptAdapter(scriptList, currentPackageName, appName, this, launcher)
            binding.rec.adapter = adapter
            setupItemTouchHelper()
        }
    }

    override fun initEvent() {
        binding.fab.setOnClickListener { showNewScriptDialog() }
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                adapter.removeItem(position, this@MultiScriptActivity)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val p = Paint()
                    p.color = getDynamicColor(this@MultiScriptActivity, androidx.appcompat.R.attr.colorError)
                    val background = RectF(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRect(background, p)

                    val deleteIcon = ContextCompat.getDrawable(this@MultiScriptActivity, R.drawable.delete_24px)
                    deleteIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setTint(getDynamicColor(this@MultiScriptActivity, com.google.android.material.R.attr.colorOnError))
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rec)
    }

    @SuppressLint("InflateParams")
    private fun showNewScriptDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_two_edit, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val edit = view.findViewById<TextInputEditText>(R.id.edit)
        val inputLayout2 = view.findViewById<TextInputLayout>(R.id.text_input_layout2)
        val edit2 = view.findViewById<TextInputEditText>(R.id.edit2)
        inputLayout.hint = resources.getString(R.string.script_name)
        inputLayout2.hint = resources.getString(R.string.description)
        
        edit.doBeforeTextChanged { _, _, _, _ -> edit.error = null }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.new_script))
            .setView(view)
            .setPositiveButton(resources.getString(R.string.sure)) { _, _ ->
                if (edit.text.isNullOrEmpty()) {
                    edit.error = resources.getString(R.string.input_sth)
                } else if (scriptList.any { entry -> entry.key == edit.text.toString() }) {
                    edit.error = resources.getString(R.string.existed)
                } else {
                    createAndEditScript(edit.text.toString(), edit2.text.toString())
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun createAndEditScript(name: String, description: String) {
        ScriptConfigHelper.writeScriptConfig(currentPackageName, name, description)
        
        val intent = Intent(this, AppsEdit::class.java).apply {
            putExtra("packageName", currentPackageName)
            putExtra("appName", appName)
            putExtra("scripName", name)
            putExtra("scriptDescription", description)
        }
        launcher.launch(intent)
    }

}
