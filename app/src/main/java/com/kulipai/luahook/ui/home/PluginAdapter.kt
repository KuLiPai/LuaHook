package com.kulipai.luahook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kulipai.luahook.R
import com.kulipai.luahook.core.plugin.PluginInfo
import com.kulipai.luahook.databinding.ItemPluginCardBinding
import com.kulipai.luahook.mcp.McpForegroundService
import com.kulipai.luahook.mcp.McpNetworkAccess

class PluginAdapter(
    private val plugins: MutableList<PluginInfo>,
    private val onClick: (PluginInfo) -> Unit,
    private val onToggle: (PluginInfo, Boolean) -> Unit,
) : RecyclerView.Adapter<PluginAdapter.Holder>() {

    class Holder(val binding: ItemPluginCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPluginCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val plugin = plugins[position]
        val context = holder.itemView.context
        holder.binding.pluginName.text = plugin.name
        holder.binding.pluginDesc.text = when {
            !plugin.enabled -> context.getString(R.string.mcp_status_off)
            !McpForegroundService.isListening() -> context.getString(R.string.mcp_status_down, plugin.port)
            McpNetworkAccess.needsPermission(context) -> context.getString(R.string.mcp_status_lan_blocked)
            else -> context.getString(R.string.mcp_status_on, plugin.port)
        }
        holder.binding.pluginSwitch.setOnCheckedChangeListener(null)
        holder.binding.pluginSwitch.isChecked = plugin.enabled
        holder.binding.pluginSwitch.setOnCheckedChangeListener { _, checked ->
            onToggle(plugin, checked)
        }
        holder.binding.pluginSettings.setOnClickListener { onClick(plugin) }
        holder.itemView.setOnClickListener { onClick(plugin) }
    }

    override fun getItemCount(): Int = plugins.size

    fun submit(next: List<PluginInfo>) {
        plugins.clear()
        plugins.addAll(next)
        notifyDataSetChanged()
    }
}
