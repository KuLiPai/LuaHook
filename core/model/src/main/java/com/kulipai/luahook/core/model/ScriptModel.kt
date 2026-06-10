package com.kulipai.luahook.core.model

data class HomeOverview(
    val isPrBuild: Boolean,
    val moduleEnabled: Boolean,
    val runtimeMode: String,
    val enabledScriptApps: Int,
    val enabledModuleCount: Int,
    val hasNewVersion: Boolean,
    val androidVersion: String,
    val appVersion: String,
    val lsposedVersion: String?,
    val selinuxStatus: String,
    val scriptStorage: String,
)

data class ScriptApp(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val uid: Int,
    val scriptCount: Int,
    val systemApp: Boolean,
    val enabled: Boolean,
    val scripts: List<ScriptDefinition>,
)

data class ScriptDefinition(
    val id: String,
    val title: String,
    val description: String,
    val enabled: Boolean,
    val content: String,
)

data class DemoModule(
    val id: String,
    val name: String,
    val info: String,
    val version: String,
    val author: String,
    val description: String,
    val enabled: Boolean,
    val canRun: Boolean,
    val hasSettings: Boolean,
    val hasConfig: Boolean,
    val files: List<ModuleFile>,
)

data class ModuleFile(
    val path: String,
    val content: String,
    val isDirectory: Boolean = false,
)

data class LogEntry(
    val time: String,
    val level: String,
    val tag: String,
    val message: String,
)
