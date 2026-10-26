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
}
