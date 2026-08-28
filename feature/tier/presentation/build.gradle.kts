plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.tier.presentation"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.feature.tier.domain)
    // The settings screen shows who is signed in; a read across features
    // at the domain layer, never at the screen layer.
    implementation(projects.feature.account.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
