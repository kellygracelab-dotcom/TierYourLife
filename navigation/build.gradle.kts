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
    implementation(projects.core.theme)
    implementation(projects.core.ui)
    implementation(projects.feature.tier.presentation)
    implementation(projects.feature.account.presentation)
    implementation(projects.feature.aistudio.presentation)
}
