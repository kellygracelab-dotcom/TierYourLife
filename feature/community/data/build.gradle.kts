plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.community.data"
}

dependencies {
    implementation(projects.core.network)
    implementation(projects.core.logging)
    implementation(projects.feature.community.domain)
    implementation(projects.feature.tier.domain)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
