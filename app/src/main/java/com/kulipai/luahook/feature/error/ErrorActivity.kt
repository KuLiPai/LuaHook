package com.kulipai.luahook.feature.error

import android.content.Intent
import android.view.LayoutInflater
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kulipai.luahook.core.base.BaseActivity
import com.kulipai.luahook.databinding.ActivityErrorBinding

/**
 * ErrorActivity 是一个专门用于显示应用程序错误信息的 activity。
 * 它从 Intent 中接收错误消息和堆栈跟踪，并将其显示在 UI 上。
 */
class ErrorActivity : BaseActivity<ActivityErrorBinding>() {

    companion object {
        // 用于 Intent 传递错误信息的键
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_STACK_TRACE = "stack_trace"
    }

    private lateinit var errorMessage: String
    private lateinit var stackTrace: String

    override fun inflateBinding(inflater: LayoutInflater): ActivityErrorBinding {
        return ActivityErrorBinding.inflate(inflater)
    }

    override fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
    }

    override fun initData() {
        // 从 Intent 中获取错误信息
        errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "未知错误"
        stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "无堆栈跟踪信息"
        binding.tvErrorMessage.text = errorMessage
    }

    override fun initEvent() {
        binding.sendErrMsg.setOnClickListener {
            ErrorLogDelegate.shareErrorLog(errorMessage, stackTrace)
        }

        binding.closeApp.setOnClickListener {
            finish()
        }

        // 设置重启应用程序按钮的点击监听器
        binding.btnRestartApp.setOnClickListener {
            // 重启应用程序到主 activity
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // 关闭当前错误 activity
        }
    }
}
