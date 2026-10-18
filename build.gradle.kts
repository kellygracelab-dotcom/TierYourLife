plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint.gradle) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.kover)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    subprojects.forEach { kover(it) }
}

// What the JVM tests cover of the logic. Screens are left out on purpose: the
// emulator tests check them, and counting them here would only say that unit
// tests do not draw pixels. Generated code is nobody's to test.
kover {
    reports {
        // The floor rises with the tests: raise it when they do, never lower it to pass.
        verify {
            rule {
                minBound(27)
            }
        }
        filters {
            excludes {
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview",
                    "dagger.Module",
                    "dagger.hilt.InstallIn",
                    "javax.annotation.processing.Generated",
                )
                classes(
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_HiltModules*",
                    "Hilt_*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*ComposableSingletons*",
                    "*.BuildConfig",
                    "*.R",
                    "*.R\$*",
                    "dagger.hilt.internal.*",
                    "hilt_aggregated_deps.*",
                )
            }
        }
    }
}
