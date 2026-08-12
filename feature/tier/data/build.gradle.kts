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
        val tmdbToken = providers.gradleProperty("TMDB_READ_ACCESS_TOKEN").orNull
            ?: localProperties.getProperty("TMDB_READ_ACCESS_TOKEN", "")
        buildConfigField("String", "TMDB_READ_ACCESS_TOKEN", "\"$tmdbToken\"")
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

dependencies {
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
    // Coil shares this module's Wikimedia-aware OkHttpClient.
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)
}
