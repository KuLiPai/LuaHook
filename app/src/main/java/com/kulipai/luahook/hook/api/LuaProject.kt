package com.kulipai.luahook.hook.api

import com.kulipai.luahook.core.file.WorkspaceFileManager
import org.luaj.LuaValue
import org.luaj.lib.OneArgFunction

/**
 * 项目路径与目录解析 API 绑定（同时支持工作区项目与独立打包模块）。
 */
class LuaProject(
    val projectName: String = "",
    val customProjectDir: String? = null
) {
    fun registerTo(env: LuaValue) {
        if (projectName.isEmpty() && customProjectDir.isNullOrEmpty()) {
            return
        }

        val baseDir = if (!customProjectDir.isNullOrEmpty()) {
            if (customProjectDir.endsWith("/")) customProjectDir else "$customProjectDir/"
        } else {
            WorkspaceFileManager.DIR + WorkspaceFileManager.Project + "/" + projectName + "/"
        }

        val dirFunc = object : OneArgFunction() {
            override fun call(p0: LuaValue): LuaValue {
                if (p0 == NIL || !p0.isstring()) {
                    return valueOf(baseDir)
                }
                val sub = p0.tojstring().trim()
                if (sub.isEmpty()) {
                    return valueOf(baseDir)
                }
                val cleanSub = sub.removePrefix("./").trimStart('/')
                return valueOf(baseDir + cleanSub)
            }
        }

        env["getProjectDir"] = dirFunc
        env["getProjectPath"] = dirFunc
        env["projectDir"] = LuaValue.valueOf(baseDir)
        env["projectPath"] = LuaValue.valueOf(baseDir)
    }
}
