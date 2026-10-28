plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
    alias(libs.plugins.tieryourlife.room.library)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.community.data"
}

dependencies {
    implementation(projects.core.network)
    implementation(projects.core.logging)
    implementation(projects.feature.community.domain)
    implementation(projects.feature.tier.domain)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    // Whose proposals are whose, and a section's picture going to this person's own folder, as faces and card pictures do.
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    // The platform's own ExifInterface is what lint warns against; this one reads a photo's orientation safely.
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core.ktx)
    testImplementation(testFixtures(projects.feature.community.domain))
}
