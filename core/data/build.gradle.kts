plugins {
    alias(libs.plugins.luahook.android.library)
}

android {
    namespace = "com.kulipai.luahook.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material.kolor)

}
