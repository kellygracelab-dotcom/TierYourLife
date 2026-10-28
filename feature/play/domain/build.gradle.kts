import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `java-test-fixtures`
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
    // The game the account keeps is JSON both clients write in one normal
    // form, built and read as a tree; no classes are serialized.
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
