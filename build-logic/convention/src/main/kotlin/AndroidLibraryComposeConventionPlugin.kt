import com.android.build.api.dsl.CommonExtension
import com.kulipai.luahook.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Android 库 Compose 插件
 *
 * 该插件专门用于配置 Android 库的 Compose 相关设置
 * 继承自 AndroidLibrary 插件的所有配置，并添加 Compose 特定配置
 *
 * Original author: Joker.X
 * Modified by: KuLiPai
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 应用 Android 库插件
            pluginManager.apply("com.kulipai.luahook.android.library")
            // 应用 Kotlin Compose 编译器插件
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            // 应用 Kotlin Parcelize 插件
            pluginManager.apply("org.jetbrains.kotlin.plugin.parcelize")

            // 获取 Android 通用扩展并配置 Compose
            val extension = extensions.findByType(CommonExtension::class.java)
            extension?.let { configureAndroidCompose(it) }
        }
    }
}