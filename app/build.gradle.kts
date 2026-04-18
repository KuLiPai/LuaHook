import org.gradle.kotlin.dsl.support.kotlinCompilerOptions

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

}

android {

    namespace = "com.kulipai.luahook"
    compileSdk = libs.versions.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true    // 开启BuildConfig类的生成
        aidl = true           // 启用aidl
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.kulipai.luahook"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = 37
        versionCode = 42
        versionName = "4.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // 启用代码压缩
            isShrinkResources = false // 启用资源压缩
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            // 排除 APK 根目录下的这些文件夹及其内容
//            excludes += "tables/**"
            excludes += "schema/**"
            excludes += "src/**"

            // （排除许可证文件重复导致的构建错误）
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

//        isCoreLibraryDesugaringEnabled = true

    }
    kotlin {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-XXLanguage:+ContextParameters")
        }
    }

    aaptOptions {
        additionalParameters += listOf("--package-id", "0x69", "--allow-reserved-package-id")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(fileTree(mapOf("dir" to "${rootProject.projectDir}/libs", "include" to listOf("*.jar"))))
    implementation(libs.okhttp)
    compileOnly(fileTree("compileOnly"))
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    // The core module that provides APIs to a shell
    implementation(libs.core)
    implementation(libs.kotlin.reflect)
    implementation(libs.xphelper)
    implementation(libs.dexkit)
    implementation(libs.ripple.components.android)
    implementation(libs.ripple.insets)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.shizuku.api)
    implementation(libs.provider) // 如果你需要使用 ShizukuProvider

    implementation(platform(libs.sora.editor.bom))
    implementation(libs.sora.editor)
    implementation(libs.sora.editor.language.textmate)
    implementation(project(":androlua"))

    //Xposed service 100
    compileOnly(project(":libxposed:api"))
    implementation(project(":libxposed:service"))

    //coil3 加载图片
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    // nanohttpd
    implementation(libs.nanohttpd)


    //引入DialogX主体
    implementation(libs.dialogx)
    //非必须 DialogX官方提供的主题样式
    implementation(libs.dialogxkongzuestyle)
    implementation (libs.dialogxmiuistyle)
    implementation(libs.dialogxiosstyle)
    implementation(libs.dialogxmaterialyou)


}
