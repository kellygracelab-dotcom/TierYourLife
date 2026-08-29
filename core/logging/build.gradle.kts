plugins {
    alias(libs.plugins.tieryourlife.android.library)
}

android {
    namespace = "com.artiuillab.tieryourlife.core.logging"
}

dependencies {
    api(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
}
