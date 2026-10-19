package com.kulipai.luahook.feature.project.create

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.databinding.ActivityCreateProjectBinding
import kotlinx.coroutines.launch
import com.kulipai.luahook.feature.script.selector.ScopeSelectorActivity
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreateProjectActivity : BaseActivity<ActivityCreateProjectBinding>() {

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

    override fun inflateBinding(inflater: LayoutInflater): ActivityCreateProjectBinding {
        return ActivityCreateProjectBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // Initial state: "all" if empty? Or empty means global? 
        // Logic: Empty list usually means no scope or all? 
        // Let's assume initially "All" is selected.
        if (selectedScope.isEmpty()) {
            selectedScope.add("all")
        }
        updateScopeChips()
        
        val savedAuthor = getSharedPreferences("conf", MODE_PRIVATE).getString("author", "")
        binding.etProjectAuthor.setText(savedAuthor)
    }

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
    
    private fun showSymbolSelectionDialog() {
        // Use custom layout to avoid conflicting with other modules
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_glyph_picker, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_glyphs)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)
        
        // Dense Grid: 6 columns
        recyclerView.layoutManager = GridLayoutManager(this, 6)
        
        // Use local Adapter class to avoid global namespace pollution if possible, or rename
        val adapter = ProjectGlyphAdapter { symbol ->
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
                // Load font specifically for checking glyphs
                val typeface = ResourcesCompat.getFont(this, R.font.material_symbols)
                paint.typeface = typeface
                
                // Scan \u0001 to \uffff
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
    
    // Updated textToBitmap
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
        val image = createBitmap(width, height)
        val canvas = Canvas(image)
        canvas.drawText(text, 0f, baseline, paint)
        return image
    }
    
    // Dedicated Adapter for Project Creation
    class ProjectGlyphAdapter(var onItemClick: (String) -> Unit) : RecyclerView.Adapter<ProjectGlyphAdapter.ViewHolder>() {
        private val symbols = mutableListOf<String>()
        
        fun setData(list: List<String>) {
            symbols.clear()
            symbols.addAll(list)
            notifyDataSetChanged()
        }
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
             val textView: TextView = view.findViewById(R.id.tv_glyph)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_picker_glyph, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
             val symbol = symbols[position]
             holder.textView.text = symbol
             
             // Set material font
             try {
                // Consider caching this type/font lookup
                val font = ResourcesCompat.getFont(holder.itemView.context, R.font.material_symbols)
                holder.textView.typeface = font
             } catch(e: Exception) {}
             
             holder.itemView.setOnClickListener { onItemClick(symbol) }
        }
        
        override fun getItemCount() = symbols.size
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
            val name = binding.etProjectName.text.toString()
            val desc = binding.etProjectDesc.text.toString()
            val author = binding.etProjectAuthor.text.toString()
            val launcher = binding.etProjectLauncher.text.toString()
            
            // Save author for next time
            if (author.isNotEmpty()) {
                getSharedPreferences("conf", MODE_PRIVATE).edit().putString("author", author).apply()
            }

            if (name.isBlank()) {
                binding.etProjectName.error = resources.getString(R.string.input_id)
                return@setOnClickListener
            }
            
            var cacheIconPath: String? = null
            // ... (rest of icon logic same as before, but handle bitmap generation from unicode if chosen) ...
            // Wait, projectRepository.createProject takes iconPath OR iconUnicode.
            // If unicode is selected, we pass it.
            // If image is selected, we pass path.
            
            if (selectedIconPath != null) {
                try {
                    val inputStream = contentResolver.openInputStream(Uri.parse(selectedIconPath))
                    val tempFile = File(externalCacheDir, "temp_icon.png")
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    cacheIconPath = tempFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (selectedIconUnicode != null) {
                // We might want to save the generated bitmap as icon.png even if it's unicode, 
                // so it shows up in file system lists easily? 
                // Previous logic allowed passing unicode string. 
                // But ProjectList shows image from file.
                // Best to save it as file.
                val bmp = textToBitmap(selectedIconUnicode!!)
                val tempFile = File(externalCacheDir, "temp_icon_unicode.png")
                try {
                    tempFile.outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    cacheIconPath = tempFile.absolutePath
                } catch(e: Exception) { e.printStackTrace() }
            }

            lifecycleScope.launch {
                val success = AppContainer.instance.projectRepository.createProject(
                    name = name,
                    description = desc,
                    author = author,
                    iconPath = cacheIconPath,
                    iconUnicode = "\\u"+ selectedIconUnicode?.codePointAt(0)?.toHexString(), // We can still pass it for meta info
                    scope = selectedScope,
                    launcher = launcher
                )

                if (success) {
                    Toast.makeText(this@CreateProjectActivity, resources.getString(R.string.save_ok), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreateProjectActivity, getString(R.string.msg_create_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private val pickIcon = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
             selectedIconPath = it.toString()
             selectedIconUnicode = null
             binding.imgProjectIcon.setImageURI(it)
        }
    }
}


