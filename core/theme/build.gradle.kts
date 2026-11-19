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
    // Back closes a tip (TipHost).
    implementation(libs.androidx.activity.compose)
    implementation(libs.material.color.utilities)
    // The proxy's colour vectors, read as they are.
    testImplementation(libs.kotlinx.serialization.json)
}
