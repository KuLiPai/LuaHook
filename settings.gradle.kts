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

// 只有在 local.properties 中配置了 useLocalLibLuaHook=true 才会使用本地源码替换，
// 这样你推代码上去时，只要别人没有加这个配置，就不会走本地构建，也就不会报错！
if (localProps.getProperty("useLocalLibLuaHook") == "true") {
    includeBuild("/home/kulipai/Projects/AndroidStudioProjects/libluahook") {
        dependencySubstitution {
            substitute(module("com.github.KuLiPai.libluahook:luahook-core")).using(project(":luahook-core"))
            substitute(module("com.github.KuLiPai.libluahook:luahook-ext-dexkit")).using(project(":luahook-ext-dexkit"))
            substitute(module("com.github.KuLiPai.libluahook:luahook-ext-layout")).using(project(":luahook-ext-layout"))
            substitute(module("com.github.KuLiPai.libluahook:luahook-ext-native")).using(project(":luahook-ext-native"))
        }
    }
}
