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
    // A face from the phone goes straight to Storage, as card pictures do; FirebaseStorage itself is provided beside them.
    implementation(libs.firebase.storage)
    // The platform's own ExifInterface is what lint warns against; this one reads a photo's orientation safely.
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core.ktx)
}
