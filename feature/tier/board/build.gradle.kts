plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.tier.board"
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.feature.tier.domain)
    // A board's settings sheet publishes it, and says why it could not.
    implementation(projects.feature.community.domain)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    androidTestImplementation(testFixtures(projects.core.settings))
}
