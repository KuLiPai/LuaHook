plugins {
    alias(libs.plugins.luahook.android.library)
    alias(libs.plugins.luahook.koin)
}

android {
    namespace = "com.kulipai.luahook.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material.kolor)

}
