plugins {
    alias(libs.plugins.tieryourlife.android.library)
}

android {
    namespace = "com.artiuillab.tieryourlife.e2e"

    // Firebase finds its parts through the merged manifest, which Robolectric reads only with the resources in.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// The app's own network, sync and publishing against the proxy running in the
// Firebase emulators (README, "End-to-end"). Compiled with every build, run
// only when asked: `./gradlew :e2e:testDebugUnitTest -Pe2e`.
val e2e = providers.gradleProperty("e2e").isPresent
tasks.withType<Test>().configureEach {
    // A copy, so the configuration cache keeps a boolean rather than this script.
    val wanted = e2e
    onlyIf("needs the proxy in the Firebase emulators; run with -Pe2e") { wanted }
    // What they check is the emulators' answer, which no input of this task describes.
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}

// Crashlytics starts with any FirebaseApp and refuses to without the app's
// build id. Only the app's start-up reports to it, and nothing here starts the app.
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.google.firebase", module = "firebase-crashlytics")
}

dependencies {
    testImplementation(projects.core.network)
    testImplementation(projects.core.settings)
    testImplementation(projects.feature.tier.domain)
    testImplementation(projects.feature.play.domain)
    testImplementation(projects.feature.tier.data)
    testImplementation(projects.feature.community.domain)
    testImplementation(projects.feature.community.data)
    testImplementation(projects.feature.account.domain)
    testImplementation(testFixtures(projects.core.settings))
    testImplementation(testFixtures(projects.feature.account.domain))
    testImplementation(platform(libs.firebase.bom))
    testImplementation(libs.firebase.auth)
    testImplementation(libs.retrofit.core)
    testImplementation(libs.retrofit.kotlinx.serialization)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
