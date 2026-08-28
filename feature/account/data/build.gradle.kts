plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.account.data"
}

dependencies {
    implementation(projects.feature.account.domain)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
}
