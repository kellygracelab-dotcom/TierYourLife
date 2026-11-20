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
    namespace = "com.artiuillab.tieryourlife.core.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // An instrumented run installs a fresh app, whose App Check debug token nobody
        // registered: against production every call it made was a refusal in the
        // proxy's logs. It gets the placeholder address here, as it does on CI.
        val onDevice = gradle.startParameter.taskNames.any { it.substringAfterLast(':').startsWith("connected", ignoreCase = true) }
        val proxyBaseUrl = providers.gradleProperty("PROXY_BASE_URL").orNull
            ?: localProperties.getProperty("PROXY_BASE_URL", "").takeUnless { onDevice }
            ?: ""
        buildConfigField("String", "PROXY_BASE_URL", "\"$proxyBaseUrl\"")
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.auth)
}
