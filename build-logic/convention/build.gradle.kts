plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "tieryourlife.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
    }
}
