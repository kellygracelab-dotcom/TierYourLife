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
    // The tablet pictures in the README are drawn here, because the rail is
    // here, and they are drawn from fixtures of the domain's own types.
    androidTestImplementation(projects.feature.tier.domain)
    androidTestImplementation(projects.feature.account.domain)
}
