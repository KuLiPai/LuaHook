package com.kulipai.luahook.core.file

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 工作区文件变更通知中心。
 * 基于 Kotlin 协程与 SharedFlow 实现事件流分发，
 * 当 MCP 工具、后台任务或工作区通过 WorkspaceFileManager 修改/删除文件时，
 * 触发流事件，订阅端通过协程与生命周期感知（如 repeatOnLifecycle）优雅收集。
 */
object WorkspaceEventManager {

    private val _fileEvents = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val fileEvents: SharedFlow<String> = _fileEvents.asSharedFlow()

    fun notifyFileChanged(relativePath: String) {
        val normalizedPath = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        _fileEvents.tryEmit(normalizedPath)
    }
}
