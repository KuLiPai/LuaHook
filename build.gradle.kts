// 顶层构建文件，用于配置所有子项目/模块的通用选项

// 配置项目级插件

plugins {
    // Android应用程序插件，用于构建Android应用
    alias(libs.plugins.android.application) apply false

    // Kotlin Compose插件，用于Jetpack Compose UI开发
    alias(libs.plugins.kotlin.compose) apply false


    // Android库插件，用于构建Android库模块
    alias(libs.plugins.android.library) apply false
}