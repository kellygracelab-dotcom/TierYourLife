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

// The module graph is the architecture, so it is checked like code. Read from
// the build files' production dependencies; tests may reach wherever they need.
val checkModuleBoundaries by tasks.registering {
    group = "verification"
    description = "Fails when a module depends on one its layer must not see."
    val buildFiles = subprojects.filter { it.buildFile.exists() }.associate { it.path to it.buildFile }
    inputs.files(buildFiles.values)
    doLast {
        val dependency = Regex("""^\s*(?:implementation|api|compileOnly)\(projects\.([A-Za-z.]+)\)""", RegexOption.MULTILINE)
        fun isPresentation(path: String) = path.endsWith(":presentation") || path == ":feature:tier:board"
        fun isData(path: String) = path.endsWith(":data")
        fun isDomain(path: String) = path.endsWith(":domain")
        fun featureOf(path: String) = path.split(":").getOrNull(2)

        val problems = buildFiles.flatMap { (from, file) ->
            dependency.findAll(file.readText()).map { ":" + it.groupValues[1].replace('.', ':') }.mapNotNull { to ->
                when {
                    to == ":app" -> "nothing depends on the app"
                    to == ":navigation" && from != ":app" -> "only the app wires navigation"
                    from == ":app" && isPresentation(to) -> "the app reaches screens through navigation only"
                    from.startsWith(":core:") && !to.startsWith(":core:") -> "core sees no feature"
                    isDomain(from) && !isDomain(to) -> "a domain depends on other domains only"
                    isData(from) && (isPresentation(to) || isData(to)) -> "data sees neither screens nor another feature's data"
                    isPresentation(from) && isData(to) -> "screens reach data through domain ports"
                    isPresentation(from) && isPresentation(to) && featureOf(from) != featureOf(to) && to != ":feature:tier:board" ->
                        "screens of one feature do not open another's; navigation wires them"
                    else -> null
                }?.let { rule -> "$from -> $to: $rule" }
            }.toList()
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Module boundaries broken:\n" + problems.joinToString("\n") { "  $it" })
        }
        logger.lifecycle("Module boundaries hold across ${buildFiles.size} modules.")
    }
}
