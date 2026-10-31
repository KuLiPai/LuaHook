pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
//    // 在这里集中管理所有插件的版本
//    plugins {
//        id("com.android.application") version "8.11.2"
//        id("com.android.library") version "8.11.2"
//        id("org.jetbrains.kotlin.android") version "2.3.0"
//        id("org.jetbrains.kotlin.jvm") version "2.3.0"
//    }
}



dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Add JitPack here for dependencies
    }
}

rootProject.name = "LuaHook"
include(":app")

val localPropsFile = file("local.properties")
val localProps = java.util.Properties()
if (localPropsFile.exists()) {
    localProps.load(java.io.FileInputStream(localPropsFile))
}

// libluahook 源码依赖策略（复合构建 Composite Build）：
// 1. 若 local.properties 中配置 useLocalLibLuaHook=false，则强制使用 JitPack/Maven 远程依赖。
// 2. 源码路径探测优先级：
//    a. local.properties 中配置的 libluahook.path（若存在且为有效目录）
//    b. 项目根目录下的 Git Submodule 子模块 ./libluahook
// 3. 若均不存在，自动回退到 JitPack/Maven 预编译依赖，保证新贡献者开箱即用。
val useLocalLibLuaHook = localProps.getProperty("useLocalLibLuaHook")?.toBooleanStrictOrNull() ?: true

if (useLocalLibLuaHook) {
    val customDir = localProps.getProperty("libluahook.path")
        ?.takeIf { it.isNotBlank() }
        ?.let { file(it) }
        ?.takeIf { it.isDirectory }

    val submoduleDir = file("libluahook")
        .takeIf { it.isDirectory && (file("libluahook/settings.gradle.kts").exists() || file("libluahook/settings.gradle").exists()) }

    val targetLibLuaHookDir = customDir ?: submoduleDir

    if (targetLibLuaHookDir != null) {
        // 自动将父工程的 Android SDK 路径同步给子模块，避免 includeBuild 报错 "SDK location not found"
        val parentSdkDir = localProps.getProperty("sdk.dir")
            ?: System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
        if (!parentSdkDir.isNullOrBlank()) {
            val childLocalProps = file("$targetLibLuaHookDir/local.properties")
            runCatching {
                if (!childLocalProps.exists()) {
                    childLocalProps.writeText("sdk.dir=${parentSdkDir.replace("\\", "/")}\n")
                } else {
                    val childProps = java.util.Properties().apply {
                        load(java.io.FileInputStream(childLocalProps))
                    }
                    if (childProps.getProperty("sdk.dir").isNullOrBlank()) {
                        childLocalProps.appendText("\nsdk.dir=${parentSdkDir.replace("\\", "/")}\n")
                    }
                }
            }
        }

        includeBuild(targetLibLuaHookDir) {
            dependencySubstitution {
                substitute(module("com.github.KuLiPai.libluahook:luahook-core")).using(project(":luahook-core"))
                substitute(module("com.github.KuLiPai.libluahook:luahook-ext-dexkit")).using(project(":luahook-ext-dexkit"))
                substitute(module("com.github.KuLiPai.libluahook:luahook-ext-layout")).using(project(":luahook-ext-layout"))
                substitute(module("com.github.KuLiPai.libluahook:luahook-ext-native")).using(project(":luahook-ext-native"))
            }
        }
    }
}
