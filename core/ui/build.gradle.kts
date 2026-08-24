plugins {
    alias(libs.plugins.tieryourlife.android.library)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.ui"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
