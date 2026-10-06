package com.kulipai.luahook.ui.script.selector

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kulipai.luahook.R
import com.kulipai.luahook.data.model.AppInfo


fun getDynamicColor(context: Context, @AttrRes colorAttributeResId: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(colorAttributeResId, typedValue, true)
    return if (typedValue.resourceId != 0) {
        ContextCompat.getColor(context, typedValue.resourceId)
    } else {
        typedValue.data
    }
}


class SelectAppsAdapter(
    private var apps: List<AppInfo>,
    private val context: Context,
    private var selectApps: MutableList<String>
) :


    RecyclerView.Adapter<SelectAppsAdapter.AppsViewHolder>() {
    val pm: PackageManager = context.packageManager


    private fun getAppIcon(packageName: String): android.graphics.drawable.Drawable {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            pm.defaultActivityIcon
        }
    }

    inner class AppsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val version: TextView = itemView.findViewById(R.id.version)
        val card: MaterialCardView = itemView.findViewById(R.id.card)


        init {

            //跳转到app脚本编辑
            card.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION || pos !in apps.indices) return@setOnClickListener
                val currentPkg = apps[pos].packageName

                if (currentPkg in selectApps) {
                    card.cardElevation = 0.toFloat()

                    val icon = getAppIcon(currentPkg)
                    this@AppsViewHolder.icon.setImageDrawable(icon)
//                    icon.setImageDrawable(apps[adapterPosition].icon)
                    this@AppsViewHolder.icon.setColorFilter(0)
                    selectApps -= currentPkg
                } else {

                    card.cardElevation = 30.0.toFloat()
                    card.elevation = 0.0.toFloat()
                    icon.setImageResource(R.drawable.check_circle_24px)
                    icon.setColorFilter(
                        getDynamicColor(
                            context,
                            androidx.appcompat.R.attr.colorPrimary
                        )
                    )
                    selectApps += currentPkg
                }


            }


        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_apps, parent, false) // 加载自定义布局
        return AppsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppsViewHolder, position: Int) {

        holder.name.text = apps[position].appName
        holder.packageName.text = apps[position].packageName
        holder.version.text = apps[position].versionName
        if (apps[position].packageName in selectApps) {
            holder.card.cardElevation = 30.0.toFloat()
            holder.card.elevation = 0.0.toFloat()
            holder.icon.setImageResource(R.drawable.check_circle_24px)
            holder.icon.setColorFilter(
                getDynamicColor(
                    context,
                    androidx.appcompat.R.attr.colorPrimary
                )
            )
        } else {
            holder.card.cardElevation = 0.toFloat()

            val icon = getAppIcon(apps[position].packageName)
            holder.icon.setImageDrawable(icon)
//            holder.icon.setImageDrawable(apps[position].icon)
            holder.icon.setColorFilter(0)
        }

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