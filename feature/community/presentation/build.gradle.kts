plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.community.presentation"
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.feature.community.domain)
    implementation(projects.feature.tier.domain)
    // A stranger's list is drawn by the board renderer.
    implementation(projects.feature.tier.board)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    androidTestImplementation(testFixtures(projects.core.settings))
    androidTestImplementation(testFixtures(projects.feature.tier.domain))
}
