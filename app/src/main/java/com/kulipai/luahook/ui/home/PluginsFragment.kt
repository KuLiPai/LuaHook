package com.kulipai.luahook.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.core.plugin.PluginInfo
import com.kulipai.luahook.core.plugin.PluginManager
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.databinding.FragmentHomePluginsBinding
import com.kulipai.luahook.mcp.McpForegroundService
import java.net.Inet4Address
import java.net.NetworkInterface

class PluginsFragment : BaseFragment<FragmentHomePluginsBinding>() {

    private lateinit var adapter: PluginAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomePluginsBinding {
        return FragmentHomePluginsBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        adapter = PluginAdapter(mutableListOf(), onClick = { showStatus(it) }, onToggle = { plugin, enabled ->
            if (ShellManager.mode.value == ShellManager.Mode.NONE) {
                toast(R.string.Inactive_modules)
                load()
                return@PluginAdapter
            }
            if (PluginManager.update(enabled, plugin.port)) {
                PluginManager.apply(requireContext())
            }
            load()
        })
        binding.pluginRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.pluginRecycler.adapter = adapter
        ShellManager.mode.observe(viewLifecycleOwner) { load() }
    }

    override fun onResume() {
        super.onResume()
        if (_ready()) load()
    }

    private fun _ready(): Boolean = view != null

    private fun load() {
        if (view == null) return
        val plugin = PluginManager.mcpPlugin()
        adapter.submit(listOfNotNull(plugin))
    }

    private fun showStatus(plugin: PluginInfo) {
        val status = when {
            !plugin.enabled -> getString(R.string.mcp_status_off)
            McpForegroundService.isListening() -> getString(R.string.mcp_status_on, plugin.port).substringBefore(" ·")
            else -> getString(R.string.mcp_status_down, plugin.port).substringBefore(" ·")
        }
        val localUrl = "http://127.0.0.1:${plugin.port}/mcp"
        val lanIp = lanAddress()
        val lanUrl = lanIp?.let { "http://$it:${plugin.port}/mcp" }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
            addView(label(getString(R.string.mcp_status_line, status)))
            addView(addressRow(getString(R.string.mcp_local), localUrl))
            addView(addressRow(getString(R.string.mcp_lan), lanUrl ?: getString(R.string.mcp_no_lan), lanUrl))
            addView(label("\n" + getString(R.string.mcp_api_list)))
        }
        val scroll = ScrollView(requireContext()).apply { addView(content) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(plugin.name)
            .setView(scroll)
            .setPositiveButton(R.string.mcp_port) { _, _ -> showPortDialog(plugin) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addressRow(title: String, shown: String, copyUrl: String? = shown): LinearLayout {
        val text = label("$title\n$shown").apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 16, 0, 8)
            addView(text)
        }
        if (copyUrl != null) {
            val button = MaterialButton(requireContext()).apply {
                this.text = getString(R.string.mcp_copy)
                setOnClickListener { confirmCopy(copyUrl) }
            }
            row.addView(button)
        }
        return row
    }

    private fun label(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextIsSelectable(true)
        }
    }

    private fun confirmCopy(url: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.mcp_copy_ask) + "\n" + url)
            .setPositiveButton(R.string.sure) { _, _ ->
                val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("mcp", url))
                toast(R.string.mcp_copied)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

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

    private fun showPortDialog(plugin: PluginInfo) {
        if (ShellManager.mode.value == ShellManager.Mode.NONE) {
            toast(R.string.Inactive_modules)
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_edit, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val edit = view.findViewById<TextInputEditText>(R.id.edit)
        inputLayout.hint = getString(R.string.mcp_port)
        edit.inputType = InputType.TYPE_CLASS_NUMBER
        edit.setText(plugin.port.toString())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(plugin.name)
            .setView(view)
            .setPositiveButton(R.string.sure) { _, _ ->
                val port = edit.text?.toString()?.toIntOrNull()
                if (port == null || port !in 1..65535) {
                    toast(R.string.mcp_port_invalid)
                    return@setPositiveButton
                }
                if (PluginManager.update(plugin.enabled, port)) {
                    PluginManager.apply(requireContext())
                    toast(R.string.save_ok)
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(res: Int) {
        Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()
    }
}
