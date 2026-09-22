package com.kulipai.luahook.ui.script.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.kulipai.luahook.R
import com.kulipai.luahook.ui.script.editor.app.AppsEdit
import com.kulipai.luahook.ui.script.setting.ScriptSetActivity
import com.kulipai.luahook.core.file.WorkspaceFileManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray

class MultScriptAdapter(
    private val conf: MutableList<MutableMap.MutableEntry<String, Any?>>,
    private val currentPackageName: String,
    private val appName: String,
    private val context: Context,
    private val launcher: ActivityResultLauncher<Intent>
) :


    RecyclerView.Adapter<MultScriptAdapter.MultScriptViewHolder>() {

    val path = WorkspaceFileManager.AppConf + "/" + currentPackageName + ".txt"


    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId", "InflateParams")
    @OptIn(DelicateCoroutinesApi::class)
    inner class MultScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card)
        val scriptName: TextView = itemView.findViewById(R.id.name)
        val description: TextView = itemView.findViewById(R.id.description)
        val logoText: TextView = itemView.findViewById(R.id.logoText)
        val switchWidget: MaterialSwitch = itemView.findViewById(R.id.switchWidget)
        val setButton: Button = itemView.findViewById(R.id.setButton)


        init {

            card.setOnClickListener {

                val innerList = conf[bindingAdapterPosition].value as JSONArray
                // 进入编辑界面
                val intent = Intent(context, AppsEdit::class.java)
                intent.putExtra("packageName", currentPackageName)
                intent.putExtra("appName", appName)
                intent.putExtra("scripName", conf[bindingAdapterPosition].key)
                intent.putExtra("scriptDescription", innerList[1].toString())
                launcher.launch(intent)

            }
            card.setOnLongClickListener {

                val view = LayoutInflater.from(context).inflate(R.layout.dialog_scriptdetail, null)
                val bsdLogoText: TextView = view.findViewById(R.id.logoText)
                val bsdName: TextView = view.findViewById(R.id.name)
                val bsdDescription: TextView = view.findViewById(R.id.description)
                val bsdAuthor: TextView = view.findViewById(R.id.author)

                var bsd = BottomSheetDialog(context)

                bsdLogoText.text = conf[bindingAdapterPosition].key[0].toString()
                bsdName.text = conf[bindingAdapterPosition].key

                val innerList = conf[bindingAdapterPosition].value as JSONArray

                bsdDescription.text = (innerList[1] as String).takeUnless { it.isEmpty() } ?: "无"

                val param =
                    WorkspaceFileManager.parseParameters(read(WorkspaceFileManager.DIR + "/" + WorkspaceFileManager.AppScript + "/" + currentPackageName + "/" + conf[bindingAdapterPosition].key + ".lua"))

                bsdAuthor.text = param?.author ?: "无"


                val window = bsd.window
                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                bsd.setContentView(view)
                bsd.show()



                true
            }

            setButton.setOnClickListener {
                val intent = Intent(context, ScriptSetActivity::class.java)
                intent.putExtra("path", WorkspaceFileManager.DIR + WorkspaceFileManager.AppScript + "/" + currentPackageName + "/" + conf[bindingAdapterPosition].key + ".lua")
                context.startActivity(intent)
            }

            switchWidget.setOnClickListener {

                val innerList = conf[bindingAdapterPosition].value as JSONArray

                conf[bindingAdapterPosition].setValue(
                    JSONArray(
                        arrayOf(
                            switchWidget.isChecked,
                            innerList[1],
                            innerList[2]
                        )
                    )
                )

                GlobalScope.launch(Dispatchers.IO) {
                    WorkspaceFileManager.writeMap(path, conf.associate { it.key to it.value }.toMutableMap())
                }
            }



        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultScriptViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multscript, parent, false) // 加载自定义布局
        return MultScriptViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MultScriptViewHolder, position: Int) {
        val innerList = conf[position].value as JSONArray
        holder.scriptName.text = conf[position].key
        holder.logoText.text = conf[position].key[0].toString()
        holder.switchWidget.isChecked = innerList[0] as Boolean
        if (innerList[1].toString().isNotEmpty()) {
            holder.description.text = innerList[1] as String
        }


//            holder.timeText.text = logs[position].substringBefore(".")
//            holder.logText.text = logs[position].substringAfter("LuaXposed: ")
    }

    override fun getItemCount(): Int {
        return conf.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(new: MutableList<MutableMap.MutableEntry<String, Any?>>) {
        conf.clear()
        conf.addAll(new)
        notifyDataSetChanged() // 通知 RecyclerView 更新数据
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(position: Int, context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.resources.getString(R.string.tips))
            .setMessage(context.resources.getString(R.string.confirm_deletion))
            .setPositiveButton(context.resources.getString(R.string.sure)) { dialog, _ ->
                WorkspaceFileManager.rm(WorkspaceFileManager.AppScript + "/" + currentPackageName + "/" + conf[position].key + ".lua")
                conf.removeAt(position)
                GlobalScope.launch(Dispatchers.IO) {
                    WorkspaceFileManager.writeMap(
                        path,
                        conf.associate { it.key to it.value }.toMutableMap()
                    )

                }
                notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ ->
                notifyDataSetChanged()
                dialog.dismiss()
            }
            .setOnDismissListener {
                notifyDataSetChanged()
            }
            .show()


    }

    fun read(path: String): String {
        val relative = if (path.startsWith(WorkspaceFileManager.DIR)) {
            path.removePrefix(WorkspaceFileManager.DIR)
        } else {
            path
        }
        return WorkspaceFileManager.read(relative)
    }
}