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
    alias(libs.plugins.tieryourlife.room.library)
}

android {
    namespace = "com.artiuillab.tieryourlife.feature.tier.data"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val proxyBaseUrl = providers.gradleProperty("PROXY_BASE_URL").orNull
            ?: localProperties.getProperty("PROXY_BASE_URL", "")
        buildConfigField("String", "PROXY_BASE_URL", "\"$proxyBaseUrl\"")
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

dependencies {
    api(projects.core.network)
    implementation(projects.core.logging)
    implementation(projects.feature.tier.domain)
    implementation(projects.feature.account.domain)
    implementation(libs.androidx.core.ktx)
    // Coil shares this module's Wikimedia-aware OkHttpClient.
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)
}
