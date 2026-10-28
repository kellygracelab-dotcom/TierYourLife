plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.community.presentation"

    // Community drawn on the JVM by Robolectric and compared with the pictures in
    // src/test/screenshots; resources must be real for it to look like itself.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.feature.community.domain)
    implementation(projects.feature.tier.domain)
    // Proposing a section asks a guest to sign in first.
    implementation(projects.feature.account.domain)
    // A stranger's list is drawn by the board renderer.
    implementation(projects.feature.tier.board)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    // The same fakes serve the JVM tests and the emulator ones.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    listOf(projects.core.settings, projects.feature.tier.domain, projects.feature.community.domain, projects.feature.account.domain).forEach { owner ->
        testImplementation(testFixtures(owner))
        androidTestImplementation(testFixtures(owner))
    }
}
