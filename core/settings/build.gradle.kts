plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.settings"

    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
