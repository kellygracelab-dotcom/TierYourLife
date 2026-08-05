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
        buildConfigField(
            "String",
            "TMDB_READ_ACCESS_TOKEN",
            "\"${localProperties.getProperty("TMDB_READ_ACCESS_TOKEN", "")}\"",
        )
    }

    // MigrationTestHelper reads exported schemas from androidTest assets.
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}


dependencies {
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
    // Coil here, not only in presentation: Commons rejects a request without a descriptive
    // User-Agent, so the image loader has to be built with this module's own OkHttpClient
    // rather than Coil's default one. That is a networking concern, which is this module's job.
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)
}
