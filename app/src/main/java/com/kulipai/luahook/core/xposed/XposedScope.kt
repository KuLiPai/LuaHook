package com.kulipai.luahook.core.xposed

import android.content.Context
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/**
 * api101+模块作用域管理
 */

object XposedScope {
    private var _service: XposedService? = null
    val service get() = _service

    inline fun withService(onService: (XposedService) -> Unit) {
        service?.let { onService(it) }
    }

    fun requestScope(context: Context, pkg: String) {
        withService {
            if (!it.scope.contains(pkg)) {
                it.requestScope(listOf(pkg), object : XposedService.OnScopeEventListener {})
            }
        }
    }

    fun requestManyScope(context: Context, pkgList: MutableList<String>, index: Int) {
        withService {
            val toRequest = pkgList.filter { pkg -> !it.scope.contains(pkg) }
            if (toRequest.isNotEmpty()) {
                it.requestScope(toRequest, object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(approved: List<String>) {
                        // Batch request approved
                    }

                    override fun onScopeRequestFailed(message: String) {
                        // Batch request failed
                    }
                })
            }
        }
    }

    fun removeScope(context: Context, pkg: String) {
        withService {
            if (it.scope.contains(pkg)) {
                it.removeScope(listOf(pkg))
            }
        }
    }

    fun init() {
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                _service = service
            }

            override fun onServiceDied(service: XposedService) {
                _service = null
            }
        })
    }
}