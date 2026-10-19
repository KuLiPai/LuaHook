package com.kulipai.luahook.core.crash

import android.content.Context
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * CustomCrashHandler 是一个自定义的未捕获异常处理器。
 * 它会捕获应用程序中任何未捕获的异常，然后委托给 crashHandler 展示错误或退出。
 *
 * @param context 应用程序上下文
 * @param crashHandler 崩溃回调，用于启动错误界面展示堆栈
 */
class AppCrashHandler(
    private val context: Context,
    private val crashHandler: ((context: Context, errorMessage: String, stackTrace: String) -> Unit)? = null
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        val errorMessage = e.localizedMessage ?: "未知错误"
        val stackTrace = getStackTrace(e)

        try {
            crashHandler?.invoke(context, errorMessage, stackTrace)
        } catch (_: Exception) {
        }

        // 终止当前进程，防止应用程序在显示错误后继续运行在不稳定的状态
        Process.killProcess(Process.myPid())
        exitProcess(1)
    }

    private fun getStackTrace(e: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        return sw.toString()
    }

    companion object {
        fun init(
            context: Context,
            crashHandler: ((context: Context, errorMessage: String, stackTrace: String) -> Unit)? = null
        ) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (defaultHandler !is AppCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    AppCrashHandler(context.applicationContext, crashHandler)
                )
            }
        }
    }
}