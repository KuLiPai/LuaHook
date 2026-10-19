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
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.data.repository.ScriptRepository
import com.kulipai.luahook.ui.script.editor.app.AppsEdit
import com.kulipai.luahook.ui.script.setting.ScriptSetActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray

class MultScriptAdapter(
    private val conf: MutableList<MutableMap.MutableEntry<String, Any?>>,
    private val currentPackageName: String,
    private val appName: String,
    private val context: Context,
    private val launcher: ActivityResultLauncher<Intent>,
    private val scriptRepository: ScriptRepository = AppContainer.instance.scriptRepository,
    private val adapterScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : RecyclerView.Adapter<MultScriptAdapter.MultScriptViewHolder>() {

    companion object {
        fun extractConfig(value: Any?): Triple<Boolean, String, String> {
            return when (value) {
                is JSONArray -> Triple(
                    value.optBoolean(0, true),
                    value.optString(1, ""),
                    value.optString(2, "v1.0")
                )
                is Array<*> -> Triple(
                    (value.getOrNull(0) as? Boolean) ?: true,
                    (value.getOrNull(1) as? String) ?: "",
                    (value.getOrNull(2) as? String) ?: "v1.0"
                )
                is Boolean -> Triple(value, "", "v1.0")
                else -> Triple(true, "", "v1.0")
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId", "InflateParams")
    inner class MultScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card)
        val scriptName: TextView = itemView.findViewById(R.id.name)
        val description: TextView = itemView.findViewById(R.id.description)
        val logoText: TextView = itemView.findViewById(R.id.logoText)
        val switchWidget: MaterialSwitch = itemView.findViewById(R.id.switchWidget)
        val setButton: Button = itemView.findViewById(R.id.setButton)

        init {
            card.setOnClickListener {
                val (_, desc, _) = extractConfig(conf[bindingAdapterPosition].value)
                val intent = Intent(context, AppsEdit::class.java)
                intent.putExtra("packageName", currentPackageName)
                intent.putExtra("appName", appName)
                intent.putExtra("scripName", conf[bindingAdapterPosition].key)
                intent.putExtra("scriptDescription", desc)
                launcher.launch(intent)
            }

            card.setOnLongClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_scriptdetail, null)
                val bsdLogoText: TextView = view.findViewById(R.id.logoText)
                val bsdName: TextView = view.findViewById(R.id.name)
                val bsdDescription: TextView = view.findViewById(R.id.description)
                val bsdAuthor: TextView = view.findViewById(R.id.author)

                val bsd = BottomSheetDialog(context)
                val name = conf[bindingAdapterPosition].key
                bsdLogoText.text = name.firstOrNull()?.toString() ?: ""
                bsdName.text = name

                val (_, desc, _) = extractConfig(conf[bindingAdapterPosition].value)
                bsdDescription.text = desc.takeUnless { it.isEmpty() } ?: "无"

                adapterScope.launch {
                    val scriptContent = scriptRepository.readAppScript(currentPackageName, name)
                    val param = scriptRepository.parseParameters(scriptContent)
                    bsdAuthor.text = param?.author ?: "无"
                }

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
                intent.putExtra(
                    "path",
                    "/data/local/tmp/LuaHook/AppScript/$currentPackageName/${conf[bindingAdapterPosition].key}.lua"
                )
                context.startActivity(intent)
            }

            switchWidget.setOnClickListener {
                val (_, desc, ver) = extractConfig(conf[bindingAdapterPosition].value)
                val scriptName = conf[bindingAdapterPosition].key
                val isChecked = switchWidget.isChecked

                conf[bindingAdapterPosition].setValue(
                    JSONArray().put(isChecked).put(desc).put(ver)
                )

                adapterScope.launch {
                    scriptRepository.setAppScriptEnabled(currentPackageName, scriptName, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultScriptViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multscript, parent, false)
        return MultScriptViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MultScriptViewHolder, position: Int) {
        val (enabled, desc, _) = extractConfig(conf[position].value)
        holder.scriptName.text = conf[position].key
        holder.logoText.text = conf[position].key.firstOrNull()?.toString() ?: ""
        holder.switchWidget.isChecked = enabled
        if (desc.isNotEmpty()) {
            holder.description.text = desc
        }
    }

    override fun getItemCount(): Int = conf.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(new: MutableList<MutableMap.MutableEntry<String, Any?>>) {
        conf.clear()
        conf.addAll(new)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(position: Int, context: Context) {
        val scriptName = conf[position].key
        MaterialAlertDialogBuilder(context)
            .setTitle(context.resources.getString(R.string.tips))
            .setMessage(context.resources.getString(R.string.confirm_deletion))
            .setPositiveButton(context.resources.getString(R.string.sure)) { dialog, _ ->
                conf.removeAt(position)
                notifyDataSetChanged()
                adapterScope.launch {
                    scriptRepository.deleteAppScript(currentPackageName, scriptName)
                }
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
}