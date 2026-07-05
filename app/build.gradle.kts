plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.artiuillab.tieryourlife"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.artiuillab.tieryourlife"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.core.theme)
    implementation(projects.feature.tier.presentation)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
}