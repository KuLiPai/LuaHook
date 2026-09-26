# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn javax.script.ScriptEngineFactory

-keep class org.luckypray.** {*;}
-keep class top.sacz.xphelper.** {*;}
-keep class com.androlua.** { *; }
-keep class com.kulipai.luahook.** {*;}
-keepclassmembers class com.kulipai.luahook.** {*;}
# 保留 libluahook 核心与扩展（被 C++ JNI 反射调用及 Lua 动态绑定的类）
-keep class io.github.kulipai.luahook.** {*;}
-keepclassmembers class io.github.kulipai.luahook.** {*;}
# 保留所有 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}
# 保留整个 LuaJ 库
-keep class org.luaj.** { *; }
-keepclassmembers class org.luaj.** { *; }
-keep class org.luaj.vm2.** { *; }
-keepclassmembers class org.luaj.vm2.** { *; }
-dontwarn org.luaj.**
-keep class com.myopicmobile.** { *; }
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
-dontwarn com.androlua.**
-dontwarn kotlin.Cloneable$DefaultImpls
-keep class org.eclipse.tm4e.languageconfiguration.internal.model.** { *; }
-keep class org.joni.ast.** { *; }
-keep class io.kulipai.sora.luaj.** { *; }
-dontwarn io.kulipai.sora.luaj.**
-keep class io.dingyi222666.sora.lua.tool.** { *; }
-dontwarn io.github.rosemoe.oniguruma.OnigNative
-keep class org.nanohttpd.protocols.http.** { *; }
-keep class com.kongzue.** { *; }