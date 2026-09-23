package com.kulipai.luahook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kulipai.luahook.databinding.ItemPluginCardBinding

class PluginAdapter : RecyclerView.Adapter<PluginAdapter.Holder>() {

    class Holder(val binding: ItemPluginCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPluginCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        // Reserved for future plugin features
    }

    override fun getItemCount(): Int = 0
}
