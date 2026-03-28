import com.kulipai.luahook.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Koin 依赖注入构建插件
 *
 * @author kulipai
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            dependencies {
                // Koin 依赖注入
                "implementation"(platform(libs.findLibrary("koin.bom").get()))
                "implementation"(libs.findLibrary("koin.core").get())
                "implementation"(libs.findLibrary("koin.android").get())
                "implementation"(libs.findLibrary("koin.androidx.compose").get())
                "implementation"(libs.findLibrary("koin.compose.viewmodel").get())
            }
        }
    }
}
