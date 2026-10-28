plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.play.presentation"

    // Play drawn on the JVM by Robolectric and compared with the pictures in
    // src/test/screenshots; resources must be real for it to look like itself.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.theme)
    implementation(projects.feature.play.domain)
    implementation(projects.feature.tier.domain)
    // Play is drawn over a board, with the board's own tiles and bands.
    implementation(projects.feature.tier.board)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    testImplementation(testFixtures(projects.feature.play.domain))
    testImplementation(testFixtures(projects.feature.tier.domain))
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}
