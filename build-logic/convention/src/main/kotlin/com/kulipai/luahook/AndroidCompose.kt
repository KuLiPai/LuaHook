package com.kulipai.luahook

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * 配置 Android Compose 相关设置
 *
 * 该函数用于配置使用 Compose 的模块，主要功能包括：
 * - 启用 Compose 构建功能
 * - 配置 Compose 编译器选项
 * - 添加 Compose 相关依赖
 * - 添加 MiUiX 相关依赖
 * - 添加 Navigation3 导航框架依赖
 *
 * 通过扩展 CommonExtension 来实现对 Android 库和应用模块的 Compose 配置
 *
 * @param commonExtension Android通用扩展
 * Original author: Joker.X
 * Modified by: KuLiPai
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    // 启用 Compose 构建功能
    commonExtension.buildFeatures.compose = true

    // 配置 Compose 相关依赖
    dependencies {
        // 使用 Compose BOM 统一依赖版本
        val bom = libs.findLibrary("androidx.compose.bom").get()
        "implementation"(platform(bom))

        // 核心 UI 组件
        "implementation"(libs.findLibrary("androidx.compose.ui").get())
        "implementation"(libs.findLibrary("androidx.compose.ui.graphics").get())
        "implementation"(libs.findLibrary("androidx.compose.ui.tooling.preview").get())
        "implementation"(libs.findLibrary("androidx.compose.material3").get())

        // Compose 集成支持
        "implementation"(libs.findLibrary("androidx.activity.compose").get())
        "implementation"(libs.findLibrary("androidx.lifecycle.runtime.ktx").get())

        // 开发调试工具
        "debugImplementation"(libs.findLibrary("androidx.compose.ui.tooling").get())
        "debugImplementation"(libs.findLibrary("androidx.compose.ui.test.manifest").get())

        // MiUiX
        "implementation"(libs.findLibrary("miuix.ui").get())
        "implementation"(libs.findLibrary("miuix.preference").get())
        "implementation"(libs.findLibrary("miuix.icons").get())
        "implementation"(libs.findLibrary("miuix.navigation3.ui").get())

        // Navigation3 导航框架
        "implementation"(libs.findLibrary("androidx.navigation3.runtime").get())
        "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.navigation3").get())

        // 液态玻璃和毛玻璃模糊等
        "implementation"(libs.findLibrary("backdrop").get())
        "implementation"(libs.findLibrary("capsule").get())
        "implementation"(libs.findLibrary("haze").get())

        // material 图标扩展
        "implementation"(libs.findLibrary("androidx.compose.material.icons.extended").get())



    }
}
