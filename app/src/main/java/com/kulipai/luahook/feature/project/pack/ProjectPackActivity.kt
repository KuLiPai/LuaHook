package com.kulipai.luahook.feature.project.pack

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.project.Project
import com.kulipai.luahook.databinding.ActivityProjectPackBinding
import com.kulipai.luahook.feature.project.pack.engine.PackResult
import com.kulipai.luahook.feature.project.pack.engine.PackStage
import com.kulipai.luahook.feature.project.pack.engine.signer.SignConfig
import com.kulipai.luahook.feature.script.selector.ScopeSelectorActivity
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

class ProjectPackActivity : BaseActivity<ActivityProjectPackBinding>() {

    private val viewModel: ProjectPackViewModel by viewModels()

    private var projectName: String = ""
    private var isInitialPopulated = false
    private val selectedScope = mutableListOf<String>()

    private var customIconBitmap: Bitmap? = null
    private var selectedKeystoreFile: File? = null
    private var pendingSaveApkFile: File? = null

    private var progressDialog: AlertDialog? = null
    private var progressTextView: TextView? = null
    private var progressBar: ProgressBar? = null

    private val scopeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val list = result.data?.getStringArrayListExtra("selected_scope")
            if (list != null) {
                selectedScope.clear()
                selectedScope.addAll(list)
                updateScopeChips()
            }
        }
    }

    private val pickIconLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handlePickedIconUri(it) }
    }

    private val pickKeystoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handlePickedKeystoreUri(it) }
    }

    private val saveApkLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val srcFile = pendingSaveApkFile
        if (uri != null && srcFile != null && srcFile.exists()) {
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(this, getString(R.string.msg_apk_saved), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_apk_save_failed, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityProjectPackBinding {
        return ActivityProjectPackBinding.inflate(inflater)
    }

    override fun initView() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun initData() {
        projectName = intent.getStringExtra("project_name")
            ?: intent.getStringExtra("projectName")
            ?: ""

        if (projectName.isBlank()) {
            finish()
            return
        }

        viewModel.loadProject(projectName)
    }

    override fun initEvent() {
        binding.cardIconPreview.setOnClickListener {
            pickIconLauncher.launch("image/*")
        }

        binding.btnSelectScope.setOnClickListener {
            val intent = Intent(this, ScopeSelectorActivity::class.java)
            if (!selectedScope.contains("all")) {
                intent.putStringArrayListExtra("selected_scope", ArrayList(selectedScope))
            }
            scopeLauncher.launch(intent)
        }

        binding.btnAddManualScope.setOnClickListener {
            showAddManualScopeDialog()
        }

        binding.rgKeySource.setOnCheckedChangeListener { _, checkedId ->
            binding.layoutCustomKeystore.isVisible = (checkedId == R.id.rb_custom_keystore)
        }

        binding.btnChooseKeystore.setOnClickListener {
            pickKeystoreLauncher.launch(arrayOf("*/*"))
        }

        binding.btnStartPack.setOnClickListener {
            validateAndStartPacking()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!state.isLoadingProject && !isInitialPopulated) {
                        isInitialPopulated = true
                        populateInitialForm(state.project, state.suggestedPackageName)
                    }

                    if (state.isPacking) {
                        showOrUpdateProgressDialog(state.packStage, state.packProgress)
                    } else {
                        dismissProgressDialog()
                    }

                    state.packResult?.let { result ->
                        viewModel.clearPackResult()
                        showPackSuccessDialog(result)
                    }

                    state.errorMessage?.let { err ->
                        viewModel.clearError()
                        Toast.makeText(
                            this@ProjectPackActivity,
                            getString(R.string.msg_pack_failed, err),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun populateInitialForm(project: Project?, suggestedPkg: String) {
        val name = project?.name ?: projectName
        binding.etApkName.setText(name)
        binding.etAuthor.setText(project?.author?.takeIf { it.isNotBlank() } ?: "")
        binding.etPackageName.setText(suggestedPkg)
        binding.etVersionName.setText("1.0.0")
        binding.etVersionCode.setText("1")
        binding.etModuleDesc.setText(
            project?.description?.takeIf { it.isNotBlank() } ?: "$name - LuaHook Module"
        )

        selectedScope.clear()
        if (project != null && project.scope.isNotEmpty()) {
            selectedScope.addAll(project.scope)
        } else {
            selectedScope.add("all")
        }
        updateScopeChips()

        // 加载项目自带图标（图片或 Material Symbol 字体图标）
        if (project != null) {
            loadInitialProjectIcon(project)
        }
    }

    private fun loadInitialProjectIcon(project: Project) {
        val iconStr = project.icon
        if (iconStr.isNotEmpty() && iconStr.codePointAt(0) in 0xE000..0xEFFF) {
            val bitmap = renderUnicodeSymbolBitmap(iconStr)
            if (bitmap != null) {
                customIconBitmap = bitmap
                binding.imgApkIcon.setImageBitmap(bitmap)
                return
            }
        }

        if (iconStr.endsWith(".png") || iconStr.endsWith(".jpg")) {
            val iconFile = File("${WorkspaceStorage.DIR}${WorkspaceStorage.Project}/${project.name}/$iconStr")
            if (iconFile.exists()) {
                val decoded = BitmapFactory.decodeFile(iconFile.absolutePath)
                if (decoded != null) {
                    val scaled = Bitmap.createScaledBitmap(decoded, 192, 192, true)
                    customIconBitmap = scaled
                    binding.imgApkIcon.setImageBitmap(scaled)
                }
            }
        }
    }

    private fun renderUnicodeSymbolBitmap(unicodeStr: String): Bitmap? {
        return try {
            val size = 192
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = ResourcesCompat.getFont(this@ProjectPackActivity, R.font.material_icons)
                textSize = size * 0.65f
                textAlign = Paint.Align.CENTER
                val tv = TypedValue()
                theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)
                color = tv.data
            }
            val fm = paint.fontMetrics
            val centerY = size / 2f - (fm.descent + fm.ascent) / 2f
            canvas.drawText(unicodeStr, size / 2f, centerY, paint)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun handlePickedIconUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val raw = BitmapFactory.decodeStream(input)
                if (raw != null) {
                    val scaled = Bitmap.createScaledBitmap(raw, 192, 192, true)
                    customIconBitmap = scaled
                    binding.imgApkIcon.setImageBitmap(scaled)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Failed to load icon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePickedKeystoreUri(uri: Uri) {
        try {
            val displayName = queryDisplayName(uri) ?: "keystore.jks"
            val destFile = File(cacheDir, "custom_keystore_${System.currentTimeMillis()}.bin")
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            selectedKeystoreFile?.delete()
            selectedKeystoreFile = destFile
            binding.tvKeystorePath.text = displayName
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Failed to read keystore", Toast.LENGTH_SHORT).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun updateScopeChips() {
        binding.chipGroupScope.removeAllViews()
        if (selectedScope.contains("all")) {
            val chip = Chip(this).apply {
                text = getString(R.string.text_all_apps)
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedScope.remove("all")
                    updateScopeChips()
                }
            }
            binding.chipGroupScope.addView(chip)
        } else {
            for (pkg in selectedScope.toList()) {
                val chip = Chip(this).apply {
                    text = pkg
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        selectedScope.remove(pkg)
                        updateScopeChips()
                    }
                }
                binding.chipGroupScope.addView(chip)
            }
        }
    }

    private fun showAddManualScopeDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = (24 * resources.displayMetrics.density).toInt()
            val padV = (8 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, 0)
        }
        val inputLayout = TextInputLayout(this).apply {
            hint = getString(R.string.hint_package_name)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val editText = TextInputEditText(inputLayout.context)
        inputLayout.addView(editText)
        container.addView(inputLayout)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_add_scope))
            .setView(container)
            .setPositiveButton(getString(R.string.action_add)) { _, _ ->
                val pkg = editText.text?.toString()?.trim().orEmpty()
                if (pkg.isNotEmpty()) {
                    if (pkg == "all") {
                        selectedScope.clear()
                        selectedScope.add("all")
                    } else {
                        selectedScope.remove("all")
                        if (!selectedScope.contains(pkg)) {
                            selectedScope.add(pkg)
                        }
                    }
                    updateScopeChips()
                }
            }
            .setNeutralButton(getString(R.string.text_all_apps)) { _, _ ->
                selectedScope.clear()
                selectedScope.add("all")
                updateScopeChips()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun validateAndStartPacking() {
        val appName = binding.etApkName.text?.toString()?.trim().orEmpty()
        val author = binding.etAuthor.text?.toString()?.trim().orEmpty()
        val pkgName = binding.etPackageName.text?.toString()?.trim().orEmpty()
        val versionName = binding.etVersionName.text?.toString()?.trim().orEmpty().ifBlank { "1.0.0" }
        val versionCodeStr = binding.etVersionCode.text?.toString()?.trim().orEmpty()
        val versionCode = versionCodeStr.toIntOrNull() ?: -1
        val desc = binding.etModuleDesc.text?.toString()?.trim().orEmpty()

        if (appName.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_empty_app_name), Toast.LENGTH_SHORT).show()
            return
        }
        if (!PackageNameValidator.isValid(pkgName)) {
            Toast.makeText(this, getString(R.string.err_invalid_package_name), Toast.LENGTH_SHORT).show()
            return
        }
        if (versionCode <= 0) {
            Toast.makeText(this, getString(R.string.err_invalid_version_code), Toast.LENGTH_SHORT).show()
            return
        }

        val abis = linkedSetOf<String>()
        if (binding.cbAbiArm64.isChecked) abis.add("arm64-v8a")
        if (binding.cbAbiArmv7.isChecked) abis.add("armeabi-v7a")
        if (abis.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_no_abi_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val v1 = binding.cbSignV1.isChecked
        val v2 = binding.cbSignV2.isChecked
        val v3 = binding.cbSignV3.isChecked
        if (!v1 && !v2 && !v3) {
            Toast.makeText(this, getString(R.string.err_no_signing_scheme), Toast.LENGTH_SHORT).show()
            return
        }

        val useCustomKeystore = binding.rbCustomKeystore.isChecked
        if (useCustomKeystore && (selectedKeystoreFile == null || !selectedKeystoreFile!!.exists())) {
            Toast.makeText(this, getString(R.string.err_missing_keystore), Toast.LENGTH_SHORT).show()
            return
        }

        val signConfig = SignConfig(
            useCustomKeystore = useCustomKeystore,
            keystoreFile = selectedKeystoreFile,
            keystorePassword = binding.etKeystorePassword.text?.toString().orEmpty(),
            keyAlias = binding.etKeyAlias.text?.toString().orEmpty(),
            keyPassword = binding.etKeyPassword.text?.toString().orEmpty(),
            v1SigningEnabled = v1,
            v2SigningEnabled = v2,
            v3SigningEnabled = v3
        )

        val iconPngBytes = customIconBitmap?.let { bmp ->
            val bos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
            bos.toByteArray()
        }

        val finalScopes = if (selectedScope.isEmpty()) listOf("all") else selectedScope.toList()

        viewModel.startPacking(
            context = applicationContext,
            projectName = projectName,
            appName = appName,
            packageName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            description = desc.ifBlank { appName },
            author = author,
            scopes = finalScopes,
            showLauncherIcon = binding.switchShowLauncherIcon.isChecked,
            customIconPngBytes = iconPngBytes,
            abiFilter = abis,
            signConfig = signConfig
        )
    }

    private fun showOrUpdateProgressDialog(stage: PackStage, progress: Int) {
        val stageText = when (stage) {
            PackStage.PREPARE -> getString(R.string.stage_prepare)
            PackStage.INJECT_PROJECT -> getString(R.string.stage_inject_project)
            PackStage.REWRITE_MANIFEST -> getString(R.string.stage_rewrite_manifest)
            PackStage.ZIPALIGN -> getString(R.string.stage_zipalign)
            PackStage.SIGN -> getString(R.string.stage_sign)
            PackStage.COMPLETE -> getString(R.string.stage_complete)
        }

        if (progressDialog == null || !progressDialog!!.isShowing) {
            val pad = (24 * resources.displayMetrics.density).toInt()
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(pad, pad, pad, pad)
            }
            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                isIndeterminate = false
                max = 100
                this.progress = progress
            }
            val tv = TextView(this).apply {
                text = stageText
                val topMargin = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, topMargin, 0, 0)
            }
            layout.addView(pb)
            layout.addView(tv)

            progressBar = pb
            progressTextView = tv
            progressDialog = MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_packing_progress))
                .setView(layout)
                .setCancelable(false)
                .create()
            progressDialog?.show()
        } else {
            progressBar?.progress = progress
            progressTextView?.text = stageText
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
        progressBar = null
        progressTextView = null
    }

    private fun showPackSuccessDialog(result: PackResult) {
        val sizeMb = String.format(Locale.US, "%.2f MB", result.fileSizeBytes / (1024.0 * 1024.0))
        val shortSha = result.sha256.take(16) + "..."
        val summary = getString(
            R.string.msg_pack_summary,
            result.appName,
            result.packageName,
            result.versionName,
            result.versionCode,
            sizeMb,
            shortSha
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_pack_success))
            .setMessage(summary)
            .setPositiveButton(getString(R.string.action_install_apk)) { _, _ ->
                installPackedApk(result.apkFile)
            }
            .setNeutralButton(getString(R.string.action_save_apk)) { _, _ ->
                pendingSaveApkFile = result.apkFile
                saveApkLauncher.launch(result.apkFile.name)
            }
            .setNegativeButton(getString(R.string.action_share_apk)) { _, _ ->
                sharePackedApk(result.apkFile)
            }
            .show()
    }

    private fun installPackedApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Install failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePackedApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                apkFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_apk)))
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Share failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        dismissProgressDialog()
        selectedKeystoreFile?.delete()
        super.onDestroy()
    }
}
