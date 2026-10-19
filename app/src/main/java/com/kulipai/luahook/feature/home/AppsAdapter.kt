package com.kulipai.luahook.feature.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.feature.script.manager.MultiScriptActivity
import com.kulipai.luahook.data.model.AppInfo

class AppsAdapter(private var apps: List<AppInfo>, private val context: Context) :
    RecyclerView.Adapter<AppsAdapter.AppsViewHolder>() {
    val pm: PackageManager = context.packageManager

    @SuppressLint("NotifyDataSetChanged")
    inner class AppsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val name: TextView = itemView.findViewById(R.id.name)
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val version: TextView = itemView.findViewById(R.id.version)
        val card: MaterialCardView = itemView.findViewById(R.id.card)


        init {

            //跳转到app脚本编辑
            card.setOnClickListener{
                val intent = Intent(context, MultiScriptActivity::class.java)
                intent.putExtra("packageName", apps[bindingAdapterPosition].packageName)
                intent.putExtra("appName", apps[bindingAdapterPosition].appName)
                context.startActivity(intent)
            }

            card.setOnLongClickListener {

                MaterialAlertDialogBuilder(context)
                    .setTitle(context.resources.getString(R.string.tips))
                    .setMessage(context.resources.getString(R.string.confirm_deletion))
                    .setPositiveButton(context.resources.getString(R.string.sure)) { dialog, _ ->
                        val savedList = AppContainer.instance.workspaceStorage.readStringList("/apps.txt")
                        savedList.remove(apps[bindingAdapterPosition].packageName)
                        AppContainer.instance.workspaceStorage.writeStringList("/apps.txt", savedList)
                        val availableAppsToShow: List<AppInfo> = apps.filter { appInfo ->
                            savedList.contains(appInfo.packageName)
                            // 或者写成: appInfo.packageName !in selectedPackagesSet
                        }
                        apps = availableAppsToShow
                        notifyDataSetChanged()
                        dialog.dismiss()
                    }
                    .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                true
            }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_apps, parent, false) // 加载自定义布局
        return AppsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {
        holder.name.text= apps[position].appName
        holder.packageName.text= apps[position].packageName
        holder.version.text= apps[position].versionName
//        holder.icon.setImageDrawable(apps[position].icon)

        val icon = pm.getApplicationIcon(pm.getApplicationInfo(apps[position].packageName, 0))
        holder.icon.setImageDrawable(icon)

    }

    override fun getItemCount(): Int {
        return apps.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<AppInfo>) {
        apps = newList
        notifyDataSetChanged()
    }


}