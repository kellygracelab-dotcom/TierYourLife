plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.android.library.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.usage"

    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.network)
    implementation(projects.core.settings)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    // The compose compiler runs on the fixtures too, and asks for its runtime.
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.ui)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(projects.core.settings))
}
