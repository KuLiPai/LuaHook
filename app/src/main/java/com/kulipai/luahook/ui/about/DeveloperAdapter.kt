package com.kulipai.luahook.ui.about

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kulipai.luahook.data.model.DeveloperInfo
import com.kulipai.luahook.databinding.ItemDeveloperBinding

class DeveloperAdapter(
    private val onItemClick: (DeveloperInfo) -> Unit
) : ListAdapter<DeveloperInfo, DeveloperAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemDeveloperBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeveloperBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.developerAvatar.setImageResource(item.avatarRes)
        holder.binding.developerName.text = item.name
        holder.binding.developerContribution.text = item.contribution
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DeveloperInfo>() {
            override fun areItemsTheSame(oldItem: DeveloperInfo, newItem: DeveloperInfo): Boolean =
                oldItem.githubUrl == newItem.githubUrl

            override fun areContentsTheSame(oldItem: DeveloperInfo, newItem: DeveloperInfo): Boolean =
                oldItem == newItem
        }
    }
}
