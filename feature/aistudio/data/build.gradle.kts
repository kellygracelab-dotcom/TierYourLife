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
        val geminiKey = providers.gradleProperty("GEMINI_API_KEY").orNull
            ?: localProperties.getProperty("GEMINI_API_KEY", "")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }
}

dependencies {
    implementation(projects.feature.aistudio.domain)
    implementation(projects.feature.tier.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
