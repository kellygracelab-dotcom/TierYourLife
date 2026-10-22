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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    // Preferences are faked on both sides; the rest only in the JVM tests.
    testFixtures(projects.core.settings).let { preferences ->
        testImplementation(preferences)
        androidTestImplementation(preferences)
    }
    testImplementation(testFixtures(projects.feature.tier.domain))
    testImplementation(testFixtures(projects.feature.community.domain))
    testImplementation(testFixtures(projects.feature.account.domain))
}
