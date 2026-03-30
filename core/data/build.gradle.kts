plugins {
    alias(libs.plugins.luahook.android.library)
}

android {
    namespace = "com.kulipai.luahook.core.data"
}

dependencies {
    implementation(project(":core:theme"))
    implementation(libs.androidx.datastore.preferences)
}
