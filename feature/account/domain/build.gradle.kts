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
    implementation(libs.javax.inject)
    // The account arrives as a Flow, so whoever reads it needs coroutines too.
    api(libs.kotlinx.coroutines.core)
}
