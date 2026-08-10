import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class RoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        dependencies {
            add("implementation", libs.findLibrary("androidx-room-runtime").get())
            add("implementation", libs.findLibrary("androidx-room-ktx").get())
            add("ksp", libs.findLibrary("androidx-room-compiler").get())
            add("androidTestImplementation", libs.findLibrary("androidx-room-testing").get())
        }

        extensions.configure<KspExtension> {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}
