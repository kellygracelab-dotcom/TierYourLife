plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.settings"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
