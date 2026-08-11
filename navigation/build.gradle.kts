plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.navigation)
}

android {
    namespace = "com.artiuillab.tieryourlife.navigation"
}

dependencies {
    implementation(projects.core.settings)
    implementation(projects.feature.tier.presentation)
    implementation(projects.feature.aistudio.presentation)
}
