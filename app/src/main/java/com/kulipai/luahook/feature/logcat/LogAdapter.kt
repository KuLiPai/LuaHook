package com.kulipai.luahook.feature.logcat

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kulipai.luahook.R

class LogAdapter(
    private var logs: MutableList<String>,
    private val isSelectableMode: Boolean = false,
    private val onLogCopied: ((String) -> Unit)? = null
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card)
        val timeText: TextView = itemView.findViewById(R.id.time)
        val logText: TextView = itemView.findViewById(R.id.log)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val rawLog = logs[position]
        holder.timeText.text = rawLog.substringBefore(".")

        val logContent = when {
            rawLog.contains("LuaXposed: ") -> rawLog.substringAfter("LuaXposed: ")
            rawLog.contains("LuaXposed : ") -> rawLog.substringAfter("LuaXposed : ")
            else -> rawLog
        }
        holder.logText.text = logContent

        if (isSelectableMode) {
            // 日志页面：保留原生划选复制文本，不拦截卡片点击
            holder.logText.setTextIsSelectable(true)
            holder.card.isClickable = false
            holder.card.isFocusable = false
            holder.card.foreground = null
            holder.card.setOnTouchListener(null)
            holder.card.setOnClickListener(null)
            holder.logText.setOnClickListener(null)
        } else {
            // 悬浮窗：关闭文本划选，启用整行卡片单击复制与视觉动画反馈
            holder.logText.setTextIsSelectable(false)
            holder.logText.isClickable = false
            holder.logText.isFocusable = false
            holder.card.isClickable = true
            holder.card.isFocusable = true

            val typedValue = TypedValue()
            if (holder.card.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)) {
                holder.card.foreground = ContextCompat.getDrawable(holder.card.context, typedValue.resourceId)
            }

            // 关键：在手指按下（ACTION_DOWN）瞬间提前移除 FLAG_NOT_FOCUSABLE 并请求焦点，
            // 彻底解决 Android 10+ 在浮窗覆盖其他 App 时拦截剪切板写入（mWm.isUidFocused == false）的问题
            holder.card.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    FloatingClipboardHelper.prepareFocus(v)
                }
                false
            }

            holder.card.setOnClickListener { v ->
                // 显式轻量透明度反馈动画，确保在各系统悬浮窗图层下有清晰的点击效果
                holder.card.animate().cancel()
                holder.card.alpha = 0.4f
                holder.card.animate().alpha(1.0f).setDuration(160).start()

                // 纯净复制：优先读取视图当前渲染出来的文本，防御边界情况
                val textToCopy = holder.logText.text?.toString()?.ifEmpty { logContent } ?: logContent
                FloatingClipboardHelper.copy(v, textToCopy) {
                    if (onLogCopied != null) {
                        onLogCopied.invoke(textToCopy)
                    } else {
                        // 兜底 Toast（通常用于非悬浮窗场景）
                        val context = v.context
                        Toast.makeText(context, context.getString(R.string.log_copied), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    fun appendLogs(newLogs: List<String>) {
        if (newLogs.isEmpty()) return
        val start = logs.size
        logs.addAll(newLogs)
        notifyItemRangeInserted(start, newLogs.size)
    }

    fun clearLogs() {
        val count = logs.size
        if (count == 0) return
        logs.clear()
        notifyItemRangeRemoved(0, count)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateLogs(newLogs: MutableList<String>) {
        if (logs == newLogs) return
        logs = newLogs
        notifyDataSetChanged()
    }
}
