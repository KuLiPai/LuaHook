plugins {
//    alias(libs.plugins.luahook.android.library)
    alias(libs.plugins.luahook.android.library.compose)
}

android {
    namespace = "com.kulipai.luahook.core.theme"
}

dependencies {
    implementation(libs.material.kolor)

    implementation(project(":core:model"))

}