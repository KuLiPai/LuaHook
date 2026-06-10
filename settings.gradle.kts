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
include(":androlua")

