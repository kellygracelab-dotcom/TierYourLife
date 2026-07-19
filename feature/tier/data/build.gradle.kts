plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.room.library)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.tier.data"
}

dependencies {
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
}
