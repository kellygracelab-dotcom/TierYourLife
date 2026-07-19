plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "tieryourlife.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "tieryourlife.android.library.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("roomLibrary") {
            id = "tieryourlife.room.library"
            implementationClass = "RoomConventionPlugin"
        }
        register("hilt") {
            id = "tieryourlife.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
