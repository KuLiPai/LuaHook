package com.kulipai.luahook.feature.logcat

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kulipai.luahook.R
import com.kulipai.luahook.app.di.AppContainer
import com.kulipai.luahook.core.shell.ShellMode
import com.kulipai.luahook.core.shell.StandaloneShellExecutor
import com.kulipai.luahook.data.repository.LogcatRepository
import com.kulipai.luahook.data.repository.LogcatRepositoryImpl
import com.petterp.floatingx.core.FloatingX
import com.petterp.floatingx.core.container.FxViewHolder
import com.petterp.floatingx.core.layout.FxAdsorb
import com.petterp.floatingx.core.layout.FxEdge
import com.petterp.floatingx.core.layout.FxGravity
import com.petterp.floatingx.core.update
import com.petterp.floatingx.system.permission.FxPermissionStrategy
import com.petterp.floatingx.system.systemHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingLogcatController(
    private val activity: ComponentActivity,
    private val logcatRepository: LogcatRepository? = null
) {

    companion object {
        const val LOGCAT_FLOATING_TAG = "logcat"
        const val LOGCAT_CACHE = "cache"
        const val LOGCAT_CLEAR_TIME = "logClearTime"
        const val LOGCAT_REFRESH_INTERVAL = 1_000L
    }

    private var floatingLogRefreshJob: Job? = null
    private var floatingLogMinimized = false
    private val floatingLogHistory = LogHistory()
    private var floatingLogError: String? = null

    private val defaultRepository: LogcatRepository by lazy {
        val executor = AppContainer.instanceOrNull?.shellExecutor ?: StandaloneShellExecutor()
        LogcatRepositoryImpl(executor)
    }

    private fun getRepository(): LogcatRepository {
        return logcatRepository
            ?: AppContainer.instanceOrNull?.logcatRepository
            ?: defaultRepository
    }

    /**
     * 控制悬浮窗的显示/隐藏/恢复
     */
    fun toggle() {
        val floatingLogcat = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG)
        if (floatingLogcat != null) {
            if (floatingLogMinimized) {
                restoreFloatingLogcat()
            } else if (floatingLogcat.isShowing) {
                stopFloatingLogRefresh()
                floatingLogcat.hide()
            } else {
                floatingLogcat.show()
                refreshFloatingLogcat()
            }
        } else {
            FloatingX.install(LOGCAT_FLOATING_TAG) {
                layout(R.layout.floating_logcat)
                anchor(FxGravity.TOP_START, dx = 24f, dy = 200f)
                adsorb(FxAdsorb.Edges(setOf(FxEdge.START, FxEdge.END)))
                systemHost(activity) {
                    theme(R.style.Theme_LuaHook)
                    permission(FxPermissionStrategy.auto())
                }
            }.also { control ->
                floatingLogMinimized = false
                control.updateContent(::bindFloatingLogcat)
                control.show()
            }
            refreshFloatingLogcat()
        }
    }

    /**
     * 手动触发刷新日志
     */
    fun refresh() {
        refreshFloatingLogcat()
    }

    /**
     * Shell 模式改变时的回调
     */
     fun onShellModeChanged(mode: ShellMode) {
         if (!floatingLogMinimized) {
             FloatingX.controlOrNull(LOGCAT_FLOATING_TAG)?.updateContent(::bindFloatingLogcat)
             refreshFloatingLogcat()
         }
     }

    private fun getShellMode(): ShellMode {
        return AppContainer.instanceOrNull?.shellExecutor?.mode?.value ?: ShellMode.NONE
    }

    /**
     * 销毁并清理悬浮窗资源
     */
    fun destroy() {
        stopFloatingLogRefresh()
        floatingLogMinimized = false
        FloatingX.uninstall(LOGCAT_FLOATING_TAG)
    }

    private fun bindFloatingLogcat(holder: FxViewHolder) {
        val recyclerView = holder.getView<RecyclerView>(R.id.LogRecyclerView)
        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        }
        if (recyclerView.adapter == null) {
            recyclerView.adapter = LogAdapter(
                logs = floatingLogHistory.entries.toMutableList(),
                isSelectableMode = false,
                onLogCopied = {
                    showInWindowToast(holder, activity.getString(R.string.log_copied))
                }
            )
        }
        recyclerView.itemAnimator = null

        updateFloatingLogcatStatus(holder)

        holder.getView<ImageButton>(R.id.btnClean).setOnClickListener {
            val clearTime = getRepository().getCurrentLogcatTimeFormat()
            activity.getSharedPreferences(LOGCAT_CACHE, Context.MODE_PRIVATE).edit {
                putString(LOGCAT_CLEAR_TIME, clearTime)
            }
            floatingLogHistory.clear()
            floatingLogError = null
            (recyclerView.adapter as? LogAdapter)?.clearLogs()
            updateFloatingLogcatStatus(holder)
            refreshFloatingLogcat()
            showInWindowToast(holder, activity.getString(R.string.clear_success))
        }
        holder.getView<ImageButton>(R.id.btnMinimize).setOnClickListener {
            minimizeFloatingLogcat()
        }
        holder.getView<ImageButton>(R.id.btnClose).setOnClickListener {
            stopFloatingLogRefresh()
            FloatingX.uninstall(LOGCAT_FLOATING_TAG)
        }
    }

    private fun updateFloatingLogcatStatus(holder: FxViewHolder) {
        val recyclerView = holder.getView<RecyclerView>(R.id.LogRecyclerView)
        val status = holder.getView<TextView>(R.id.noPower)
        val message = when {
            (recyclerView.adapter?.itemCount ?: 0) > 0 -> null
            getShellMode() == ShellMode.NONE -> activity.getString(R.string.no_power)
            floatingLogError != null -> activity.getString(R.string.logcat_read_failed, floatingLogError)
            recyclerView.adapter?.itemCount == 0 -> activity.getString(R.string.logcat_empty)
            else -> null
        }
        status.text = message
        status.visibility = if (message == null) View.GONE else View.VISIBLE
        recyclerView.visibility = if (message == null) View.VISIBLE else View.INVISIBLE
    }

    private fun showInWindowToast(holder: FxViewHolder, message: String) {
        val toastView = holder.getViewOrNull<TextView>(R.id.tvInWindowToast) ?: return
        toastView.text = message
        toastView.animate().cancel()
        toastView.alpha = 0f
        toastView.translationY = 24f
        toastView.visibility = View.VISIBLE
        toastView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(160)
            .withEndAction {
                toastView.postDelayed({
                    toastView.animate()
                        .alpha(0f)
                        .translationY(16f)
                        .setDuration(200)
                        .withEndAction {
                            toastView.visibility = View.GONE
                        }
                        .start()
                }, 900)
            }
            .start()
    }

    private fun stopFloatingLogRefresh() {
        floatingLogRefreshJob?.cancel()
        floatingLogRefreshJob = null
    }

    private fun refreshFloatingLogcat() {
        val control = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) ?: return
        if (floatingLogMinimized) return

        stopFloatingLogRefresh()
        floatingLogRefreshJob = activity.lifecycleScope.launch {
            while (isActive && !floatingLogMinimized &&
                FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) === control
            ) {
                // The window may still be waiting for overlay permission after show().
                if (control.isShowing) {
                    control.updateContent(::updateFloatingLogcatStatus)
                    if (getShellMode() != ShellMode.NONE) {
                        val result = getRepository().readSystemLogsByTagSince(
                            "LuaXposed",
                            activity.getSharedPreferences(LOGCAT_CACHE, Context.MODE_PRIVATE)
                                .getString(LOGCAT_CLEAR_TIME, null)
                        )
                        if (!isActive || floatingLogMinimized || !control.isShowing ||
                            FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) !== control
                        ) return@launch

                        floatingLogError = result.exceptionOrNull()?.message
                        result.getOrNull()?.let { logs ->
                            val appended = floatingLogHistory.appendSnapshot(logs)
                            if (appended.isEmpty()) return@let
                            control.updateContent { holder ->
                                val recyclerView = holder.getView<RecyclerView>(R.id.LogRecyclerView)
                                val adapter = recyclerView.adapter as? LogAdapter ?: return@updateContent
                                val followLatest = adapter.itemCount == 0 || !recyclerView.canScrollVertically(1)
                                adapter.appendLogs(appended)
                                if (followLatest) {
                                    recyclerView.scrollToPosition(adapter.itemCount - 1)
                                }
                            }
                        }
                        control.updateContent(::updateFloatingLogcatStatus)
                    }
                }
                delay(LOGCAT_REFRESH_INTERVAL)
            }
        }
    }

    private fun minimizeFloatingLogcat() {
        val control = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) ?: return
        floatingLogMinimized = true
        stopFloatingLogRefresh()
        control.update {
            layout(R.layout.floating_logcat_ball)
        }
        control.updateContent { holder ->
            holder.view.setOnClickListener {
                restoreFloatingLogcat()
            }
            holder.view.setOnLongClickListener {
                stopFloatingLogRefresh()
                FloatingX.uninstall(LOGCAT_FLOATING_TAG)
                floatingLogMinimized = false
                true
            }
        }
    }

    private fun restoreFloatingLogcat() {
        val control = FloatingX.controlOrNull(LOGCAT_FLOATING_TAG) ?: return
        floatingLogMinimized = false
        control.update {
            layout(R.layout.floating_logcat)
        }
        control.updateContent(::bindFloatingLogcat)
        refreshFloatingLogcat()
    }
}
