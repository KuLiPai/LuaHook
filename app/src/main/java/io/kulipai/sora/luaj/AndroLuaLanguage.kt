package io.kulipai.sora.luaj

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

class AndroLuaLanguage(
) : LuaLanguage {

    private val privateKeywords = mutableListOf<String>()

    private val privatePackages = mutableMapOf<String, MutableList<CompletionName>>()

    private val languageNames = mutableListOf<String>()

    private val autoCompleter: AutoCompleter = AndroLuaAutoCompleter(this)

    val names: List<String>
        get() = languageNames

    val keywords: List<String>
        get() = privateKeywords

    val packages: Map<String, List<CompletionName>>
        get() = privatePackages

    var showDiagnostic = true

    private val uiHandler = Handler(Looper.getMainLooper())
    private var onDiagnosticListener: LuaLanguage.OnDiagnosticListener? = null

    override fun setOnDiagnosticListener(listener: LuaLanguage.OnDiagnosticListener?) {
        onDiagnosticListener = listener
    }

    init {
        privateKeywords.addAll(AndroLuaLanguage.keywords)

        languageNames.addAll(AndroLuaLanguage.names)


        addBasePackage("io", package_io.split('|'));
        addBasePackage("string", package_string.split('|'));
        addBasePackage("luajava", package_luajava.split('|'));
        addBasePackage("os", package_os.split('|'));
        addBasePackage("table", package_table.split('|'));
        addBasePackage("math", package_math.split('|'));
        addBasePackage("utf8", package_utf8.split('|'));
        addBasePackage("coroutine", package_coroutine.split('|'));
        addBasePackage("package", package_package.split('|'));
        addBasePackage("debug", package_debug.split('|'));



        addBasePackage(
            "lpparam",
            package_lpparam.split('|')
        )
        addBasePackage(
            "suparam",
            package_suparam.split('|')
        )
        addBasePackage(
            "dexkitbridge",
            package_DexKitBridge.split('|')
        )
        addBasePackage(
            "dexfinder",
            package_DexFinder.split('|')
        )
        addBasePackage(
            "http",
            package_http.split('|')
        )
        addBasePackage(
            "resources",
            package_res.split('|')
        )
        addBasePackage(
            "file",
            package_file.split('|')
        )
        addBasePackage(
            "json",
            package_json.split('|')
        )

        addBasePackage(
            "sp",
            package_sp.split('|')
        )

        addBasePackage(
            "xphelper",
            package_XpHelper.split('|')
        )


        addBasePackage(
            "xsp",
            package_xsp.split('|')
        )

        addBasePackage(
            "native",
            package_native.split('|')
        )


    }

    fun addBasePackage(name: String, words: List<String>) {
        privatePackages[name] = mutableListOf()
        for (word in words) {
            privatePackages[name]?.add(
                CompletionName(
                    word,
                    CompletionItemKind.Function,
                    " :function"
                )
            )
        }
    }

    fun addBasePackageByKind(name: String, words: List<CompletionName>) {
        privatePackages[name] = mutableListOf()
        for (word in words) {
            privatePackages[name]?.addAll(words)
        }
    }

    fun isBasePackage(name: String): Boolean {
        return privatePackages.containsKey(name)
    }

    fun getBasePackage(name: String): List<CompletionName>? {
        return privatePackages[name]
    }

    fun addNames(names: List<String>) {
        languageNames.addAll(names)
    }

    fun removeBasePackage(name: String) {
        privatePackages.remove(name)
    }


    override fun requireAutoComplete(
        reference: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val result = autoCompleter.requireAutoComplete(
            reference,
            position,
            publisher,
            extraArguments
        )

        if (result != null && showDiagnostic) {
           uiHandler.post {
               onDiagnosticListener?.onDiagnosticsChanged(result)
           }
        }
    }

    companion object {
        //private final static String functionTarget   = "_ENV|_G|_VERSION|assert|collectgarbage|coroutine|create|isyieldable|resume|running|status|wrap|yield|debug|gethook|getinfo|getlocal|getmetatable|getregistry|getupvalue|getuservalue|sethook|setlocal|setmetatable|setupvalue|setuservalue|traceback|upvalueid|upvaluejoin|dofile|error|getfenv|getmetatable|io|close|flush|input|lines|open|output|popen|read|stderr|stdin|stdout|tmpfile|type|write|ipairs|load|loadfile|loadstring|luajava|bindClass|clear|coding|createArray|createProxy|instanceof|loadLib|loaded|luapath|new|newInstance|package|math|abs|acos|asin|atan|atan2|ceil|cos|cosh|deg|exp|floor|fmod|frexp|huge|ldexp|log|log10|max|maxinteger|min|mininteger|modf|pi|pow|rad|random|randomseed|sin|sinh|sqrt|tan|tanh|tointeger|type|ult|module|next|os|clock|date|difftime|execute|exit|getenv|remove|rename|setlocale|time|tmpname|package|config|cpath|loaded|loaders|loadlib|path|preload|searchers|searchpath|seeall|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|setfenv|setmetatable|string|byte|char|dump|find|format|gfind|gmatch|gsub|len|lower|match|pack|packsize|rep|reverse|sub|unpack|upper|table|concat|foreach|foreachi|insert|maxn|move|pack|remove|sort|unpack|tonumber|tostring|type|unpack|char|charpattern|utf8|codepoint|codes|len|offset|xpcall";
        //private final static String functionTarget1sx   = "_ENV|_G|_VERSION|assert|collectgarbage|coroutine.create|coroutine.isyieldable|coroutine.resume|coroutine.running|coroutine.status|coroutine.wrap|coroutine.yield|debug.debug|debug.gethook|debug.getinfo|debug.getlocal|debug.getmetatable|debug.getregistry|debug.getupvalue|debug.getuservalue|debug.sethook|debug.setlocal|debug.setmetatable|debug.setupvalue|debug.setuservalue|debug.traceback|debug.upvalueid|debug.upvaluejoin|dofile|error|getfenv|getmetatable|io.close|io.flush|io.input|io.lines|io.open|io.output|io.popen|io.read|io.stderr|io.stdin|io.stdout|io.tmpfile|io.type|io.write|ipairs|load|loadfile|loadstring|luajava.bindClass|luajava.clear|luajava.coding|luajava.createArray|luajava.createProxy|luajava.instanceof|luajava.loadLib|luajava.loaded|luajava.luapath|luajava.new|luajava.newInstance|luajava.package|math.abs|math.acos|math.asin|math.atan|math.atan2|math.ceil|math.cos|math.cosh|math.deg|math.exp|math.floor|math.fmod|math.frexp|math.huge|math.ldexp|math.log|math.log10|math.max|math.maxinteger|math.min|math.mininteger|math.modf|math.pi|math.pow|math.rad|math.random|math.randomseed|math.sin|math.sinh|math.sqrt|math.tan|math.tanh|math.tointeger|math.type|math.ult|module|next|os.clock|os.date|os.difftime|os.execute|os.exit|os.getenv|os.remove|os.rename|os.setlocale|os.time|os.tmpname|package.config|package.cpath|package.loaded|package.loaders|package.loadlib|package.path|package.preload|package.searchers|package.searchpath|package.seeall|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|setfenv|setmetatable|string.byte|string.char|string.dump|string.find|string.format|string.gfind|string.gmatch|string.gsub|string.len|string.lower|string.match|string.pack|string.packsize|string.rep|string.reverse|string.sub|string.unpack|string.upper|table.concat|table.foreach|table.foreachi|table.insert|table.maxn|table.move|table.pack|table.remove|table.sort|table.unpack|tonumber|tostring|type|unpack|utf8.char|utf8.charpattern|utf8.codepoint|utf8.codes|utf8.len|utf8.offset|xpcall";
        private const val keywordTarget =
            "and|break|case|catch|continue|default|defer|do|else|elseif|end|false|finally|for|function|goto|if|in|lambda|local|nil|not|or|repeat|return|switch|then|true|try|until|when|while"
        private const val globalTarget =
            "self|__add|__band|__bnot|__bor|__bxor|__call|__close|__concat|__div|__eq|__gc|__idiv|__index|__le|__len|__lt|__mod|__mul|__newindex|__pow|__shl|__shr|__sub|__tostring|__unm|_ENV|_G|assert|collectgarbage|dofile|error|getfenv|getmetatable|ipairs|load|loadfile|loadstring|module|next|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|self|setfenv|setmetatable|tointeger|tonumber|tostring|type|unpack|xpcall"

        private const val packageName =
            "coroutine|debug|io|luajava|math|os|package|string|table|utf8"
        private val package_coroutine: String =
            "create|isyieldable|resume|running|status|wrap|yield"
        private val package_debug: String =
            "debug|gethook|getinfo|getlocal|getmetatable|getregistry|getupvalue|getuservalue|sethook|setlocal|setmetatable|setupvalue|setuservalue|traceback|upvalueid|upvaluejoin"
        private val package_io: String =
            "close|flush|info|input|isdir|lines|ls|mkdir|open|output|popen|read|readall|stderr|stdin|stdout|tmpfile|type|write"
        private val package_luajava: String =
            "astable|bindClass|clear|coding|createArray|createProxy|getContext|instanceof|loadLib|loaded|luapath|new|newArray|newInstance|override|package|tostring"
        private val package_math: String =
            "abs|acos|asin|atan|atan2|ceil|cos|cosh|deg|exp|floor|fmod|frexp|huge|ldexp|log|log10|max|maxinteger|min|mininteger|modf|pi|pow|rad|random|randomseed|sin|sinh|sqrt|tan|tanh|tointeger|type|ult"
        private val package_os: String =
            "clock|date|difftime|execute|exit|getenv|remove|rename|setlocale|time|tmpname"
        private val package_package: String =
            "config|cpath|loaded|loaders|loadlib|path|preload|searchers|searchpath|seeall"
        private val package_string: String =
            "byte|char|dump|find|format|gfind|gmatch|gsub|len|lower|match|rep|reverse|sub|upper"
        private val package_table: String =
            "clear|clone|concat|const|find|foreach|foreachi|gfind|insert|maxn|move|pack|remove|size|sort|unpack"
        private val package_utf8: String =
            "byte|char|charpattern|charpos|codepoint|codes|escape|find|fold|gfind|gmatch|gsub|insert|len|lower|match|ncasecmp|next|offset|remove|reverse|sub|title|upper|width|widthindex"
        private val extFunctionTarget: String =
            "activity|call|compile|dump|each|enum|import|loadbitmap|loadlayout|loadmenu|service|set|task|thread|timer"




        private const val package_lpparam =
            "packageName|classLoader|appInfo|isFirstApplication|processName"
        private const val package_suparam =
            "startsSystemServer|modulePath"
        private const val package_http = "get|post|upload|delete|put|postJson|head|download"
        private const val package_sp = "clear|getAll|remove|contains|get|set"
        private const val package_XpHelper =
            "initContext|initZygote|setConfigPath|setConfigPassword|injectResourcesToContext|moduleApkPath"
        private const val package_xsp = "getAll|contains|get|reload"
        private const val package_DexKitBridge =
            "close|initFullCache|setThreadNum|getDexNum|exportDexFile|batchFindClassUsingStrings|batchFindMethodUsingStrings|findClass|findMethod|findField|getClassData|getMethodData|getFieldData|getCallMethods|getInvokeMethods|create"
        private const val package_json = "encode|decode"
        private const val package_DexFinder =
            "setAutoCloseTime|create|getDexKitBridge|findMethod|findField|findClass|clearCache|resetTimer|close"
        private const val package_res = "getRConstants|getColor|getString|getResourceId|getDrawable"
        private const val package_file =
            "isFile|isDir|isExists|read|readBytes|write|writeBytes|append|appendBytes|copy|move|rename|delete|getName|getSize"

        private const val package_native =
            "memory|ptr|module_base|getModuleBase|resolve_symbol|readPoint|get_module_base|hook|new_function"


        val luaHookKey = listOf(
            "hook",
            "replace",
//            "Xposed",
            "import",
            "log",
            "setField",
            "getField",
            "invoke",
            "file",
            "http",
            "sp",
            "xsp",
            "lpparam",
            "suparam",
            "json",
            "imports",
//            "new",
//            "newInstance",
//            "getConstructor",
            "clearDrawableCache",
            "loadDrawableFromFile",
            "loadDrawableAsync",
            "loadDrawableSync",
            "resources",
            "hookcotr",
            "hookctor",
            "injectActivity",
            "XposedBridge",
            "XposedHelpers",
//            "createProxy",
            "DexKitBridge",
            "findClass",
            "DexFinder",
            "hookAll",
            "XpHelper",
            "arrayOf",
            "Task",
            "callMethod",
            "getStaticField",
            "setStaticField",
            "native",
            "loadDex",
            "printFields",
            "printStackTrace",
            "getAppLanguage"
        )



        private val functionTarget: String =
            "$globalTarget|$extFunctionTarget"
        private val keywords =
            keywordTarget.split('|')

        private val names =
            functionTarget.split('|')+luaHookKey

    }
}

data class CompletionName(
    val name: CharSequence,
    val type: CompletionItemKind,
    val description: CharSequence = "",
    val label: CharSequence = name
)