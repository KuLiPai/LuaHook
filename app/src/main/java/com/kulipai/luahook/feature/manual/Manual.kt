package com.kulipai.luahook.feature.manual

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialContainerTransform
import com.kulipai.luahook.R
import com.kulipai.luahook.feature.script.editor.SoraEditorDelegate.initEditor
import io.github.rosemoe.sora.widget.CodeEditor


class Manual : AppCompatActivity(), OnCardExpandListener {

    private val rec: RecyclerView by lazy { findViewById(R.id.rec) }
    private val container: CoordinatorLayout by lazy { findViewById(R.id.main) }
    private val detail: MaterialCardView by lazy { findViewById(R.id.detail) }
    private val editor: CodeEditor by lazy { findViewById(R.id.editor) }

    private var currentCard: View? = null

    // 仅在 Android 13+ 使用
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private lateinit var backCallback: OnBackInvokedCallback


    override fun onCardExpanded(startView: View) {
        currentCard = startView // 保存当前展开的卡片 View
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                onBackPressed() // 触发你刚刚写的逻辑
            }
        }



        enableEdgeToEdge()
        setContentView(R.layout.activity_manual)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initEditor(editor)



        val title =
            listOf(
                resources.getString(R.string.Tag1),
                resources.getString(R.string.Tag2),
                resources.getString(R.string.Tag3),
                resources.getString(R.string.Tag4),
                resources.getString(R.string.Tag5),
                resources.getString(R.string.Tag6),
                resources.getString(R.string.Tag7),
                resources.getString(R.string.Tag8),
                resources.getString(R.string.Tag9),
                resources.getString(R.string.Tag11),
                resources.getString(R.string.Tag12),
                resources.getString(R.string.Tag13),


            )

        val body = listOf(
            """
                lpparam.packageName
            """.trimIndent(),
            """
                hook {
                  class = "com.xx",           -- 必填：目标类的全限定名字符串 或 Class 类型的类对象
                  --classloader = lpparam.classLoader, -- 可选：用于加载类的 ClassLoader，不填默认 lpparam.classLoader
                  method = "f1",              -- 必填：要 Hook 的方法名
                  params = {"int", "String", findClass("com.my.class", lpparam.classLoader)}, -- 可选：一个 table，包含参数类型字符串或 Class 对象
                  before = function(it)
                    -- before hook：在原方法执行前执行
                    print("进入 f1 方法前")
                    it.args[0] = "xxx"  -- 修改参数0的值
                  end,
                  after = function(it)
                    -- after hook：在原方法执行后执行
                    print("离开 f1 方法后，返回值:", it.result)
                    it.result = "yyy" -- 修改返回值
                  end,
                }
            """.trimIndent(),
            """
                findClass("com.xxx",lpparam.classLoader)
            """.trimIndent(),
            """
                -- 静态方法
                class.fun()
                
                -- 示例
                class().fun()
                
                --例如
                hook {
                    class = "class", 
                    classloader = lpparam.classLoader,
                    method = "fun",
                    params = {"int"},
                    before = function(it)
                        it.thisObject.check(1)
                    end,
                    after = function(it)
                    
                    end
                }
                
                --例如
                class = findClass("com.xxx",lpparam.classLoader)
                class.login("123","abc")
            """.trimIndent(),
            """
                --获取
                getField(类,字段名)
                
                --设置
                setField(类,字段名,设置值)
                
                 --例如
                 hook {
                    class = "class", 
                    classloader = lpparam.classLoader,
                    method = "fun",
                    params = {"int"},
                    before = function(it) --假设arg0是个类 == Class{"name":"hhi",isVip=0}
                        log(getField(it.args[0],"name"))
                        setField(it.args[0],"isVip",1)
                    end,
                    after = function(it)
                    
                    end
                }
                
            """.trimIndent(),
            """
                http.get(url [,head,cookie],callback)
                
                http.post(url,data[,head,cookie],callback)
                
                --例如
                http.get("https://baidu.com",function(a,b)
                    --a 状态码 
                    --b body内容
                    if a==200 then
                        log(b)
                    end
                end)
                
                --例如
                http.post("https://baidu.com","name=a&password=b",function(a,b)
                    --a 状态码 
                    --b body内容
                    if a==200 then
                        log(b)
                    end
                end)
            """.trimIndent(),
            """
                -- 方法1: 直接使用Java的File类
                File("path").isFile()
                -- ...
                
                -- 方法2: 使用分装函数file类
                -- 判断文件或目录是否存在
                print("是否是文件:", file.isFile("/sdcard/demo.txt"))
                print("是否是目录:", file.isDir("/sdcard/"))
                print("是否存在:", file.isExists("/sdcard/demo.txt"))

                -- 写入字符串
                file.write("/sdcard/demo.txt", "Hello, LuaJ!")
                -- 追加字符串
                file.append("/sdcard/demo.txt", "\nAppend line 1.")
                -- 读取内容
                local content = file.read("/sdcard/demo.txt")
                print("文件内容:\n" .. content)

                -- 写入二进制（可用于图片、音频等）
                file.writeBytes("/sdcard/demo.bin", "binary\x00data")
                -- 追加二进制
                file.appendBytes("/sdcard/demo.bin", "\xFF\xAA")
                -- 读取二进制
                local bin = file.readBytes("/sdcard/demo.bin")
                print("读取的二进制内容（字符串显示）:\n" .. bin)

                -- 文件复制与移动
                file.copy("/sdcard/demo.txt", "/sdcard/demo_copy.txt")
                file.move("/sdcard/demo_copy.txt", "/sdcard/demo_moved.txt")

                -- 文件重命名（同目录）
                file.rename("/sdcard/demo_moved.txt", "demo_renamed.txt")

                -- 删除文件
                file.delete("/sdcard/demo_renamed.txt")

                -- 获取文件名与大小
                print("文件名:", file.getName("/sdcard/demo.txt"))
                print("文件大小:", file.getSize("/sdcard/demo.txt"), "bytes")
            """.trimIndent(),
            """
                --decode / encode
                
                local data = json.decode('{"name":"Xposed","version":1.0}')
                print("模块名:", data.name)
    
                local encoded = json.encode({status="ok", count=3})
                print("编码结果:", encoded)
            """.trimIndent(),
            """
                -- imports 优先引入宿主类，如果宿主找不到就是模块的类
                -- import 引入模块的类，支持通配符*
                --全局引入一个类
                imports "android.widget.Toast"
                Toast.makeText(activity,"hello",1000).show()
                
                --自定义名称
                AAA = imports "android.widget.Toast"
                AAA.makeText(activity,"hello",1000).show()
            """.trimIndent(),
            """
                -- 异步加载图片,isCache 是 是否启用缓存
                -- loadDrawableAsync(url,isCache,callback)
                loadDrawableAsync("https://aaa.png",true,function(drawable)
                       log(drawable) -- 获取图片drawable属性
                end)
                
                -- 加载本地图片
                -- drawable = loadDrawableFromFile(path,isCache)
                drawable = loadDrawableFromFile("/sdcard/a.png",true)
                
                -- 清除缓存
                -- key 特定的缓存键，如果为null且clearAll为true则清除所有缓存，clearAll 是否清除所有缓存，返回是否成功清除缓存
                -- clearDrawableCache(key,clearAll)
                clearDrawableCache(nil,true) --清除所有缓存
            """.trimIndent(),
            """
                -- 示例1：获取字符串 (需要有效的 Context 对象)
                local appNameResId = resources.getResourceId(hostContext, "app_name", "string")
                if appNameResId and appNameResId ~= 0 then
                  local appName = resources.getString(hostContext, "app_name")
                  if appName then
                    print("App Name: " .. appName)
                   else
                    print("Failed to get string for 'app_name'")
                  end
                 else
                  print("Resource ID for 'app_name' not found.")
                end

                -- 示例2：获取 Drawable (需要有效的 Context 对象)
                -- 注意：直接在Lua中使用Drawable对象可能意义不大，除非你有特定的库或方法来处理它
                -- 通常更有用的是获取资源ID，然后在需要的地方使用它（例如，在其他Java调用中）
                local launcherIcon = resources.getDrawable(hostContext, "ic_launcher")
                if launcherIcon then
                  print("Got launcher icon Drawable object: " .. tostring(launcherIcon))
                  -- 你可能无法直接在纯Lua中显示这个Drawable
                  -- 可以尝试获取它的类名等信息
                  print("Icon Class: " .. launcherIcon:getClass():getName())
                 else
                  print("Failed to get drawable 'ic_launcher'")
                end

                -- 示例3：获取颜色 (需要有效的 Context 对象)
                local primaryColor = resources.getColor(hostContext, "colorPrimary")
                if primaryColor and primaryColor ~= 0 then -- 检查非0，因为0可能是有效颜色（透明黑）也可能是错误
                  print(string.format("Primary Color (int): %d (Hex: #%08x)", primaryColor, primaryColor))
                 else
                  print("Failed to get color 'colorPrimary'")
                end

                -- 示例4：获取指定包名的资源 (假设目标包已安装且有权限访问)
                -- local otherAppIcon = resources.getDrawable(hostContext, "some_icon", "com.other.app")
                -- if otherAppIcon then
                --     print("Got icon from another app!")
                -- else
                --     print("Failed to get icon from com.other.app")
                -- end

                -- 示例5: 获取所有 drawable 资源的名称和 ID
                local drawableConstants = resources.getRConstants(hostContext, "drawable")
                if drawableConstants then
                  print("Drawable Resources:")
                  local count = 0
                  for name, id in pairs(drawableConstants) do
                    print(string.format("  %s = %d", name, id))
                    count = count + 1
                    if count > 10 then -- 限制打印数量
                      print("  ... and more")
                      break
                    end
                  end
                 else
                  print("Failed to get drawable constants.")
                end
            """.trimIndent(),
            """
                -- 顾名思义 hook构造方法
                hookctor {
                    class = "class", 
                    classloader = lpparam.classLoader,
                    params = {"int"},
                    before = function(it)
                    
                    end,
                    after = function(it)
                    
                    end
                }
            """.trimIndent(),


        )

        rec.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rec.adapter = ManAdapter(title, body, container, detail, editor, this)


        //Todo 插入编辑框等操作

    }


    @SuppressLint("GestureBackNavigation")
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (detail.isVisible && currentCard != null) {
            // 执行反向动画
            val transform = MaterialContainerTransform().apply {
                startView = detail
                endView = currentCard
                addTarget(currentCard!!)
                duration = 300L
                scrimColor = Color.TRANSPARENT
                fadeMode = MaterialContainerTransform.FADE_MODE_OUT
                fitMode = MaterialContainerTransform.FIT_MODE_AUTO
            }

            TransitionManager.beginDelayedTransition(container, transform)
            detail.visibility = View.INVISIBLE
            currentCard!!.visibility = View.VISIBLE

            currentCard = null // 重置
        } else {
            super.onBackPressed() // 正常返回
        }
    }


}


interface OnCardExpandListener {
    fun onCardExpanded(startView: View)
}
