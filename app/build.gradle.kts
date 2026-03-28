plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.luahook.android.application.compose)
    alias(libs.plugins.luahook.koin)
    id("kotlin-parcelize")


}

android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(listOf("META-INF/**", "kotlin/**", "org/**", "**.bin"))
    }
}

base {
    archivesName.set(
        "LuaHook_${libs.versions.versionName.get()}_${libs.versions.versionCode.get()}"
    )
}


dependencies {

    // 导航
    implementation(libs.androidx.navigation3.runtime)
//    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.compose.material.icons.extended)


    implementation(libs.androidx.datastore.preferences)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui.graphics)


    implementation(libs.backdrop)
    implementation(libs.capsule)
    implementation(libs.haze)


    implementation(libs.material.kolor)



    implementation(libs.androidx.core.splashscreen)


}