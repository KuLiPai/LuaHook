package com.kulipai.luahook.data.model

data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean = false,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L
)