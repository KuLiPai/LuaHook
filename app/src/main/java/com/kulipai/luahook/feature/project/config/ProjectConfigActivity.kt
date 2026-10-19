package com.kulipai.luahook.feature.project.config

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.project.Project
import com.kulipai.luahook.core.shell.ShellResult
import com.kulipai.luahook.data.repository.ProjectRepository
import com.kulipai.luahook.databinding.ActivityProjectConfigBinding
import kotlinx.coroutines.launch
import com.kulipai.luahook.feature.project.create.CreateProjectActivity
import com.kulipai.luahook.feature.script.selector.ScopeSelectorActivity
import java.io.File

class ProjectConfigActivity : BaseActivity<ActivityProjectConfigBinding>() {

    private lateinit var originalProjectName: String
    private var currentProject: Project? = null

    private var selectedIconPath: String? = null
    private var selectedIconUnicode: String? = null
    private val selectedScope = mutableListOf<String>()

    private val scopeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
             val data = result.data
             val list = data?.getStringArrayListExtra("selected_scope")
             if (list != null) {
                 selectedScope.clear()
                 selectedScope.addAll(list)
                 updateScopeChips()
             }
        }
    }
    
    // Icon Picker
    private val pickIcon = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
             selectedIconPath = it.toString()
             selectedIconUnicode = null
             binding.imgProjectIcon.setImageURI(it)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityProjectConfigBinding {
        return ActivityProjectConfigBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = getString(R.string.title_project_settings)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnCreate.text = getString(R.string.btn_save_changes) // Reusing layout, button ID is btn_create
    }

    override fun initData() {
        originalProjectName = intent.getStringExtra("project_name") ?: ""
        if (originalProjectName.isEmpty()) {
            finish()
            return
        }
        
        lifecycleScope.launch {
            val projects = AppContainer.instance.projectRepository.getProjects()
            currentProject = projects.find { it.name == originalProjectName }
            
            if (currentProject == null) {
                Toast.makeText(this@ProjectConfigActivity, getString(R.string.msg_project_not_found), Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            
            // Populate UI
            binding.etProjectName.setText(currentProject!!.name)
            binding.etProjectDesc.setText(currentProject!!.description)
            binding.etProjectAuthor.setText(currentProject!!.author)
            binding.etProjectLauncher.setText(currentProject!!.launcher)
            
            // Icon
            if (currentProject!!.icon.length == 1 && currentProject!!.icon.codePointAt(0) > 255) { // Assuming unicode
                 val symbol = currentProject!!.icon
                 selectedIconUnicode = symbol
                 val font = try {
                     ResourcesCompat.getFont(this@ProjectConfigActivity, R.font.material_symbols)
                 } catch(e: Exception) { null }
                 binding.imgProjectIcon.setImageBitmap(textToBitmap(symbol, font))
            } else {
                 // File path relative to project
                 val iconFile = File("${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/${currentProject!!.name}", currentProject!!.icon)
                 if (iconFile.exists()) {
                     binding.imgProjectIcon.setImageBitmap(BitmapFactory.decodeFile(iconFile.absolutePath))
                 } else {
                     binding.imgProjectIcon.setImageResource(R.mipmap.ic_launcher)
                 }
            }
            
            // Scope
            selectedScope.clear()
            selectedScope.addAll(currentProject!!.scope)
            if (selectedScope.isEmpty()) selectedScope.add("all")
            updateScopeChips()
        }
    }

    override fun initEvent() {
        binding.cardIconPreview.setOnClickListener {
            showIconSelectionDialog()
        }
        
        binding.btnSelectScope.setOnClickListener {
             val intent = Intent(this, ScopeSelectorActivity::class.java)
             val current = if (selectedScope.contains("all")) ArrayList() else ArrayList(selectedScope)
             intent.putStringArrayListExtra("current_scope", current)
             scopeLauncher.launch(intent)
        }
        
        binding.btnAddManualScope.setOnClickListener {
            showAddScopeDialog()
        }

        binding.btnCreate.setOnClickListener {
            saveChanges()
        }
    }
    
    private fun saveChanges() {
        val newName = binding.etProjectName.text.toString().trim()
        val newDesc = binding.etProjectDesc.text.toString().trim()
        
        if (newName.isEmpty()) {
             binding.etProjectName.error = getString(R.string.err_name_empty)
             return
        }
        
        lifecycleScope.launch {
            // Handle Rename if changed
            if (newName != originalProjectName) {
                // Check if exists
                if (AppContainer.instance.workspaceStorage.directoryExists("${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$newName")) {
                    Toast.makeText(this@ProjectConfigActivity, getString(R.string.err_project_exists), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // Move directory
                val oldDir = "${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$originalProjectName"
                val newDir = "${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$newName"
                
                // Simple move utilizing shell
                val mvResult = AppContainer.instance.shellExecutor.exec("mv \"$oldDir\" \"$newDir\"")
                // Note: Renaming also requires updating info.json key!
                
                if (mvResult !is ShellResult.Success) {
                    Toast.makeText(this@ProjectConfigActivity, getString(R.string.err_rename_failed), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Update info.json
                val infoMap = AppContainer.instance.workspaceStorage.readMap(ProjectRepository.INFO_JSON)
                val wasEnabled = infoMap[originalProjectName] as? Boolean ?: false
                infoMap.remove(originalProjectName)
                infoMap[newName] = wasEnabled
                AppContainer.instance.workspaceStorage.writeMap(ProjectRepository.INFO_JSON, infoMap)
                
                originalProjectName = newName // Update reference
            }
            
            // Handle Icon Update
            var iconValue = currentProject?.icon ?: "icon.png"
            val projectFullPath = "${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/$newName"
            
            if (selectedIconPath != null) {
                try {
                    val inputStream = contentResolver.openInputStream(Uri.parse(selectedIconPath))
                    val destIcon = "$projectFullPath/icon.png"
                    // Write to temp then mv/cp
                    val tempFile = File(externalCacheDir, "temp_icon_update.png")
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val script = "cat \"${tempFile.absolutePath}\" > \"$destIcon\"\nchmod 666 \"$destIcon\""
                    AppContainer.instance.shellExecutor.exec(script)
                    iconValue = "icon.png"
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (selectedIconUnicode != null) {
                val bmp = textToBitmap(selectedIconUnicode!!)
                val destIcon = "$projectFullPath/icon.png" 
                val tempFile = File(externalCacheDir, "temp_icon_unicode_update.png")
                try {
                    tempFile.outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    val script = "cat \"${tempFile.absolutePath}\" > \"$destIcon\"\nchmod 666 \"$destIcon\""
                    AppContainer.instance.shellExecutor.exec(script)
                    iconValue = selectedIconUnicode!!
                } catch(e: Exception) { e.printStackTrace() }
            }

            val newAuthor = binding.etProjectAuthor.text.toString().trim()
            val newLauncher = binding.etProjectLauncher.text.toString().trim()

            // Re-generate init.lua
            val scopeLua = if (selectedScope.contains("all")) "\"all\"" else {
                "{\n" + selectedScope.joinToString(",\n") { "    \"$it\"" } + "\n}"
            }
            
            val newInitLua = """
name = "$newName"
description = "$newDesc"
author = "$newAuthor"
icon = "$iconValue"
launcher = "$newLauncher"
scope = ${scopeLua.trimIndent()}
""".trimIndent()

            AppContainer.instance.workspaceStorage.write("${WorkspaceStorage.Project}/$newName/init.lua", newInitLua)
            
            Toast.makeText(this@ProjectConfigActivity, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    // ... Duplicated helper methods from CreateProjectActivity (updateScopeChips, showSymbolDialog, textToBitmap...)
    // Ideally these should be in a shared Mixin or Helper but I will duplicate for speed as they are private logic
    
    private fun updateScopeChips() {
        binding.chipGroupScope.removeAllViews()
        for (pkg in selectedScope) {
            val chip = Chip(this)
            chip.text = if (pkg == "all") getString(R.string.text_all_apps) else pkg
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                selectedScope.remove(pkg)
                updateScopeChips()
            }
            chip.setOnClickListener {
                if (pkg != "all") {
                    binding.etProjectLauncher.setText(pkg)
                }
            }
            binding.chipGroupScope.addView(chip)
        }
    }
    
    private fun showSymbolSelectionDialog() {
         // Use custom layout to avoid conflicting with other modules
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_glyph_picker, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_glyphs)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)
        
        // Dense Grid: 6 columns
        recyclerView.layoutManager = GridLayoutManager(this, 6)
        
        // Use local Adapter class to avoid global namespace pollution if possible, or rename
        val adapter = CreateProjectActivity.ProjectGlyphAdapter { symbol ->
            selectedIconUnicode = symbol
            selectedIconPath = null
            
            // Render
            val font = try {
                ResourcesCompat.getFont(this, R.font.material_symbols)
            } catch (e: Exception) { null }
            
            binding.imgProjectIcon.setImageBitmap(textToBitmap(symbol, font))
            // Dismiss dialog handled via captured reference
        }
        recyclerView.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_select_symbol))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.cancel), null)
            .create()

        adapter.onItemClick = { symbol ->
             selectedIconUnicode = symbol
             selectedIconPath = null
             val font = try {
                 ResourcesCompat.getFont(this, R.font.material_symbols)
             } catch (e: Exception) { null }
             
             binding.imgProjectIcon.setImageBitmap(textToBitmap(symbol, font))
             dialog.dismiss()
        }

        dialog.show()
        
        // Async Load
        Thread {
            val validSymbols = mutableListOf<String>()
            val paint = Paint()
            try {
                val typeface = ResourcesCompat.getFont(this, R.font.material_symbols)
                paint.typeface = typeface
                for (code in 0xe000..0xeFFF) {
                    val str = String(Character.toChars(code))
                    if (paint.hasGlyph(str)) {
                        validSymbols.add(str)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            runOnUiThread {
                progressBar.visibility = View.GONE
                adapter.setData(validSymbols)
            }
        }.start()
    }
     
    private fun showIconSelectionDialog() {
        val options = arrayOf(getString(R.string.option_select_gallery), getString(R.string.option_select_symbol))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_choose_icon))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickIcon.launch("image/*")
                    1 -> showSymbolSelectionDialog()
                }
            }
            .show()
    }
    
    private fun showAddScopeDialog() {
        val input = TextInputEditText(this)
        input.hint = "com.example.package"
        val layout = TextInputLayout(this)
        layout.addView(input)
        layout.setPadding(32, 16, 32, 0)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_add_scope))
            .setView(layout)
            .setPositiveButton(getString(R.string.action_add)) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty() && !selectedScope.contains(text)) {
                    // Remove 'all' if adding specific
                    if (selectedScope.contains("all")) selectedScope.remove("all")
                    selectedScope.add(text)
                    updateScopeChips()
                    binding.etProjectLauncher.setText(text)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun textToBitmap(text: String, typeface: Typeface? = null): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 100f
        
        val typedValue = TypedValue()
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        paint.color = typedValue.data
        
        paint.textAlign = Paint.Align.LEFT
        if (typeface != null) paint.typeface = typeface
        
        val baseline = -paint.ascent() 
        val width = (paint.measureText(text) + 0.5f).toInt().coerceAtLeast(1)
        val height = (baseline + paint.descent() + 0.5f).toInt().coerceAtLeast(1)
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawText(text, 0f, baseline, paint)
        return image
    }

}
