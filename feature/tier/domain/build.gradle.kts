import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = JVM_11
    }
}

dependencies {
    // A flow, for the one thing in here that has a running state: pictures
    // arriving on a phone that has just been handed somebody's boards.
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
