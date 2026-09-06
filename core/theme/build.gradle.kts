plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.theme"
}

dependencies {
    implementation(projects.core.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
}
