package com.kulipai.luahook.core.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.IInterface
import android.os.RemoteException
import androidx.lifecycle.MutableLiveData
import com.kulipai.luahook.BuildConfig
import com.kulipai.luahook.core.shell.ShellResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

object ShizukuApi {

    private fun IBinder.wrap() = ShizukuBinderWrapper(this)
    private fun IInterface.asShizukuBinder() = this.asBinder().wrap()

    private var userService: IUserService? = null

    // 响应式 StateFlow 状态流
    private val _isBinderAvailableFlow = MutableStateFlow(false)
    val isBinderAvailableFlow: StateFlow<Boolean> = _isBinderAvailableFlow.asStateFlow()

    private val _isPermissionGrantedFlow = MutableStateFlow(false)
    val isPermissionGrantedFlow: StateFlow<Boolean> = _isPermissionGrantedFlow.asStateFlow()

    private val _isServiceConnectedFlow = MutableStateFlow(false)
    val isServiceConnectedFlow: StateFlow<Boolean> = _isServiceConnectedFlow.asStateFlow()

    // 遗留 LiveData 兼容字段（保留供 UI 层读取，如 HomeFragment）
    val isBinderAvailable = MutableLiveData(false)
    val isPermissionGranted = MutableLiveData(false)
    val isServiceConnected = MutableLiveData(false)

    fun init() {
        // 使用 Sticky 确保能获取到初始状态
        Shizuku.addBinderReceivedListenerSticky {
            val available = Shizuku.pingBinder()
            _isBinderAvailableFlow.value = available
            isBinderAvailable.postValue(available)
            if (available) {
                val granted = checkShizukuPermission()
                _isPermissionGrantedFlow.value = granted
                isPermissionGranted.postValue(granted)
            }
        }

        Shizuku.addBinderDeadListener {
            _isBinderAvailableFlow.value = false
            _isPermissionGrantedFlow.value = false
            _isServiceConnectedFlow.value = false
            isBinderAvailable.postValue(false)
            isPermissionGranted.postValue(false)
            isServiceConnected.postValue(false)
            userService = null
        }
    }

    val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _isPermissionGrantedFlow.value = granted
            isPermissionGranted.postValue(granted)
        }

    fun checkShizukuPermission(): Boolean = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestShizuku() {
        if (_isBinderAvailableFlow.value && !_isPermissionGrantedFlow.value) {
            Shizuku.requestPermission(114514)
        }
    }

    fun bindShizuku(context: Context) {
        if (!_isBinderAvailableFlow.value || !_isPermissionGrantedFlow.value) {
            return
        }

        if (_isServiceConnectedFlow.value) return

        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, UserService::class.java.name)
        ).daemon(false)
            .processNameSuffix("adb")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        Shizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder != null && binder.pingBinder()) {
                    userService = IUserService.Stub.asInterface(binder)
                    _isServiceConnectedFlow.value = true
                    isServiceConnected.postValue(true)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                _isServiceConnectedFlow.value = false
                isServiceConnected.postValue(false)
                userService = null
            }
        })
    }

    fun execShell(cmd: String): ShellResult {
        val service = userService
            ?: return ShellResult.Error("Service not bound")

        return try {
            val result = service.exec(cmd)
            if (result.success) {
                ShellResult.Success(result.output)
            } else {
                ShellResult.Error(result.output)
            }
        } catch (e: RemoteException) {
            ShellResult.Error(
                stderr = "RemoteException",
                throwable = e
            )
        }
    }
}