package com.kulipai.luahook.feature.manual

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialContainerTransform
import com.kulipai.luahook.R
import io.github.rosemoe.sora.widget.CodeEditor

class ManAdapter(
    private val title: List<String>,
    private val body: List<String>,
    private val root: CoordinatorLayout,
    private val details: MaterialCardView,
    private val editor: CodeEditor,

    private val listener: OnCardExpandListener
) :


    RecyclerView.Adapter<ManAdapter.ManViewHolder>() {

    inner class ManViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card) // 使用自定义布局中的 ID
        val title: TextView = itemView.findViewById(R.id.title)


        init {
            card.setOnClickListener {
                editor.setText(body[bindingAdapterPosition])
                startContainerTransition(root, it, details)
                listener.onCardExpanded(it) // ← 通知 MainActivity 当前展开的是哪个 View
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_man, parent, false) // 加载自定义布局
        return ManViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ManViewHolder, position: Int) {
//        holder.symbolTextView.text = symbols[position]
        holder.title.text = title[position]
    }

    override fun getItemCount(): Int {
        return title.size
    }

    fun startContainerTransition(root: View, v1: View, v2: View) {
        val transform = MaterialContainerTransform().apply {
            fadeMode = MaterialContainerTransform.FADE_MODE_IN // 0，对应 FADE_MODE_IN
            fitMode = MaterialContainerTransform.FIT_MODE_AUTO  // 0，对应 FIT_MODE_AUTO
            scrimColor = Color.TRANSPARENT
            startView = v1
            endView = v2
            addTarget(v2)
        }

        // 开始动画过渡
        TransitionManager.beginDelayedTransition(root as ViewGroup, transform)

        v1.visibility = View.INVISIBLE
        v2.visibility = View.VISIBLE
    }
}