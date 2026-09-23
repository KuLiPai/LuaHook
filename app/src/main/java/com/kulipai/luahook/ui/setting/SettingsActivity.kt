package com.kulipai.luahook.ui.setting

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.core.language.LanguageUtils
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.data.model.McpInfo
import com.kulipai.luahook.databinding.ActivitySettingsBinding
import com.kulipai.luahook.mcp.McpForegroundService
import com.kulipai.luahook.mcp.McpManager
import com.kulipai.luahook.ui.about.AboutActivity
import java.net.Inet4Address
import java.net.NetworkInterface

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(inflater)
    }

    override fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun initEvent() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.language.setOnClickListener {
            showLanguagePickerDialog()
        }

        binding.about.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        binding.mcpCard.setOnClickListener {
            showStatus(McpManager.getMcpInfo(this))
        }

        binding.mcpSettings.setOnClickListener {
            showStatus(McpManager.getMcpInfo(this))
        }

        ShellManager.mode.observe(this) {
            updateMcpView()
        }
    }

    override fun onResume() {
        super.onResume()
        updateMcpView()
    }

    private fun updateMcpView() {
        val mcp = McpManager.getMcpInfo(this)
        binding.mcpDesc.text = when {
            !mcp.enabled -> getString(R.string.mcp_status_off)
            McpForegroundService.isListening() -> getString(R.string.mcp_status_on, mcp.port)
            else -> getString(R.string.mcp_status_down, mcp.port)
        }

        binding.mcpSwitch.setOnCheckedChangeListener(null)
        binding.mcpSwitch.isChecked = mcp.enabled
        binding.mcpSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (ShellManager.mode.value == ShellManager.Mode.NONE) {
                toast(R.string.Inactive_modules)
                updateMcpView()
                return@setOnCheckedChangeListener
            }
            if (McpManager.update(isChecked, mcp.port, this)) {
                McpManager.apply(this)
            }
            updateMcpView()
        }
    }

    private fun showLanguagePickerDialog() {
        val languages = arrayOf(
            resources.getString(R.string.follow_system),
            "Deutsch",
            "English",
            "Español",
            "Français",
            "हिन्दी",
            "日本語",
            "繁體中文",
            "简体中文",
            "한국어",
            "Português"
        )
        val languageCodes = arrayOf(
            LanguageUtils.LANGUAGE_FOLLOW_SYSTEM,
            LanguageUtils.LANGUAGE_GERMAN,
            LanguageUtils.LANGUAGE_ENGLISH,
            LanguageUtils.LANGUAGE_SPANISH,
            LanguageUtils.LANGUAGE_FRENCH,
            LanguageUtils.LANGUAGE_HINDI,
            LanguageUtils.LANGUAGE_JAPANESE,
            LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL,
            LanguageUtils.LANGUAGE_CHINESE_SIMPLIFIED,
            LanguageUtils.LANGUAGE_KOREAN,
            LanguageUtils.LANGUAGE_PORTUGUESE
        )

        var currentLanguage = LanguageUtils.getCurrentLanguage(this)
        if (currentLanguage == "zh") currentLanguage = LanguageUtils.LANGUAGE_CHINESE_SIMPLIFIED

        var checkedItem = languageCodes.indexOf(currentLanguage)
        if (checkedItem == -1) checkedItem = 0

        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.Select_language))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedLanguageCode = languageCodes[which]
                LanguageUtils.changeLanguage(this, selectedLanguageCode)
                recreate()
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /** 点卡片或齿轮：运行状态、本地和局域网的 /mcp 地址，以及接口说明。 */
    private fun showStatus(mcp: McpInfo) {
        val status = when {
            !mcp.enabled -> getString(R.string.mcp_status_off)
            McpForegroundService.isListening() -> getString(R.string.mcp_status_on, mcp.port).substringBefore(" ·")
            else -> getString(R.string.mcp_status_down, mcp.port).substringBefore(" ·")
        }
        val localUrl = "http://127.0.0.1:${mcp.port}/mcp"
        val lanIp = lanAddress()
        val lanUrl = lanIp?.let { "http://$it:${mcp.port}/mcp" }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
            addView(label(getString(R.string.mcp_status_line, status)))
            addView(addressRow(getString(R.string.mcp_local), localUrl))
            addView(addressRow(getString(R.string.mcp_lan), lanUrl ?: getString(R.string.mcp_no_lan), lanUrl))
            addView(label("\n" + getString(R.string.mcp_api_list)))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        MaterialAlertDialogBuilder(this)
            .setTitle("MCP")
            .setView(scroll)
            .setPositiveButton(R.string.mcp_port) { _, _ -> showPortDialog(McpManager.getMcpInfo(this)) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 一行地址加可见的复制按钮。没有局域网地址时不给复制。 */
    private fun addressRow(title: String, shown: String, copyUrl: String? = shown): LinearLayout {
        val text = label("$title\n$shown").apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 8)
            addView(text)
        }
        if (copyUrl != null) {
            val button = MaterialButton(this).apply {
                this.text = getString(R.string.mcp_copy)
                setOnClickListener { confirmCopy(copyUrl) }
            }
            row.addView(button)
        }
        return row
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextIsSelectable(true)
        }
    }

    /** 先问是否复制，再写入剪贴板。 */
    private fun confirmCopy(url: String) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.mcp_copy_ask) + "\n" + url)
            .setPositiveButton(R.string.sure) { _, _ ->
                val clipboard = this.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("mcp", url))
                toast(R.string.mcp_copied)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 第一块已启用、非回环、非链路本地的 IPv4。 */
    private fun lanAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            for (addr in intf.inetAddresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    private fun showPortDialog(mcp: McpInfo) {
        if (ShellManager.mode.value == ShellManager.Mode.NONE) {
            toast(R.string.Inactive_modules)
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_edit, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val edit = view.findViewById<TextInputEditText>(R.id.edit)
        inputLayout.hint = getString(R.string.mcp_port)
        edit.inputType = InputType.TYPE_CLASS_NUMBER
        edit.setText(mcp.port.toString())
        MaterialAlertDialogBuilder(this)
            .setTitle("MCP")
            .setView(view)
            .setPositiveButton(R.string.sure) { _, _ ->
                val port = edit.text?.toString()?.toIntOrNull()
                if (port == null || port !in 1..65535) {
                    toast(R.string.mcp_port_invalid)
                    return@setPositiveButton
                }
                if (McpManager.update(mcp.enabled, port, this)) {
                    McpManager.apply(this)
                    toast(R.string.save_ok)
                    updateMcpView()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(res: Int) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
    }
}
