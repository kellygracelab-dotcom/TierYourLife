plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.account.data"
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.network)
    implementation(projects.feature.account.domain)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
}
