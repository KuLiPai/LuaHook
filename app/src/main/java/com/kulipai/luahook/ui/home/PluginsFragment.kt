package com.kulipai.luahook.ui.home

import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kulipai.luahook.R
import com.kulipai.luahook.core.base.BaseFragment
import com.kulipai.luahook.core.plugin.PluginInfo
import com.kulipai.luahook.core.plugin.PluginManager
import com.kulipai.luahook.core.shell.ShellManager
import com.kulipai.luahook.databinding.FragmentHomePluginsBinding

class PluginsFragment : BaseFragment<FragmentHomePluginsBinding>() {

    private lateinit var adapter: PluginAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomePluginsBinding {
        return FragmentHomePluginsBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        adapter = PluginAdapter(mutableListOf(), onClick = { showPortDialog(it) }, onToggle = { plugin, enabled ->
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
