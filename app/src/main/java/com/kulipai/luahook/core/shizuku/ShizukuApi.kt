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
import com.kulipai.luahook.core.file.WorkspaceFileManager
import com.kulipai.luahook.core.shell.ShellManager.Mode
import com.kulipai.luahook.core.shell.ShellManager.setMode
import com.kulipai.luahook.core.shell.ShellResult
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

object ShizukuApi {

    //
    private fun IBinder.wrap() = ShizukuBinderWrapper(this)
    private fun IInterface.asShizukuBinder() = this.asBinder().wrap()

    private var userService: IUserService? = null

    val isBinderAvailable = MutableLiveData(false)
    val isPermissionGranted = MutableLiveData(false)
    val isServiceConnected = MutableLiveData(false)

    fun init() {
        // 使用 Sticky 确保能获取到初始状态
        Shizuku.addBinderReceivedListenerSticky {
            val available = Shizuku.pingBinder()
            isBinderAvailable.value = available
            if (available) {
                isPermissionGranted.value = checkShizukuPermission()
            }
        }

        Shizuku.addBinderDeadListener {
            isBinderAvailable.value = false
            isPermissionGranted.value = false
            isServiceConnected.value = false
            userService = null
        }

    }


    val permissionListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            isPermissionGranted.value = grantResult == PackageManager.PERMISSION_GRANTED
        }

    fun checkShizukuPermission(): Boolean = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED


    fun requestShizuku() {
        if (isBinderAvailable.value == true && isPermissionGranted.value == false) {
            Shizuku.requestPermission(114514)
        }
    }


    fun bindShizuku(context: Context) {
        if (isBinderAvailable.value != true || isPermissionGranted.value != true) {
            // Err
            return
        }

        if (isServiceConnected.value == true) return

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
                    isServiceConnected.value = true
                    setMode(Mode.SHIZUKU)
                    WorkspaceFileManager.init(context)
                    com.kulipai.luahook.core.plugin.PluginManager.onWorkspaceReady(context)

                    // Successful
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {

                isServiceConnected.value = false
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