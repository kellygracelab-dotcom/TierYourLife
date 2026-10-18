plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.aistudio.data"
}

dependencies {
    implementation(projects.core.settings)
    implementation(projects.core.network)
    implementation(projects.core.logging)
    implementation(projects.feature.aistudio.domain)
    implementation(projects.feature.account.domain)
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
