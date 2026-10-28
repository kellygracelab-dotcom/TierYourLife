plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.play.data"
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.feature.play.domain)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
