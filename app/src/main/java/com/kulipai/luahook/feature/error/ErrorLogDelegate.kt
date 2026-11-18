package com.kulipai.luahook.feature.error

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.kulipai.luahook.R
import java.io.File
import java.util.Date

object ErrorLogDelegate {

    private const val LOG_DIR = "logs"
    private const val LOG_PREFIX = "error_"
    private const val LOG_EXT = ".txt"

    /**
     * 将错误信息和堆栈跟踪保存到文件并分享。
     */
    context(context: Context)
    fun shareErrorLog(errorMessage: String, stackTrace: String): Boolean {

        val content = buildErrorLog(errorMessage, stackTrace)

        val file = saveLogToFile(content) ?: return false

        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // 对应 AndroidManifest.xml 中 <provider> 的 authorities
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain" // 设置MIME类型为纯文本
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_error_log_subject))
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_error_log_title)))
        return true
    }

    context(context: Context)
    private fun buildErrorLog(
        message: String,
        stack: String
    ) = context.getString(
        R.string.error_log_template,
        message,
        stack,
        "${Build.MANUFACTURER} ${Build.MODEL}",
        "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        context.packageName,
        Date().toString()
    )


    /**
     * 将字符串内容保存到应用缓存目录下的日志文件。
     * @param content 要保存的文本内容。
     * @return 保存的 File 对象，如果失败则返回 null。
     */
    context(context: Context)
    fun saveLogToFile(content: String): File? = runCatching {
        val dir = File(context.cacheDir, LOG_DIR).apply { mkdirs() }

        File(dir, "$LOG_PREFIX${System.currentTimeMillis()}$LOG_EXT")
            .apply { writeText(content) }
    }.getOrNull()

}