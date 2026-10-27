plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.tier.presentation"

    // Screens drawn on the JVM need their real resources to look like themselves.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.feature.tier.board)
    implementation(projects.feature.tier.domain)
    implementation(projects.feature.community.domain)
    implementation(projects.feature.account.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    testImplementation(testFixtures(projects.core.settings))
    testImplementation(testFixtures(projects.feature.community.domain))
    testImplementation(testFixtures(projects.feature.account.domain))
    testImplementation(testFixtures(projects.feature.tier.domain))
    // Drawn on the JVM by Robolectric and compared with the pictures in src/test/screenshots.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
