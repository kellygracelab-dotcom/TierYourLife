plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.settings.presentation"
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    // Settings reads across every feature at the domain layer: the trash and
    // the backup, the account, the credits, the moderation queue.
    implementation(projects.feature.tier.domain)
    implementation(projects.feature.account.domain)
    implementation(projects.feature.aistudio.domain)
    implementation(projects.feature.community.domain)
    // The icons and the section label the whole app draws still live with the boards.
    implementation(projects.feature.tier.presentation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
