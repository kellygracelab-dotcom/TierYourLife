plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.artiuillab.tieryourlife.baselineprofile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The app's fast type is debug-signed, so its benchmark copies install
    // here without the upload key that release needs.
    buildTypes {
        create("fast")
    }

    targetProjectPath = ":app"

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

// Only the plugin's copies of fast measure anything: debug would aim the
// journeys at a debuggable app, and CI's connectedDebugAndroidTest would start
// generating profiles on every pull request; release needs the upload key.
androidComponents {
    beforeVariants { variant ->
        variant.enable = variant.buildType in setOf("benchmarkFast", "nonMinifiedFast")
    }
    onVariants { variant ->
        val loader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { loader.load(it)?.applicationId.orEmpty() },
        )
    }
}
