package com.kulipai.luahook.ui.script.setting

import android.view.LayoutInflater
import android.view.View
import com.kulipai.luahook.core.base.BaseActivity
import io.github.kulipai.luahook.core.androlua.LuaScriptUtils
import com.kulipai.luahook.core.log.e
import com.kulipai.luahook.databinding.ActivityScriptSetBinding
import io.github.kulipai.luahook.hook.api.LuaActivity
import io.github.kulipai.luahook.hook.api.LuaImport
import io.github.kulipai.luahook.hook.api.LuaUtil
import org.luaj.Globals
import org.luaj.LuaValue
import org.luaj.lib.ZeroArgFunction
import org.luaj.lib.jse.CoerceJavaToLua
import org.luaj.lib.jse.JsePlatform
import java.io.File
import androidx.activity.enableEdgeToEdge

class ScriptSetActivity : BaseActivity<ActivityScriptSetBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityScriptSetBinding {
        return ActivityScriptSetBinding.inflate(inflater)
    }

    override fun initData() {
        val path = intent.getStringExtra("path").toString()
        val script = read(path)
        val func = LuaScriptUtils.extractLuaFunctionByLabel(script, "set")
        
        func?.let {
            val funcLine = func.functionStartLine
            val callfunc = LuaScriptUtils.getFunctionName(func.functionCode)
            try {
                createGlobals().load("${func.functionCode}\n$callfunc()").call()
            } catch (e: Exception) {
                val err = LuaScriptUtils.simplifyLuaError(e.toString(), funcLine)
                binding.errText.text = err
                binding.errText.visibility = View.VISIBLE
                "${path.substringAfterLast("/")}:@set@:$err".e()
            }
        }
    }

    private fun read(path: String): String {
        return if (File(path).exists()) {
            File(path).readText()
        } else {
            ""
        }
    }

    private fun createGlobals(): Globals {
        val globals: Globals = JsePlatform.standardGlobals()

        // Load Lua modules
        globals["this"] = CoerceJavaToLua.coerce(this)
        globals["enableEdgeToEdge"] = CoerceJavaToLua.coerce(object : ZeroArgFunction() {
            override fun call(): LuaValue? {
                // BaseActivity already calls enableEdgeToEdge(), but keep for Lua compat
                enableEdgeToEdge()
                return LuaValue.NIL
            }
        })
        globals["activity"] = CoerceJavaToLua.coerce(this)
        LuaActivity(this).registerTo(globals)
        LuaUtil.loadBasicLib(globals)
        LuaImport(
            this::class.java.classLoader!!,
            this::class.java.classLoader!!
        ).registerTo(globals)
        LuaUtil.shell(globals)
        return globals
    }
}
