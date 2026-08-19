import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

plugins {
    alias(libs.plugins.tieryourlife.android.library)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.network)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.aistudio.data"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val proxyBaseUrl = providers.gradleProperty("PROXY_BASE_URL").orNull
            ?: localProperties.getProperty("PROXY_BASE_URL", "")
        buildConfigField("String", "PROXY_BASE_URL", "\"$proxyBaseUrl\"")
    }
}

dependencies {
    api(projects.core.network)
    implementation(projects.feature.aistudio.domain)
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
