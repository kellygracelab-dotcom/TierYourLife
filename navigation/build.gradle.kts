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
    implementation(projects.feature.tier.presentation)
    implementation(projects.feature.community.presentation)
    implementation(projects.feature.settings.presentation)
    implementation(projects.feature.account.presentation)
    implementation(projects.feature.aistudio.presentation)
    // Play is handed to the board screens here; its contract lives with the board.
    implementation(projects.feature.play.presentation)
    implementation(projects.feature.tier.board)
    // The tablet pictures in the README are drawn here, because the rail is
    // here, and they are drawn from fixtures of the domain's own types.
    androidTestImplementation(projects.feature.tier.board)
    androidTestImplementation(projects.feature.tier.domain)
    androidTestImplementation(projects.feature.community.domain)
    androidTestImplementation(projects.feature.account.domain)
}
