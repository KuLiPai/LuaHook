package com.kulipai.luahook.feature.home

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.kulipai.luahook.R
import com.kulipai.luahook.core.file.WorkspaceStorage
import com.kulipai.luahook.core.project.Project
import com.kulipai.luahook.databinding.ItemProjectCardBinding
import com.kulipai.luahook.feature.project.config.ProjectConfigActivity
import java.io.File

class ProjectAdapter(
    private val projects: MutableList<Project>,
    private val onProjectClick: (Project) -> Unit,
    private val onProjectToggle: (Project, Boolean) -> Unit,
    private val onProjectLongClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(val binding: ItemProjectCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        val context = holder.itemView.context
        holder.binding.projectSymbol.visibility = View.INVISIBLE
        holder.binding.projectImage.visibility = View.INVISIBLE


        holder.binding.projectName.text = project.name
        holder.binding.projectDesc.text = project.description

        // Handle Switch without triggering listener during binding
        holder.binding.projectSwitch.setOnCheckedChangeListener(null)
        holder.binding.projectSwitch.isChecked = project.isEnabled
        holder.binding.projectSwitch.setOnCheckedChangeListener { _, isChecked ->
            onProjectToggle(project, isChecked)
        }

        // Icon Logic
        if (isUnicodeIcon(project.icon)) {

            holder.binding.projectSymbol.visibility = View.VISIBLE
            holder.binding.projectSymbol.typeface =
                ResourcesCompat.getFont(context, R.font.material_icons)
            holder.binding.projectSymbol.text = project.icon

        }
        else if (project.icon.endsWith(".png") || project.icon.endsWith(".jpg")) {
            // Basic Image Loading
            val projectDir = "${WorkspaceStorage.Project}/${project.name}"
            val iconFile = File(projectDir, project.icon)

            val fullPath = WorkspaceStorage.DIR + projectDir + "/" + project.icon
            // However, projectDir already includes "Project/Name".
            // projectRepository sets project.icon to "icon.png" usually.

            // Check if I used correct path logic:
            // val projectDir = "${WorkspaceStorage.Project}/${project.name}" -> "/Project/Name"
            // val fullPath = DIR + projectDir + "/" + icon -> "/data.../LuaHook/Project/Name/icon.png"
            // This looks correct. But let's verify file existence first to avoid decoding issues.
            holder.binding.projectImage.visibility = View.VISIBLE

            val fileObj = File(WorkspaceStorage.DIR + projectDir, project.icon)
            if (fileObj.exists()) {
                val bitmap = BitmapFactory.decodeFile(fileObj.absolutePath)
                holder.binding.projectImage.setImageBitmap(bitmap)
                holder.binding.projectImage.scaleType =
                    ImageView.ScaleType.FIT_CENTER
            } else {
                holder.binding.projectImage.setImageResource(R.mipmap.ic_launcher)
            }
        } else if (project.icon.length == 1) {
            holder.binding.projectSymbol.visibility = View.VISIBLE
            holder.binding.projectSymbol.text = project.icon

        } else {
            holder.binding.projectImage.visibility = View.VISIBLE
            holder.binding.projectImage.setImageResource(R.mipmap.ic_launcher)
        }

        holder.itemView.setOnClickListener {
            // Click card -> Open Editor? Or Settings?
            // Usually Card -> Open Editor.
            onProjectClick(project)
        }

        // Settings button logic (if we had one separate from card click)
        // In XML I made settings button gone? 
        // Let's check item_project_card.xml... I put visibility="gone".
        // User asked for "Settings" behind name.
        // Let's enable it.
        holder.binding.projectSettings.visibility = View.VISIBLE
        holder.binding.projectSettings.setOnClickListener {
             val intent = Intent(context, ProjectConfigActivity::class.java)
             intent.putExtra("project_name", project.name)
             context.startActivity(intent)
        }
        
        holder.itemView.setOnClickListener {
            onProjectClick(project)
        }

        holder.itemView.setOnLongClickListener {
            onProjectLongClick(project)
            true
        }
    }

    private fun isUnicodeIcon(icon: String): Boolean {
        if (icon.isEmpty()) return false

        val codePoint = icon.codePointAt(0)
        return codePoint in 0xE000..0xEFFF
    }


    override fun getItemCount(): Int = projects.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newProjects: List<Project>) {
        projects.clear()
        projects.addAll(newProjects)
        notifyDataSetChanged()
    }
}
