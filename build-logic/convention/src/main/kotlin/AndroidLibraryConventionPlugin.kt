import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        extensions.configure<LibraryExtension> {
            compileSdk {
                version = release(37)
            }
            defaultConfig {
                minSdk = 24
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            // A warning that only warns is read once; every one fails the build.
            lint {
                warningsAsErrors = true
                abortOnError = true
                // "A newer version exists" turns red on the day a library ships, with
                // nothing in the code changed. Dependabot brings the updates instead.
                disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
            }
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit").get())
            add("testImplementation", dependencies.project(mapOf("path" to ":core:testing")))
            add("androidTestImplementation", dependencies.project(mapOf("path" to ":core:testing")))
            add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
            // AndroidJUnitRunner itself, named above as the instrumentation runner.
            add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
        }
    }
}
