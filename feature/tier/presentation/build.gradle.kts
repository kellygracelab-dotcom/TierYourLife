plugins {
    id("tieryourlife.android.library")
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.tier.presentation"
}

dependencies {
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
}