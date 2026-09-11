plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.tieryourlife.hilt)
    alias(libs.plugins.tieryourlife.navigation)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Play refuses a version code it has already seen. The commit count keeps each
// build from main above the last without anyone remembering to raise it.
val commitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 1 }

// The upload key lives outside the repository, and so do these four properties:
// ~/.gradle/gradle.properties. Without them a release build is left unsigned.
val uploadStoreFile = providers.gradleProperty("tieryourlife.upload.storeFile")

android {
    namespace = "com.artiuillab.tieryourlife"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.artiuillab.tieryourlife"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.gradleProperty("tieryourlife.versionCode").map(String::toInt).orElse(commitCount).get()
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadStoreFile.isPresent) {
            create("upload") {
                storeFile = file(uploadStoreFile.get())
                storePassword = providers.gradleProperty("tieryourlife.upload.storePassword").get()
                keyAlias = providers.gradleProperty("tieryourlife.upload.keyAlias").get()
                keyPassword = providers.gradleProperty("tieryourlife.upload.keyPassword").get()
            }
        }
    }

    buildTypes {
        debug {
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            optimization {
                enable = false
            }
            // Release stacks arrive obfuscated without it.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.logging)
    implementation(projects.core.settings)
    implementation(projects.core.theme)
    implementation(projects.navigation)
    implementation(projects.feature.account.data)
    implementation(projects.feature.account.domain)
    implementation(projects.feature.tier.data)
    implementation(projects.feature.community.data)
    implementation(projects.feature.tier.presentation)
    implementation(projects.feature.aistudio.data)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
