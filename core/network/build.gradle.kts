plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.network"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.auth)
}
